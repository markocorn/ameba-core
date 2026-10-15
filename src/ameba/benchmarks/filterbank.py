"""Fixed filter banks in front of a model: the input split into frequency parts.

A model that sees only u(k) has to build all of its memory itself. A filter
bank hands it some for free: each input channel passes through a set of fixed,
known linear filters, and the model receives the raw input *and* every filter
output, so its search is left mostly with how to combine them. This is subband
identification, and with Laguerre filters the classical Wiener--Bose structure
of nonlinear system identification.

The bank is part of the model, not of the data: the plant is driven by u
alone, its memoryless floor is still measured on u alone, and a found model is
only meaningful together with the bank that fed it. Its filters are known, so
their poles are known; analysis of a model is analysis of what it learned on
top of them.

``octaves``   one second-order band-pass per test octave (six), unit gain at
              each band's geometric centre: the input split exactly as the
              spectral test splits it
``laguerre``  six Laguerre filters with pole ``a``: a low-pass followed by a
              cascade of all-passes, an orthonormal basis whose outputs are
              progressively delayed and smoothed views of the input
``bands``     a chosen subset of the octaves, of order 2 or 4 (two cascaded
              band-passes: sharper, less overlap), optionally normalised so
              each output carries white noise at the input's RMS -- a narrow
              band otherwise hands the model a signal too small for weights of
              at most 2 to recover
``dummy``     a control, not a bank: ``count`` channels that carry nothing
              about the input -- zeros, or noise independent of it -- so any
              change they cause comes from the extra inputs themselves, not
              from what they say
"""

from __future__ import annotations

from dataclasses import dataclass
from math import cos, log, pi, sin, sinh, sqrt
from random import Random
from typing import Sequence

from .splits import BANDS


@dataclass(frozen=True)
class Biquad:
    """y(k) = b0 u(k) + b1 u(k-1) + b2 u(k-2) - a1 y(k-1) - a2 y(k-2)."""

    b0: float
    b1: float
    b2: float
    a1: float
    a2: float

    def run(self, signal: Sequence[float]) -> list[float]:
        u1 = u2 = y1 = y2 = 0.0
        out = []
        for u in signal:
            y = self.b0 * u + self.b1 * u1 + self.b2 * u2 - self.a1 * y1 - self.a2 * y2
            out.append(y)
            u2, u1, y2, y1 = u1, u, y1, y
        return out


def band_pass(low: float, high: float) -> Biquad:
    """A constant-0-dB-peak band-pass (RBJ cookbook) over ``low``..``high``.

    Frequencies are in cycles per sample. The centre is the band's geometric
    mean and the bandwidth its width in octaves, so each filter passes its own
    test octave and attenuates the others.
    """
    centre = sqrt(low * high)
    w0 = 2.0 * pi * centre
    octaves = log(high / low, 2)
    alpha = sin(w0) * sinh(log(2.0) / 2.0 * octaves * w0 / sin(w0))
    a0 = 1.0 + alpha
    return Biquad(alpha / a0, 0.0, -alpha / a0, -2.0 * cos(w0) / a0, (1.0 - alpha) / a0)


def laguerre(signal: Sequence[float], count: int, pole: float) -> list[list[float]]:
    """Outputs of ``count`` discrete Laguerre filters with pole ``pole``.

    L1(z) = sqrt(1 - a^2) z^-1 / (1 - a z^-1), and each next filter multiplies
    by the all-pass (z^-1 - a) / (1 - a z^-1). Every filter has a one-sample
    delay, so the outputs at k depend on u up to k-1 -- strictly causal, which
    is why the raw input is passed alongside them.
    """
    gain = sqrt(1.0 - pole * pole)
    previous_in = [0.0] * count  # each stage's input at k-1
    previous_out = [0.0] * count
    rows = [[0.0] * count for _ in signal]
    for k, u in enumerate(signal):
        stage_in = u
        for stage in range(count):
            if stage == 0:
                y = pole * previous_out[0] + gain * previous_in[0]
            else:
                y = pole * previous_out[stage] + previous_in[stage] - pole * stage_in
            previous_in[stage] = stage_in
            previous_out[stage] = y
            rows[k][stage] = y
            stage_in = y
    outputs = [[row[stage] for row in rows] for stage in range(count)]
    return outputs


def noise_gain(run, length: int = 4000) -> float:
    """RMS gain of a filter for white noise: the norm of its impulse response."""
    impulse = [1.0] + [0.0] * (length - 1)
    return sqrt(sum(value * value for value in run(impulse)))


@dataclass(frozen=True)
class FilterBank:
    name: str
    pole: float = 0.5
    count: int = 6
    #: ``bands`` only: which octaves (1..6), the band-pass order, normalisation.
    bands: tuple[int, ...] = (1, 2, 3, 4, 5, 6)
    order: int = 2
    normalise: bool = False
    #: ``dummy`` only: ``zero`` or ``noise``.
    content: str = "zero"

    def _band(self, index: int):
        biquad = band_pass(*BANDS[index - 1])
        if self.order == 2:
            return biquad.run
        return lambda signal: biquad.run(biquad.run(signal))

    def filter(self, signal: Sequence[float]) -> list[list[float]]:
        if self.name == "octaves":
            return [band_pass(low, high).run(signal) for low, high in BANDS]
        if self.name == "laguerre":
            return laguerre(signal, self.count, self.pole)
        if self.name == "dummy":
            # Noise is fixed by channel and length, not by the input, so a
            # model sees the same uninformative signal at every evaluation.
            if self.content == "zero":
                return [[0.0] * len(signal) for _ in range(self.count)]
            outputs = []
            for channel in range(self.count):
                rng = Random(9000 + channel * 7919 + len(signal))
                outputs.append([rng.uniform(-1.0, 1.0) for _ in signal])
            return outputs
        if self.name == "bands":
            outputs = []
            for index in self.bands:
                run = self._band(index)
                scale = 1.0 / noise_gain(run) if self.normalise else 1.0
                outputs.append([scale * value for value in run(signal)])
            return outputs
        raise ValueError(f"Unknown filter bank {self.name!r}")

    def width(self) -> int:
        """Filter outputs per input channel."""
        if self.name == "octaves":
            return len(BANDS)
        return len(self.bands) if self.name == "bands" else self.count  # laguerre, dummy

    def augment(self, rows: Sequence[Sequence[float]]) -> tuple[tuple[float, ...], ...]:
        """Each row's raw inputs followed by every filter output of every channel."""
        channels = len(rows[0])
        filtered = [self.filter([row[c] for row in rows]) for c in range(channels)]
        return tuple(
            tuple(rows[k]) + tuple(filtered[c][f][k] for c in range(channels) for f in range(self.width()))
            for k in range(len(rows))
        )


def bank_from_spec(spec: str | None) -> FilterBank | None:
    """``none``, ``octaves``, ``laguerre[:POLE[:COUNT]]`` or
    ``bands:ORDER:LIST[:norm]``, e.g. ``bands:4:3,4,5:norm``."""
    if not spec or spec == "none":
        return None
    name, *rest = spec.split(":")
    if name == "laguerre":
        pole = float(rest[0]) if rest else 0.5
        count = int(rest[1]) if len(rest) > 1 else 6
        if not 0.0 < pole < 1.0 or count < 1:
            raise ValueError("laguerre needs 0 < pole < 1 and at least one filter")
        return FilterBank("laguerre", pole, count)
    if name == "dummy" and len(rest) == 2 and rest[0] in ("zero", "noise"):
        return FilterBank("dummy", count=int(rest[1]), content=rest[0])
    if name == "octaves" and not rest:
        return FilterBank("octaves")
    if name == "bands" and len(rest) >= 2:
        order = int(rest[0])
        bands = tuple(int(b) for b in rest[1].split(","))
        normalise = len(rest) > 2 and rest[2] == "norm"
        if order not in (2, 4) or not bands or any(not 1 <= b <= len(BANDS) for b in bands):
            raise ValueError("bands needs order 2 or 4 and octaves between 1 and 6")
        return FilterBank("bands", bands=bands, order=order, normalise=normalise)
    raise ValueError(f"Unknown filter bank {spec!r}")
