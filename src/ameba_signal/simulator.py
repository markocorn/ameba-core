"""Interpreter for mathematical-signal graphs."""

from __future__ import annotations

from collections import deque
from collections.abc import Mapping, Sequence
from math import isfinite

from ameba_graph import Graph

from .operators import STATELESS_OPERATORS, Operator
from .stateful import CYCLE_BREAKER_KINDS, STATEFUL_KINDS, DelayState, StatefulOperator, create_state


class SignalSimulationError(RuntimeError):
    """Raised when a graph cannot be interpreted safely as a signal graph."""


class SignalSimulator:
    """Create isolated execution sessions for mathematical signal graphs."""

    def __init__(self, operators: Mapping[str, Operator] | None = None) -> None:
        self.operators = dict(operators or STATELESS_OPERATORS)

    def run(self, graph: Graph, inputs: Sequence[float]) -> list[float]:
        """Evaluate one step in a new session."""
        return self.start(graph).step(inputs)

    def run_series(self, graph: Graph, inputs: Sequence[Sequence[float]]) -> list[list[float]]:
        session = self.start(graph)
        return [session.step(row) for row in inputs]

    def start(self, graph: Graph) -> SignalSession:
        return SignalSession(graph, self.operators)


class SignalSession:
    """Mutable state for one deterministic, discrete-time simulation run."""

    def __init__(self, graph: Graph, operators: Mapping[str, Operator]) -> None:
        self.graph = graph.copy()
        self.operators = dict(operators)
        self.states: dict[str, StatefulOperator] = {}
        try:
            for node in self.graph.nodes.values():
                if node.kind in STATEFUL_KINDS:
                    self.states[node.id] = create_state(node.kind, node.attributes)
            self.order = self._execution_order()
        except (TypeError, ValueError) as exc:
            raise SignalSimulationError(f"Invalid stateful operator configuration: {exc}") from exc

    def reset(self) -> None:
        for state in self.states.values():
            state.reset()

    def step(self, inputs: Sequence[float]) -> list[float]:
        values: dict[str, float] = {}

        # Delay outputs are prior state, so they are available before the
        # combinational part of the current time step is evaluated.
        for node_id, state in self.states.items():
            if self.graph.nodes[node_id].kind in CYCLE_BREAKER_KINDS:
                if not isinstance(state, DelayState):
                    raise SignalSimulationError(f"Cycle-breaker {node_id} has incompatible state")
                values[node_id] = state.output

        for node_id in self.order:
            node = self.graph.nodes[node_id]
            if node.kind in CYCLE_BREAKER_KINDS:
                continue
            if node.kind == "input":
                index = int(node.attributes.get("index", 0))
                try:
                    value = float(inputs[index])
                except IndexError as exc:
                    raise SignalSimulationError(f"Missing input at index {index}") from exc
            elif node.kind in STATEFUL_KINDS:
                arguments = self._arguments(node_id, values)
                try:
                    value = float(self.states[node_id].evaluate(arguments))
                except (TypeError, ValueError, OverflowError) as exc:
                    raise SignalSimulationError(f"Stateful operator {node_id} failed: {exc}") from exc
            else:
                operator = self.operators.get(node.kind)
                if operator is None:
                    raise SignalSimulationError(f"Unsupported signal operator: {node.kind}")
                arguments = self._arguments(node_id, values)
                try:
                    value = float(operator(arguments, node.attributes))
                except (TypeError, ValueError, OverflowError) as exc:
                    raise SignalSimulationError(f"Operator {node_id} failed: {exc}") from exc

            if not isfinite(value):
                raise SignalSimulationError(f"Node {node_id} produced a non-finite value")
            values[node_id] = value

        # Commit delay inputs only after all current-step outputs exist.
        for node_id, state in self.states.items():
            if self.graph.nodes[node_id].kind not in CYCLE_BREAKER_KINDS:
                continue
            try:
                state.evaluate(self._arguments(node_id, values))
            except (KeyError, TypeError, ValueError, OverflowError) as exc:
                raise SignalSimulationError(f"Delay operator {node_id} failed: {exc}") from exc

        outputs = sorted(
            (node for node in self.graph.nodes.values() if node.kind == "output"),
            key=lambda node: int(node.attributes.get("index", 0)),
        )
        if not outputs:
            raise SignalSimulationError("Signal graph has no output nodes")
        return [values[node.id] for node in outputs]

    def _arguments(self, node_id: str, values: Mapping[str, float]) -> list[float]:
        incoming = sorted(self.graph.incoming(node_id), key=lambda edge: edge.id)
        try:
            return [
                values[edge.source] * float(edge.attributes.get("weight", 1.0))
                for edge in incoming
            ]
        except KeyError as exc:
            raise SignalSimulationError(
                f"Node {node_id} depends on a value unavailable in the current time step"
            ) from exc

    def _execution_order(self) -> list[str]:
        self.graph.validate_structure()
        indegree = {node_id: 0 for node_id in self.graph.nodes}
        for edge in self.graph.edges.values():
            if self.graph.nodes[edge.target].kind in CYCLE_BREAKER_KINDS:
                continue
            indegree[edge.target] += 1

        ready = deque(sorted(node_id for node_id, degree in indegree.items() if degree == 0))
        order: list[str] = []
        while ready:
            node_id = ready.popleft()
            order.append(node_id)
            for edge in sorted(self.graph.outgoing(node_id), key=lambda item: item.id):
                if self.graph.nodes[edge.target].kind in CYCLE_BREAKER_KINDS:
                    continue
                indegree[edge.target] -= 1
                if indegree[edge.target] == 0:
                    ready.append(edge.target)

        if len(order) != len(self.graph.nodes):
            raise SignalSimulationError(
                "Signal graph has an algebraic cycle; every feedback loop must pass through a delay"
            )
        return order


def simulates(graph: Graph, rows: Sequence[Sequence[float]], simulator: "SignalSimulator | None" = None) -> bool:
    """Whether ``graph`` runs to completion over ``rows`` without failing.

    Structural validation cannot catch a division by zero or a diverging
    feedback loop; only running the graph can. Generation uses this to reject
    candidates that are well formed but not executable.
    """
    try:
        (simulator or SignalSimulator()).run_series(graph, rows)
    except SignalSimulationError:
        return False
    return True
