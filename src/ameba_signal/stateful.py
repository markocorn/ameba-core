"""Per-session state for discrete-time mathematical operators."""

from __future__ import annotations

from collections import deque
from math import isfinite
from typing import Protocol, Sequence


class StatefulOperator(Protocol):
    def evaluate(self, values: Sequence[float]) -> float: ...

    def reset(self) -> None: ...


class DelayState:
    """A causal delay whose current output is available before its input."""

    cycle_breaker = True

    def __init__(self, steps: int, initial: float) -> None:
        if steps < 1:
            raise ValueError("delay steps must be at least one")
        if not isfinite(initial):
            raise ValueError("delay initial value must be finite")
        self.steps = steps
        self.initial = initial
        self._buffer: deque[float] = deque()
        self.reset()

    @property
    def output(self) -> float:
        return self._buffer[0]

    def evaluate(self, values: Sequence[float]) -> float:
        value = _single(values, "delay")
        self._buffer.popleft()
        self._buffer.append(value)
        return self.output

    def reset(self) -> None:
        self._buffer = deque([self.initial] * self.steps)


class IntegralState:
    cycle_breaker = False

    def __init__(self, gain: float, initial: float) -> None:
        _require_finite("integral gain", gain)
        _require_finite("integral initial value", initial)
        self.gain = gain
        self.initial = initial
        self._previous = initial

    def evaluate(self, values: Sequence[float]) -> float:
        self._previous += self.gain * _single(values, "integral")
        return self._previous

    def reset(self) -> None:
        self._previous = self.initial


class DerivativeState:
    cycle_breaker = False

    def __init__(self, time_step: float, initial_input: float) -> None:
        if not isfinite(time_step) or time_step <= 0.0:
            raise ValueError("derivative time_step must be positive")
        _require_finite("derivative initial input", initial_input)
        self.time_step = time_step
        self.initial_input = initial_input
        self._previous_input = initial_input

    def evaluate(self, values: Sequence[float]) -> float:
        value = _single(values, "derivative")
        output = (value - self._previous_input) / self.time_step
        self._previous_input = value
        return output

    def reset(self) -> None:
        self._previous_input = self.initial_input


class LowPassState:
    cycle_breaker = False

    def __init__(self, alpha: float, initial: float) -> None:
        _require_finite("low-pass alpha", alpha)
        _require_finite("low-pass initial value", initial)
        self.alpha = alpha
        self.initial = initial
        self._previous_output = initial

    def evaluate(self, values: Sequence[float]) -> float:
        value = _single(values, "filter_lp")
        self._previous_output += self.alpha * (value - self._previous_output)
        return self._previous_output

    def reset(self) -> None:
        self._previous_output = self.initial


class HighPassState:
    cycle_breaker = False

    def __init__(self, alpha: float, initial: float, initial_input: float) -> None:
        _require_finite("high-pass alpha", alpha)
        _require_finite("high-pass initial value", initial)
        _require_finite("high-pass initial input", initial_input)
        self.alpha = alpha
        self.initial = initial
        self.initial_input = initial_input
        self._previous_output = initial
        self._previous_input = initial_input

    def evaluate(self, values: Sequence[float]) -> float:
        value = _single(values, "filter_hp")
        output = self.alpha * (self._previous_output + value - self._previous_input)
        self._previous_input = value
        self._previous_output = output
        return output

    def reset(self) -> None:
        self._previous_output = self.initial
        self._previous_input = self.initial_input


def create_state(kind: str, attributes: dict[str, object]) -> StatefulOperator:
    initial = float(attributes.get("initial", 0.0))
    if kind == "delay":
        raw_steps = attributes.get("steps", 1)
        steps = int(raw_steps)
        if isinstance(raw_steps, float) and raw_steps != steps:
            raise ValueError("delay steps must be an integer")
        return DelayState(steps, initial)
    if kind == "integral":
        return IntegralState(float(attributes.get("gain", 1.0)), initial)
    if kind == "derivative":
        return DerivativeState(float(attributes.get("time_step", 1.0)), initial)
    if kind == "filter_lp":
        return LowPassState(float(attributes.get("alpha", 0.5)), initial)
    if kind == "filter_hp":
        return HighPassState(
            float(attributes.get("alpha", 0.5)),
            initial,
            float(attributes.get("initial_input", 0.0)),
        )
    raise ValueError(f"Unsupported stateful operator: {kind}")


STATEFUL_KINDS = frozenset({"delay", "derivative", "filter_hp", "filter_lp", "integral"})
CYCLE_BREAKER_KINDS = frozenset({"delay"})


def _single(values: Sequence[float], operator: str) -> float:
    if len(values) != 1:
        raise ValueError(f"{operator} expects one input, received {len(values)}")
    return values[0]


def _require_finite(name: str, value: float) -> None:
    if not isfinite(value):
        raise ValueError(f"{name} must be finite")
