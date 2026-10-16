"""Training, validation and test data for a benchmark, kept apart.

A model scored only on the trajectory it was fitted to can memorise that
trajectory: on the Narendra rung, models two to five times below the
memoryless floor in training turned out worse than the floor on every signal
they had not seen. Identification has to be judged on data the search never
touched, so every benchmark gets three disjoint sets of excitations:

``train``       what the search optimises: white noise, steps and a sinusoid
``validation``  what picks the final model among the best few: the same three
                kinds, at other seeds, holds and periods
``test``        what is reported and nothing else: fresh noise, a slow
                sinusoid and long steps -- never used to choose anything

The ``spectral`` family replaces steps and sinusoids with band-limited
multisines, so the spectrum is covered evenly: training and validation hold
white noise plus a low, a middle and a high band; the test set is six octave
bands from near-static to Nyquist, each its own signal, so an error can be
read off as a function of frequency.

Each trajectory starts from rest and is scored against its own memoryless
floor, so a suite's score is the mean of ratios: below one means the model
beats a static gain on that set as a whole.
"""

from __future__ import annotations

from dataclasses import dataclass
from math import isfinite
from random import Random

from ameba_graph import Graph
from ameba_signal import Dataset, SignalEvaluator

from .common import (
    Benchmark,
    channel_evaluation_controls,
    channel_step_controls,
    channel_training_controls,
    evaluation_controls,
    multisine_controls,
    step_controls,
    training_controls,
    trajectory_dataset,
    trajectory_floor,
)


#: The six test octaves of the spectral family, in cycles per sample.
BANDS: tuple[tuple[float, float], ...] = (
    (0.004, 0.01), (0.01, 0.025), (0.025, 0.06), (0.06, 0.15), (0.15, 0.3), (0.3, 0.5),
)


def _controls(channels: int, kind: str, steps: int, seed: int = 0, hold: int = 10, period: int = 250,
              band: tuple[float, float] | None = None, amplitude: float = 1.0):
    if kind == "band":
        rngs = [Random(seed * 101 + channel) for channel in range(channels)]
        columns = [multisine_controls(steps, band[0], band[1], rng, amplitude) for rng in rngs]
        return columns[0] if channels == 1 else tuple(zip(*columns))
    if channels == 1:
        if kind == "noise":
            return training_controls(steps, Random(seed), amplitude)
        if kind == "steps":
            return step_controls(steps, hold=hold)
        return evaluation_controls(steps, period=period)
    if kind == "noise":
        return channel_training_controls(steps, channels, Random(seed), 1.0, hold)
    if kind == "steps":
        return channel_step_controls(steps, channels, hold=hold)
    return channel_evaluation_controls(steps, channels, period=period)


#: (label, kind, steps, seed, hold, period) per split. Training keeps the old
#: single trajectory -- noise at seed 7, 150 steps -- as its first member, so
#: results stay comparable with every run scored on that alone.
SPLITS: dict[str, tuple[tuple[str, str, int, int, int, int], ...]] = {
    "train": (
        ("noise", "noise", 150, 7, 10, 0),
        ("steps", "steps", 100, 0, 10, 0),
        ("sine", "sine", 100, 0, 0, 50),
    ),
    "validation": (
        ("noise", "noise", 150, 31, 10, 0),
        ("steps", "steps", 100, 0, 13, 0),
        ("sine", "sine", 100, 0, 0, 70),
    ),
    "test": (
        ("noise-11", "noise", 150, 11, 10, 0),
        ("noise-23", "noise", 300, 23, 10, 0),
        ("sine", "sine", 300, 0, 0, 250),
        ("steps", "steps", 300, 0, 20, 0),
    ),
}


#: (label, kind, steps, seed, hold, band) per split of the spectral family.
#: Seeds never repeat across splits, so no two signals share their phases.
SPECTRAL: dict[str, tuple[tuple, ...]] = {
    "train": (
        ("noise", "noise", 150, 7, 10, None),
        ("band-low", "band", 200, 101, 0, (0.004, 0.025)),
        ("band-mid", "band", 150, 102, 0, (0.025, 0.15)),
        ("band-high", "band", 100, 103, 0, (0.15, 0.5)),
    ),
    "validation": (
        ("noise", "noise", 150, 31, 10, None),
        ("band-low", "band", 200, 201, 0, (0.004, 0.025)),
        ("band-mid", "band", 150, 202, 0, (0.025, 0.15)),
        ("band-high", "band", 100, 203, 0, (0.15, 0.5)),
    ),
    "test": tuple(
        (f"band-{index + 1}", "band", 600, 301 + index, 0, band) for index, band in enumerate(BANDS)
    ),
}


#: The ``octaves`` family: training and validation hold white noise plus all
#: six test octaves, each its own signal, so every part of the spectrum weighs
#: the same in the score. Short octaves still span several periods; the lowest
#: two get longer records. The test set is the spectral one, unchanged.
_OCTAVE_LENGTHS = (250, 200, 150, 150, 150, 150)
OCTAVES: dict[str, tuple[tuple, ...]] = {
    "train": (("noise", "noise", 150, 7, 10, None),) + tuple(
        (f"band-{i + 1}", "band", _OCTAVE_LENGTHS[i], 111 + i, 0, band) for i, band in enumerate(BANDS)),
    "validation": (("noise", "noise", 150, 31, 10, None),) + tuple(
        (f"band-{i + 1}", "band", _OCTAVE_LENGTHS[i], 211 + i, 0, band) for i, band in enumerate(BANDS)),
    "test": SPECTRAL["test"],
}


def _scaled(members, amplitude: float = 1.0, length: float = 1.0, drop_noise: bool = False):
    """Spectral-style members with every amplitude and length scaled."""
    return tuple((label, kind, round(steps * length), seed, hold, band, amplitude)
                 for label, kind, steps, seed, hold, band in members if not (drop_noise and kind == "noise"))


_FULL = (0.004, 0.5)

#: Excitation families for asking *what* to train on. Members are (label,
#: kind, steps, seed, hold, band, amplitude); every family is tested on the
#: six octave bands, so families compare on the same signals. Seeds stay
#: clear of the test's 301-306.
EXCITATIONS: dict[str, dict[str, tuple[tuple, ...]]] = {
    # White noise at a lower amplitude: RMS 0.29 and 0.14 instead of 0.58,
    # the first about that of the test multisines.
    "noise-a05": {"train": (("noise", "noise", 1500, 7, 10, None, 0.5),),
                  "validation": (("noise", "noise", 600, 31, 10, None, 0.5),)},
    "noise-a025": {"train": (("noise", "noise", 1500, 7, 10, None, 0.25),),
                   "validation": (("noise", "noise", 600, 31, 10, None, 0.25),)},
    # One multisine over the whole band: as broad as noise, shaped like the test.
    "broadband": {"train": (("band-full", "band", 600, 401, 0, _FULL, 1.0),),
                  "validation": (("band-full", "band", 600, 501, 0, _FULL, 1.0),)},
    "broadband-noise": {"train": (("noise", "noise", 150, 7, 10, None, 1.0), ("band-full", "band", 600, 401, 0, _FULL, 1.0)),
                        "validation": (("noise", "noise", 150, 31, 10, None, 1.0), ("band-full", "band", 600, 501, 0, _FULL, 1.0))},
    # The spectral family with one thing changed.
    "spectral-nonoise": {split: _scaled(SPECTRAL[split], drop_noise=True) for split in ("train", "validation")},
    "spectral-a05": {split: _scaled(SPECTRAL[split], amplitude=0.5) for split in ("train", "validation")},
    "spectral-long": {split: _scaled(SPECTRAL[split], length=2.0) for split in ("train", "validation")},
}


@dataclass
class Suite:
    """Several trajectories scored together as mean ratio to their floors."""

    name: str
    members: list[tuple[str, SignalEvaluator, float]]

    def evaluate(self, graph: Graph) -> float:
        ratios = self.ratios(graph)
        values = list(ratios.values())
        return sum(values) / len(values) if all(isfinite(v) for v in values) else float("inf")

    def ratios(self, graph: Graph) -> dict[str, float]:
        return {label: evaluator.evaluate(graph) / floor for label, evaluator, floor in self.members}


def suite(benchmark: Benchmark, split: str, hold: int = 10, criterion: str = "ise",
          family: str = "diverse", bank=None) -> Suite:
    """One split of a benchmark as a scoring suite.

    ``hold`` overrides the noise hold of a multi-channel rung, whose slow
    plant needs a held input; a single-channel rung's noise is white.
    ``family`` picks the signals: ``diverse`` (noise, steps, sinusoids),
    ``spectral`` (noise and three broad multisines), ``octaves`` (noise and
    all six test octaves, equally weighted) or one of :data:`EXCITATIONS`,
    whose test set is always the six octaves.
    ``bank`` is an optional :class:`~ameba.benchmarks.filterbank.FilterBank`:
    the model then sees the raw input and every filter output, while the
    plant and the floor still see the raw input alone.
    """
    members = []
    if family in EXCITATIONS:
        table = EXCITATIONS[family]
        rows = table[split] if split in table else _scaled(SPECTRAL["test"])
        specs = [(label, kind, steps, seed, hold, 0, band, amplitude)
                 for label, kind, steps, seed, _, band, amplitude in rows]
    elif family in ("spectral", "octaves"):
        table = SPECTRAL if family == "spectral" else OCTAVES
        specs = [(label, kind, steps, seed, hold, 0, band, 1.0) for label, kind, steps, seed, _, band in table[split]]
    else:
        specs = [(label, kind, steps, seed, kind_hold, period, None, 1.0)
                 for label, kind, steps, seed, kind_hold, period in SPLITS[split]]
    for label, kind, steps, seed, kind_hold, period, band, amplitude in specs:
        noise_hold = hold if kind == "noise" else kind_hold
        trajectory = benchmark.build(
            _controls(benchmark.channels, kind, steps, seed, noise_hold, period, band, amplitude))
        dataset = trajectory_dataset(trajectory)
        if bank is not None:
            dataset = Dataset(bank.augment(dataset.inputs), dataset.outputs)
        evaluator = SignalEvaluator(dataset, criterion=criterion, time_step=benchmark.time_step)
        members.append((label, evaluator, trajectory_floor(trajectory, criterion, benchmark.time_step)))
    return Suite(split, members)
