"""Stateless mathematical signal operators."""

from __future__ import annotations

import math
from collections.abc import Callable, Sequence

Operator = Callable[[Sequence[float], dict[str, object]], float]


def _add(values: Sequence[float], _: dict[str, object]) -> float:
    return sum(values)


def _multiply(values: Sequence[float], _: dict[str, object]) -> float:
    result = 1.0
    for value in values:
        result *= value
    return result


def _constant(_: Sequence[float], attributes: dict[str, object]) -> float:
    return float(attributes.get("value", 0.0))


def _sin(values: Sequence[float], attributes: dict[str, object]) -> float:
    phase = float(attributes.get("phase", 0.0))
    return math.sin(_single(values, "sin") + phase)


def _negate(values: Sequence[float], _: dict[str, object]) -> float:
    return -_single(values, "negate")


def _reciprocal(values: Sequence[float], _: dict[str, object]) -> float:
    # Division enters the operator set as a unary reciprocal so that every
    # multi-input operator stays commutative; argument order is derived from
    # edge identifiers, which a structural mutation is free to change.
    value = _single(values, "reciprocal")
    if value == 0.0:
        raise ValueError("reciprocal is undefined for a zero input")
    return 1.0 / value


def _identity(values: Sequence[float], _: dict[str, object]) -> float:
    return _single(values, "output")


def _square(values: Sequence[float], _: dict[str, object]) -> float:
    # A node cannot feed the same target twice, so squaring through `multiply`
    # needs a relay node. This makes the common case a single node.
    value = _single(values, "square")
    return value * value


def _sqrt(values: Sequence[float], _: dict[str, object]) -> float:
    value = _single(values, "sqrt")
    if value < 0.0:
        raise ValueError("sqrt is undefined for a negative input")
    return math.sqrt(value)


def _absolute(values: Sequence[float], _: dict[str, object]) -> float:
    return abs(_single(values, "abs"))


def _exp(values: Sequence[float], _: dict[str, object]) -> float:
    # math.exp raises OverflowError past ~709, which the simulator turns into a
    # rejected candidate rather than a silent infinity.
    return math.exp(_single(values, "exp"))


def _log(values: Sequence[float], _: dict[str, object]) -> float:
    value = _single(values, "log")
    if value <= 0.0:
        raise ValueError("log is undefined for a non-positive input")
    return math.log(value)


def _tanh(values: Sequence[float], _: dict[str, object]) -> float:
    return math.tanh(_single(values, "tanh"))


def _single(values: Sequence[float], operator: str) -> float:
    if len(values) != 1:
        raise ValueError(f"{operator} expects one input, received {len(values)}")
    return values[0]


# --- Soft operators -----------------------------------------------------------
#
# Every soft node sums its weighted inputs and a bias, z = sum + bias, and
# answers within +-1 (or 0..1) through a smooth limit it approaches but never
# reaches: a cocoon around the node. Inside the cocoon each has a familiar
# shape -- linear, square, bump, exponential, logarithm, root, sine -- and
# outside it settles to a plateau instead of exploding or leaving its domain,
# so a small change of a weight is always a small change of behaviour. They
# are defined for every real input, so none of them can invalidate a model.


def _soft_z(values: Sequence[float], attributes: dict[str, object]) -> float:
    return sum(values) + float(attributes.get("bias", 0.0))


def _soft_lin(values, attributes):
    return math.tanh(_soft_z(values, attributes))


def _soft_mul(values, attributes):
    product = 1.0
    for value in values:
        product *= value
    return math.tanh(product + float(attributes.get("bias", 0.0)))


def _soft_sq(values, attributes):
    z = _soft_z(values, attributes)
    return z * z / (1.0 + z * z)


def _soft_bump(values, attributes):
    z = _soft_z(values, attributes)
    return 1.0 / (1.0 + z * z)


def _soft_exp(values, attributes):
    return math.tanh(math.expm1(min(_soft_z(values, attributes), 40.0)))


def _soft_log(values, attributes):
    z = _soft_z(values, attributes)
    return math.tanh(math.copysign(math.log1p(abs(z)), z))


def _soft_alog(values, attributes):
    return math.tanh(math.log1p(abs(_soft_z(values, attributes))))


def _soft_sqrt(values, attributes):
    z = _soft_z(values, attributes)
    return math.tanh(math.copysign(math.sqrt(1.0 + abs(z)) - 1.0, z))


def _soft_asqrt(values, attributes):
    return math.tanh(math.sqrt(1.0 + abs(_soft_z(values, attributes))) - 1.0)


def _soft_sin(values, attributes):
    return math.sin(_soft_z(values, attributes))


def _soft_abs(values, attributes):
    return math.tanh(abs(_soft_z(values, attributes)))


#: The soft family, in a fixed order.
SOFT_OPERATORS: dict[str, Operator] = {
    "soft_lin": _soft_lin,
    "soft_mul": _soft_mul,
    "soft_sq": _soft_sq,
    "soft_bump": _soft_bump,
    "soft_exp": _soft_exp,
    "soft_log": _soft_log,
    "soft_alog": _soft_alog,
    "soft_sqrt": _soft_sqrt,
    "soft_asqrt": _soft_asqrt,
    "soft_sin": _soft_sin,
    "soft_abs": _soft_abs,
}


STATELESS_OPERATORS: dict[str, Operator] = {
    "abs": _absolute,
    "add": _add,
    "constant": _constant,
    "exp": _exp,
    "log": _log,
    "multiply": _multiply,
    "negate": _negate,
    "output": _identity,
    "reciprocal": _reciprocal,
    "sin": _sin,
    "sqrt": _sqrt,
    "square": _square,
    "tanh": _tanh,
    **SOFT_OPERATORS,
}

# Minimum and maximum incoming edge counts. ``None`` means unbounded.
OPERATOR_ARITY: dict[str, tuple[int, int | None]] = {
    "abs": (1, 1),
    "add": (1, None),
    "constant": (0, 0),
    "exp": (1, 1),
    "log": (1, 1),
    "multiply": (1, None),
    "negate": (1, 1),
    "output": (1, 1),
    "reciprocal": (1, 1),
    "sin": (1, 1),
    "sqrt": (1, 1),
    "square": (1, 1),
    "tanh": (1, 1),
    **{kind: (1, None) for kind in SOFT_OPERATORS},
    "delay": (1, 1),
    "derivative": (1, 1),
    "filter_hp": (1, 1),
    "filter_lp": (1, 1),
    "integral": (1, 1),
}

#: Operators grouped by the role they play in a model. Selecting whole groups
#: is usually what an experiment wants -- "linear dynamics only", or "add the
#: nonlinearities" -- rather than naming operators one at a time.
OPERATOR_GROUPS: dict[str, tuple[str, ...]] = {
    # Sums and products: everything a linear model needs, plus division.
    "arithmetic": ("add", "multiply", "negate", "reciprocal"),
    # Shape the signal without remembering anything about it.
    "nonlinear": ("sin", "tanh", "square", "sqrt", "abs", "exp", "log"),
    # Carry state between steps; a model needs one of these to be dynamic.
    "memory": ("delay", "integral", "derivative", "filter_lp", "filter_hp"),
    # Sources and sinks. Inputs and outputs are the fixed model interface and
    # are never evolvable; a constant is a source the search may introduce.
    "terminal": ("input", "output", "constant"),
    # Smooth, bounded nodes: each sums its inputs and a bias, then answers
    # within +-1 through a soft limit. Not part of EVOLVABLE_OPERATORS, so a
    # search uses them only when it asks for them.
    "soft": tuple(SOFT_OPERATORS),
}

#: Every operator evolution may create: everything except the fixed interface.
EVOLVABLE_OPERATORS: tuple[str, ...] = (
    OPERATOR_GROUPS["arithmetic"]
    + OPERATOR_GROUPS["nonlinear"]
    + OPERATOR_GROUPS["memory"]
    + ("constant",)
)


def operators_in(*groups: str) -> tuple[str, ...]:
    """Expand group names into operator names, keeping group order.

    Names that are already operators pass through, so a selection can mix the
    two: ``operators_in("arithmetic", "delay")``.
    """
    selected: list[str] = []
    for name in groups:
        members = OPERATOR_GROUPS.get(name)
        if members is None:
            if name not in OPERATOR_ARITY:
                known = ", ".join(sorted(OPERATOR_GROUPS) + sorted(OPERATOR_ARITY))
                raise ValueError(f"Unknown operator or group {name!r}; expected one of {known}")
            members = (name,)
        selected.extend(item for item in members if item not in selected)
    return tuple(selected)
