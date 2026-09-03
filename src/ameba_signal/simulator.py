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

    #: Instruction opcodes, resolved once at compile time so that a step never
    #: has to ask what kind of node it is looking at.
    _INPUT, _STATEFUL, _STATELESS = 0, 1, 2

    def __init__(self, graph: Graph, operators: Mapping[str, Operator]) -> None:
        self.operators = dict(operators)
        self.states: dict[str, StatefulOperator] = {}
        try:
            for node in graph.nodes.values():
                if node.kind in STATEFUL_KINDS:
                    self.states[node.id] = create_state(node.kind, node.attributes)
            self._compile(graph)
        except (TypeError, ValueError) as exc:
            raise SignalSimulationError(f"Invalid stateful operator configuration: {exc}") from exc

    def _compile(self, graph: Graph) -> None:
        """Resolve everything that does not change between time steps.

        A session runs the same graph for every row of a dataset, so all the
        per-step lookups -- which operator a kind maps to, what an edge weighs,
        where a value lives -- are answered once here and baked into a flat
        instruction list. Values then live in a list addressed by integer slot
        rather than a dictionary keyed by node id.

        The graph is read but never retained: attribute dictionaries the
        operators need are copied, so a session stays isolated from later edits
        without deep-copying the whole graph on every evaluation.
        """
        order = self._execution_order(graph)
        slots = {node_id: index for index, node_id in enumerate(order)}
        self._values = [0.0] * len(order)

        weights: dict[str, list[tuple[int, float]]] = {node_id: [] for node_id in order}
        for edge in sorted(graph.edges.values(), key=lambda item: item.id):
            weights[edge.target].append(
                (slots[edge.source], float(edge.attributes.get("weight", 1.0)))
            )

        self._delay_reads: list[tuple[int, DelayState]] = []
        self._delay_commits: list[tuple[str, StatefulOperator, tuple[tuple[int, float], ...]]] = []
        self._steps: list[tuple] = []
        ready = set()
        for node_id in order:
            node = graph.nodes[node_id]
            if node.kind in CYCLE_BREAKER_KINDS:
                state = self.states[node_id]
                if not isinstance(state, DelayState):
                    raise SignalSimulationError(
                        f"Cycle-breaker {node_id} has incompatible state"
                    )
                self._delay_reads.append((slots[node_id], state))
                ready.add(node_id)
        for node_id in order:
            node = graph.nodes[node_id]
            arguments = tuple(weights[node_id])
            if node.kind in CYCLE_BREAKER_KINDS:
                self._delay_commits.append((node_id, self.states[node_id], arguments))
                continue
            for source_slot, _ in arguments:
                if order[source_slot] not in ready:
                    raise SignalSimulationError(
                        f"Node {node_id} depends on a value unavailable in the current time step"
                    )
            if node.kind == "input":
                self._steps.append((
                    self._INPUT, slots[node_id], int(node.attributes.get("index", 0)),
                    None, (), node_id,
                ))
            elif node.kind in STATEFUL_KINDS:
                self._steps.append((
                    self._STATEFUL, slots[node_id], self.states[node_id],
                    None, arguments, node_id,
                ))
            else:
                operator = self.operators.get(node.kind)
                if operator is None:
                    raise SignalSimulationError(f"Unsupported signal operator: {node.kind}")
                self._steps.append((
                    self._STATELESS, slots[node_id], operator,
                    dict(node.attributes), arguments, node_id,
                ))
            ready.add(node_id)

        self._output_slots = tuple(
            slots[node.id]
            for node in sorted(
                (item for item in graph.nodes.values() if item.kind == "output"),
                key=lambda item: int(item.attributes.get("index", 0)),
            )
        )

    def reset(self) -> None:
        for state in self.states.values():
            state.reset()

    def step(self, inputs: Sequence[float]) -> list[float]:
        values = self._values

        # Delay outputs are prior state, so they are available before the
        # combinational part of the current time step is evaluated.
        for slot, state in self._delay_reads:
            values[slot] = state.output

        for opcode, slot, payload, attributes, arguments, node_id in self._steps:
            if opcode == self._INPUT:
                try:
                    value = float(inputs[payload])
                except IndexError as exc:
                    raise SignalSimulationError(f"Missing input at index {payload}") from exc
            elif opcode == self._STATEFUL:
                try:
                    value = float(payload.evaluate(
                        [values[source] * weight for source, weight in arguments]
                    ))
                except (TypeError, ValueError, OverflowError) as exc:
                    raise SignalSimulationError(f"Stateful operator {node_id} failed: {exc}") from exc
            else:
                try:
                    value = float(payload(
                        [values[source] * weight for source, weight in arguments], attributes
                    ))
                except (TypeError, ValueError, OverflowError) as exc:
                    raise SignalSimulationError(f"Operator {node_id} failed: {exc}") from exc

            if not isfinite(value):
                raise SignalSimulationError(f"Node {node_id} produced a non-finite value")
            values[slot] = value

        # Commit delay inputs only after all current-step outputs exist.
        for node_id, state, arguments in self._delay_commits:
            try:
                state.evaluate([values[source] * weight for source, weight in arguments])
            except (KeyError, TypeError, ValueError, OverflowError) as exc:
                raise SignalSimulationError(f"Delay operator {node_id} failed: {exc}") from exc

        if not self._output_slots:
            raise SignalSimulationError("Signal graph has no output nodes")
        return [values[slot] for slot in self._output_slots]

    @staticmethod
    def _execution_order(graph: Graph) -> list[str]:
        graph.validate_structure()
        outgoing: dict[str, list[tuple[str, str]]] = {node_id: [] for node_id in graph.nodes}
        indegree = {node_id: 0 for node_id in graph.nodes}
        for edge in graph.edges.values():
            outgoing[edge.source].append((edge.id, edge.target))
            if graph.nodes[edge.target].kind in CYCLE_BREAKER_KINDS:
                continue
            indegree[edge.target] += 1
        for links in outgoing.values():
            links.sort()

        ready = deque(sorted(node_id for node_id, degree in indegree.items() if degree == 0))
        order: list[str] = []
        while ready:
            node_id = ready.popleft()
            order.append(node_id)
            for _, target in outgoing[node_id]:
                if graph.nodes[target].kind in CYCLE_BREAKER_KINDS:
                    continue
                indegree[target] -= 1
                if indegree[target] == 0:
                    ready.append(target)

        if len(order) != len(graph.nodes):
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
