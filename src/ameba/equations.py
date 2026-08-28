"""Read a signal graph back as a system of difference equations.

Evolution produces topology, not readable mathematics. This module recovers the
mathematics: it walks a graph and renders each node as an expression in terms of
its inputs, inlining anything used exactly once so the result reads like the
equation someone would have written by hand.

Stateful operators always get their own line, because they are recurrences
rather than expressions. Delays are additionally where feedback loops close, so
stopping the expansion there is what makes the rendering terminate at all.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Iterable

from ameba_graph import Graph
from ameba_signal.stateful import STATEFUL_KINDS

# Every rendered value carries an explicit time index, so a delay can shift a
# whole expression by rewriting these in one pass.
_TIME_INDEX = re.compile(r"\(k(?:-(\d+))?\)")

# Expression binding strength, used to decide when a subexpression needs
# parentheses before being embedded in a stronger-binding context.
_SUM = 1
_PRODUCT = 2
_ATOM = 3


@dataclass(frozen=True, slots=True)
class Equation:
    """One rendered line of the model."""

    name: str
    text: str
    note: str = ""

    def __str__(self) -> str:
        return f"{self.text}{f'    [{self.note}]' if self.note else ''}"


def graph_to_equations(graph: Graph) -> list[Equation]:
    """Render ``graph`` as an ordered system of difference equations."""
    named = _named_nodes(graph)
    return [_equation(graph, node_id, named) for node_id in _emission_order(graph, named)]


def format_equations(graph: Graph, indent: str = "  ") -> str:
    equations = graph_to_equations(graph)
    if not equations:
        return f"{indent}(empty graph)"
    width = max(len(equation.text) for equation in equations)
    return "\n".join(
        f"{indent}{equation.text.ljust(width)}"
        + (f"    [{equation.note}]" if equation.note else "")
        for equation in equations
    )


def _named_nodes(graph: Graph) -> set[str]:
    """Nodes that get their own equation instead of being inlined."""
    named: set[str] = set()
    for node in graph.nodes.values():
        if node.kind in {"input", "constant"}:
            continue
        consumers = len(graph.outgoing(node.id))
        # A recurrence, a terminal, or a value reused more than once. Anything
        # else is used exactly once and reads better inlined at its use site.
        if node.kind in STATEFUL_KINDS or node.kind == "output" or consumers != 1:
            named.add(node.id)
    return named


def _emission_order(graph: Graph, named: set[str]) -> list[str]:
    """Dependencies first, recurrences next, the output last."""
    stateful = {node_id for node_id in named if graph.nodes[node_id].kind in STATEFUL_KINDS}
    outputs = {node_id for node_id in named if graph.nodes[node_id].kind == "output"}
    plain = named - stateful - outputs

    ordered: list[str] = []
    remaining = set(plain)
    while remaining:
        ready = sorted(
            node_id
            for node_id in remaining
            # Only inlined dependencies matter; named ones are separate lines,
            # and stateful ones are values carried in from the previous step.
            if not (_inlined_dependencies(graph, node_id, named) & remaining)
        )
        if not ready:  # Defensive: a combinational cycle should be impossible.
            ready = sorted(remaining)
        ordered.extend(ready)
        remaining -= set(ready)
    return ordered + sorted(stateful) + sorted(outputs)


def _inlined_dependencies(graph: Graph, node_id: str, named: set[str]) -> set[str]:
    """Named nodes reached by expanding ``node_id`` through inlined nodes."""
    found: set[str] = set()
    pending = [node_id]
    seen: set[str] = set()
    while pending:
        current = pending.pop()
        if current in seen:
            continue
        seen.add(current)
        for edge in graph.incoming(current):
            if edge.source in named:
                found.add(edge.source)
            else:
                pending.append(edge.source)
    return found


def _equation(graph: Graph, node_id: str, named: set[str]) -> Equation:
    node = graph.nodes[node_id]
    terms = _terms(graph, node_id, named)

    if node.kind in STATEFUL_KINDS:
        text, note = _recurrence(node_id, node.kind, node.attributes, terms)
        return Equation(node_id, text, note)

    if node.kind == "output":
        index = int(node.attributes.get("index", 0))
        expression, _ = _combine(node.kind, node.attributes, terms)
        return Equation(node_id, f"y{index}(k) = {expression}", "output")

    expression, _ = _combine(node.kind, node.attributes, terms)
    return Equation(node_id, f"{node_id}(k) = {expression}", node.kind)


def _terms(graph: Graph, node_id: str, named: set[str]) -> list[tuple[str, int]]:
    """Weighted input expressions, in the order the simulator reads them."""
    terms = []
    for edge in sorted(graph.incoming(node_id), key=lambda item: item.id):
        text, level = _expand(graph, edge.source, named)
        terms.append(_weigh(text, level, float(edge.attributes.get("weight", 1.0))))
    return terms


def _expand(graph: Graph, node_id: str, named: set[str]) -> tuple[str, int]:
    node = graph.nodes[node_id]
    if node.kind == "input":
        return f"u{int(node.attributes.get('index', 0))}(k)", _ATOM
    if node.kind == "constant":
        return _number(float(node.attributes.get("value", 0.0))), _ATOM
    if node_id in named:
        return f"{node_id}(k)", _ATOM
    return _combine(node.kind, node.attributes, _terms(graph, node_id, named))


def _combine(
    kind: str, attributes: dict[str, object], terms: list[tuple[str, int]]
) -> tuple[str, int]:
    if not terms:
        return "0", _ATOM
    texts = [text for text, _ in terms]

    if kind == "add":
        if len(terms) == 1:
            return terms[0]
        return _sum(texts), _SUM
    if kind == "multiply":
        if len(terms) == 1:
            return terms[0]
        return _product(terms)
    if kind == "negate":
        return f"-{_wrap(texts[0], terms[0][1], _PRODUCT)}", _PRODUCT
    if kind == "reciprocal":
        return f"1/({texts[0]})", _ATOM
    if kind == "sin":
        phase = float(attributes.get("phase", 0.0))
        inner = texts[0] if phase == 0.0 else _sum([texts[0], _number(phase)])
        return f"sin({inner})", _ATOM
    if kind == "output":
        return terms[0]
    return f"{kind}({', '.join(texts)})", _ATOM


def _recurrence(
    node_id: str, kind: str, attributes: dict[str, object], terms: list[tuple[str, int]]
) -> tuple[str, str]:
    source = terms[0][0] if terms else "0"
    initial = float(attributes.get("initial", 0.0))

    if kind == "delay":
        steps = int(attributes.get("steps", 1))
        past = _shift(source, steps)
        return f"{node_id}(k) = {past}", f"delay {steps}, init {_number(initial)}"
    if kind == "integral":
        gain = float(attributes.get("gain", 1.0))
        term = source if gain == 1.0 else f"{_number(gain)} * {_wrap(source, _SUM, _PRODUCT)}"
        return (
            f"{node_id}(k) = {node_id}(k-1) + {term}",
            f"integral, init {_number(initial)}",
        )
    if kind == "derivative":
        step = float(attributes.get("time_step", 1.0))
        return (
            f"{node_id}(k) = ({source} - {_shift(source, 1)}) / {_number(step)}",
            f"derivative, init {_number(initial)}",
        )
    if kind == "filter_lp":
        alpha = float(attributes.get("alpha", 0.5))
        return (
            f"{node_id}(k) = {node_id}(k-1) + {_number(alpha)} * "
            f"({source} - {node_id}(k-1))",
            f"low-pass a={_number(alpha)}, init {_number(initial)}",
        )
    if kind == "filter_hp":
        alpha = float(attributes.get("alpha", 0.5))
        return (
            f"{node_id}(k) = {_number(alpha)} * ({node_id}(k-1) + {source} "
            f"- {_shift(source, 1)})",
            f"high-pass a={_number(alpha)}, init {_number(initial)}",
        )
    return f"{node_id}(k) = {kind}({source})", kind


def _product(terms: list[tuple[str, int]]) -> tuple[str, int]:
    """Render a product, folding a reciprocal factor back into a division."""
    factors = [_wrap(text, level, _PRODUCT) for text, level in terms]
    numerators = [text for text in factors if not _is_reciprocal(text)]
    denominators = [text[3:-1] for text in factors if _is_reciprocal(text)]

    numerator = _repeated(numerators) if numerators else "1"
    if not denominators:
        return numerator, _PRODUCT
    return f"{_parenthesized(numerator)} / {_parenthesized(_repeated(denominators))}", _PRODUCT


def _parenthesized(text: str) -> str:
    """Wrap unless the text is already exactly one parenthesized group."""
    if not text.startswith("(") or not text.endswith(")"):
        return f"({text})"
    depth = 0
    for position, character in enumerate(text):
        depth += (character == "(") - (character == ")")
        if depth == 0 and position < len(text) - 1:
            return f"({text})"
    return text


def _is_reciprocal(text: str) -> bool:
    return text.startswith("1/(") and text.endswith(")")


def _repeated(factors: list[str]) -> str:
    """Collapse a factor multiplied by itself into a square."""
    rendered: list[str] = []
    for factor in dict.fromkeys(factors):
        count = factors.count(factor)
        rendered.append(f"{factor}^2" if count == 2 else factor)
        if count > 2:
            rendered[-1] = f"{factor}^{count}"
    return " * ".join(rendered)


def _shift(expression: str, steps: int) -> str:
    """Rewrite every time index in an expression back by ``steps``."""
    if steps == 0:
        return expression

    def bump(match: re.Match[str]) -> str:
        return f"(k-{int(match.group(1) or 0) + steps})"

    return _TIME_INDEX.sub(bump, expression)


def _weigh(text: str, level: int, weight: float) -> tuple[str, int]:
    if weight == 1.0:
        return text, level
    if weight == -1.0:
        return f"-{_wrap(text, level, _PRODUCT)}", _PRODUCT
    return f"{_number(weight)} * {_wrap(text, level, _PRODUCT)}", _PRODUCT


def _sum(texts: Iterable[str]) -> str:
    result = ""
    for index, text in enumerate(texts):
        if index == 0:
            result = text
        elif text.startswith("-"):
            result += f" - {text[1:]}"
        else:
            result += f" + {text}"
    return result


def _wrap(text: str, level: int, required: int) -> str:
    return f"({text})" if level < required else text


def _number(value: float) -> str:
    if value == int(value) and abs(value) < 1e16:
        return str(int(value))
    return f"{value:.4g}"
