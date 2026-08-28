"""Adapter from generic graph candidates to signal-domain fitness."""

from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass
from math import isfinite

from ameba_graph import Graph

from .simulator import SignalSimulationError, SignalSimulator


@dataclass(frozen=True, slots=True)
class Dataset:
    inputs: tuple[tuple[float, ...], ...]
    outputs: tuple[tuple[float, ...], ...]

    def __post_init__(self) -> None:
        if len(self.inputs) != len(self.outputs):
            raise ValueError("Dataset input and output row counts must match")
        for name, rows in (("input", self.inputs), ("output", self.outputs)):
            widths = {len(row) for row in rows}
            if len(widths) > 1:
                raise ValueError(f"Dataset {name} rows must have a consistent width")
            if any(not isfinite(float(value)) for row in rows for value in row):
                raise ValueError(f"Dataset {name} values must be finite")


#: The classical integral error criteria, as functions of the error at time t.
#: Each is integrated over the run by the rectangle rule, so with a unit sample
#: time ISE reduces exactly to the plain sum of squared error.
CRITERIA: dict[str, Callable[[float, float], float]] = {
    "ise": lambda error, time: error * error,
    "iae": lambda error, time: abs(error),
    "itse": lambda error, time: time * error * error,
    "itae": lambda error, time: time * abs(error),
}


class SignalEvaluator:
    """Score a graph by an integral error criterion; lower scores are better.

    The default is ISE, the integral of squared error. IAE weights large and
    small deviations more evenly; the time-weighted ITSE and ITAE discount
    early error and penalise error that persists, which is what makes them
    useful for transient response tuning.
    """

    def __init__(
        self,
        dataset: Dataset,
        simulator: SignalSimulator | None = None,
        invalid_score: float = float("inf"),
        criterion: str = "ise",
        time_step: float = 1.0,
    ) -> None:
        if criterion not in CRITERIA:
            raise ValueError(
                f"Unknown error criterion {criterion!r}; expected one of "
                f"{', '.join(sorted(CRITERIA))}"
            )
        if not isfinite(time_step) or time_step <= 0.0:
            raise ValueError("time_step must be positive")
        self.dataset = dataset
        self.simulator = simulator or SignalSimulator()
        self.invalid_score = invalid_score
        self.criterion = criterion
        self.time_step = time_step

    def evaluate(self, graph: Graph) -> float:
        contribution = CRITERIA[self.criterion]
        score = 0.0
        try:
            session = self.simulator.start(graph)
            for step, (inputs, expected) in enumerate(
                zip(self.dataset.inputs, self.dataset.outputs)
            ):
                actual = session.step(inputs)
                if len(actual) != len(expected):
                    return self.invalid_score
                time = step * self.time_step
                score += sum(
                    contribution(value - target, time)
                    for value, target in zip(actual, expected)
                )
        except SignalSimulationError:
            return self.invalid_score
        return score * self.time_step
