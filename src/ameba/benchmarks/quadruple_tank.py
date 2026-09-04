"""Johansson's quadruple-tank process: a MIMO rung with an adjustable zero.

K. H. Johansson, "The Quadruple-Tank Process: A Multivariable Laboratory
Process with an Adjustable Zero", IEEE Transactions on Control Systems
Technology 8(3), 2000.

Four tanks, two pumps. Pump 1 fills tank 1 directly and tank 4 through a valve;
pump 2 fills tank 2 directly and tank 3 through a valve. Tanks 3 and 4 drain
into tanks 1 and 2, so each pump reaches each measured level by two routes --
one fast and direct, one slow and over the top:

    dh1/dt = -a1/A1 sqrt(2 g h1) + a3/A1 sqrt(2 g h3) + g1 k1 / A1 * v1
    dh2/dt = -a2/A2 sqrt(2 g h2) + a4/A2 sqrt(2 g h4) + g2 k2 / A2 * v2
    dh3/dt = -a3/A3 sqrt(2 g h3)                      + (1 - g2) k2 / A3 * v2
    dh4/dt = -a4/A4 sqrt(2 g h4)                      + (1 - g1) k1 / A4 * v1

    y1 = kc h1,   y2 = kc h2

What makes it a benchmark rather than another coupled system is the valve
setting. The linearisation has a transmission zero whose sign is decided by
``g1 + g2``: above one the system is minimum phase, below one a zero sits in
the right half plane and each output first moves *away* from where the input
is driving it, because the direct route acts before the slow one over the top.

That inverse response is the property worth testing. A model can score well on
the minimum-phase configuration by learning a smoothed gain; it cannot do that
on the non-minimum-phase one without representing two routes with different
delays, which is precisely the structure identification is supposed to find.

The dynamics are also genuinely nonlinear -- outflow goes as the square root of
level -- so this is not a linear plant wearing a MIMO shape.
"""

from __future__ import annotations

from dataclasses import dataclass
from math import sqrt
from random import Random
from typing import Sequence

from ameba_signal import Dataset

GRAVITY = 981.0  # cm/s^2


@dataclass(frozen=True, slots=True)
class TankParameters:
    """One physical configuration, in the paper's centimetre/volt/second units.

    ``split`` is (g1, g2), the fraction of each pump's flow sent to the tank it
    feeds directly. It is the only thing that moves the transmission zero, and
    on the real rig it is set by turning two valves.
    """

    area: tuple[float, float, float, float]
    outlet: tuple[float, float, float, float]
    gain: tuple[float, float]
    split: tuple[float, float]
    voltage: tuple[float, float]
    sensor: float = 0.5

    def __post_init__(self) -> None:
        if any(value <= 0.0 for value in self.area + self.outlet):
            raise ValueError("Tank areas and outlet areas must be positive")
        if not all(0.0 < value < 1.0 for value in self.split):
            raise ValueError("Valve splits must lie strictly between zero and one")

    @property
    def minimum_phase(self) -> bool:
        """Whether the transmission zero sits in the left half plane."""
        return self.split[0] + self.split[1] > 1.0


#: The paper's operating point P-, valves open wide to the direct route.
MINIMUM_PHASE = TankParameters(
    area=(28.0, 32.0, 28.0, 32.0),
    outlet=(0.071, 0.057, 0.071, 0.057),
    gain=(3.33, 3.35),
    split=(0.70, 0.60),
    voltage=(3.00, 3.00),
)

#: The paper's operating point P+, most of the flow routed over the top. This
#: is the hard one: g1 + g2 = 0.77, so a zero lies in the right half plane.
NON_MINIMUM_PHASE = TankParameters(
    area=(28.0, 32.0, 28.0, 32.0),
    outlet=(0.071, 0.057, 0.071, 0.057),
    gain=(3.14, 3.29),
    split=(0.43, 0.34),
    voltage=(3.15, 3.15),
)


def equilibrium(parameters: TankParameters) -> tuple[float, float, float, float]:
    """The exact resting levels for the configured pump voltages.

    Solved rather than taken from the paper's table, whose rounded levels are a
    percent or two off equilibrium. Starting exactly at rest means any response
    in the data is caused by the input, not by a settling transient the model
    would have to explain and could not.
    """
    a1, a2, a3, a4 = parameters.outlet
    k1, k2 = parameters.gain
    g1, g2 = parameters.split
    v1, v2 = parameters.voltage

    level3 = ((1.0 - g2) * k2 * v2 / a3) ** 2 / (2.0 * GRAVITY)
    level4 = ((1.0 - g1) * k1 * v1 / a4) ** 2 / (2.0 * GRAVITY)
    level1 = ((a3 * sqrt(2.0 * GRAVITY * level3) + g1 * k1 * v1) / a1) ** 2 / (2.0 * GRAVITY)
    level2 = ((a4 * sqrt(2.0 * GRAVITY * level4) + g2 * k2 * v2) / a2) ** 2 / (2.0 * GRAVITY)
    return level1, level2, level3, level4


def derivatives(
    levels: Sequence[float], voltages: Sequence[float], parameters: TankParameters
) -> tuple[float, float, float, float]:
    """Rate of change of each level, with outflow clamped at an empty tank."""
    a1, a2, a3, a4 = parameters.outlet
    area1, area2, area3, area4 = parameters.area
    k1, k2 = parameters.gain
    g1, g2 = parameters.split
    h1, h2, h3, h4 = levels
    v1, v2 = voltages

    def outflow(area: float, level: float) -> float:
        return area * sqrt(2.0 * GRAVITY * level) if level > 0.0 else 0.0

    return (
        (-outflow(a1, h1) + outflow(a3, h3) + g1 * k1 * v1) / area1,
        (-outflow(a2, h2) + outflow(a4, h4) + g2 * k2 * v2) / area2,
        (-outflow(a3, h3) + (1.0 - g2) * k2 * v2) / area3,
        (-outflow(a4, h4) + (1.0 - g1) * k1 * v1) / area4,
    )


def simulate(
    controls: Sequence[Sequence[float]],
    parameters: TankParameters = MINIMUM_PHASE,
    time_step: float = 3.0,
    substeps: int = 10,
) -> tuple[tuple[float, float], ...]:
    """Run from rest, returning the measured level deviation after each input.

    ``controls`` are pump-voltage *deviations* from the operating point and the
    result is the corresponding deviation in measured level, so both sides are
    centred on zero. That matters for the pass criterion: the memoryless floor
    assumes an independent zero-mean input, and a model fed absolute voltages
    could score well by learning the operating point alone.

    Integration is fourth-order Runge-Kutta over ``substeps`` per sample, which
    the tests check against a much finer integration.
    """
    if time_step <= 0.0 or substeps < 1:
        raise ValueError("time_step must be positive and substeps at least one")
    levels = list(equilibrium(parameters))
    rest = (levels[0] * parameters.sensor, levels[1] * parameters.sensor)
    nominal = parameters.voltage
    interval = time_step / substeps
    responses = []

    for row in controls:
        voltages = (nominal[0] + row[0], nominal[1] + row[1])
        for _ in range(substeps):
            k1 = derivatives(levels, voltages, parameters)
            state2 = [h + 0.5 * interval * d for h, d in zip(levels, k1)]
            k2 = derivatives(state2, voltages, parameters)
            state3 = [h + 0.5 * interval * d for h, d in zip(levels, k2)]
            k3 = derivatives(state3, voltages, parameters)
            state4 = [h + interval * d for h, d in zip(levels, k3)]
            k4 = derivatives(state4, voltages, parameters)
            levels = [
                max(0.0, h + interval / 6.0 * (a + 2.0 * b + 2.0 * c + d))
                for h, a, b, c, d in zip(levels, k1, k2, k3, k4)
            ]
        responses.append((
            levels[0] * parameters.sensor - rest[0],
            levels[1] * parameters.sensor - rest[1],
        ))
    return tuple(responses)


def excitation(
    steps: int, rng: Random, amplitude: float = 0.5, hold: int = 10
) -> tuple[tuple[float, float], ...]:
    """A random level that holds for several samples, per pump independently.

    White noise is the wrong input for this plant. Its slowest tank has a time
    constant of well over a minute, so sample-to-sample noise is filtered out
    almost entirely and the data would carry no information about the dynamics
    worth identifying. Holding each level excites the slow modes the two routes
    differ in, which is where the plant's character lives.
    """
    if hold < 1:
        raise ValueError("hold must be at least one sample")
    rows, current = [], (0.0, 0.0)
    for step in range(steps):
        if step % hold == 0:
            current = (
                rng.uniform(-amplitude, amplitude),
                rng.uniform(-amplitude, amplitude),
            )
        rows.append(current)
    return tuple(rows)


def identification_dataset(
    steps: int = 300,
    parameters: TankParameters = MINIMUM_PHASE,
    seed: int = 7,
    amplitude: float = 0.5,
    hold: int = 10,
    time_step: float = 3.0,
) -> Dataset:
    """Pump-voltage deviations in, measured level deviations one step later."""
    controls = excitation(steps, Random(seed), amplitude, hold)
    return Dataset(controls, simulate(controls, parameters, time_step))
