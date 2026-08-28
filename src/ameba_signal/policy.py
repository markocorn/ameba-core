"""Graph constraints for stateless mathematical signal networks."""

from __future__ import annotations

import math
from copy import deepcopy
from random import Random
from typing import Hashable

from ameba_graph import Edge, Graph, GraphError, Node

from .operators import EVOLVABLE_OPERATORS, OPERATOR_ARITY
from .stateful import CYCLE_BREAKER_KINDS, STATEFUL_KINDS, create_state

_ParameterSpec = tuple[float, float, bool, float]
_NODE_PARAMETERS: dict[str, dict[str, _ParameterSpec]] = {
    "constant": {"value": (-100.0, 100.0, False, 0.0)},
    "sin": {"phase": (-2.0 * math.pi, 2.0 * math.pi, False, 0.0)},
    "delay": {
        "steps": (1.0, 10.0, True, 1.0),
        "initial": (-10.0, 10.0, False, 0.0),
    },
    "derivative": {
        "time_step": (1e-6, 10.0, False, 1.0),
        "initial": (-10.0, 10.0, False, 0.0),
    },
    "filter_hp": {
        "alpha": (0.0, 1.0, False, 0.5),
        "initial": (-10.0, 10.0, False, 0.0),
        "initial_input": (-10.0, 10.0, False, 0.0),
    },
    "filter_lp": {
        "alpha": (0.0, 1.0, False, 0.5),
        "initial": (-10.0, 10.0, False, 0.0),
    },
    "integral": {
        "gain": (-10.0, 10.0, False, 1.0),
        "initial": (-10.0, 10.0, False, 0.0),
    },
}


def interface_scaffold(inputs: int = 1, outputs: int = 1) -> Graph:
    """The fixed input/output interface of a signal model.

    How many inputs and outputs a model has is part of the identification
    problem, not something evolution discovers, so the terminals are created
    once with contiguous indices and structure-locked. Generation and
    reproduction then build around them.
    """
    if inputs < 1 or outputs < 1:
        raise ValueError("A signal model needs at least one input and one output")

    graph = Graph()
    for index in range(inputs):
        graph.add_node(Node(f"u{index}", "input", {"index": index}, locked=True))
    for index in range(outputs):
        graph.add_node(Node(f"y{index}", "output", {"index": index}, locked=True))
    return graph


class SignalGraphPolicy:
    """Interpretation rules supplied to the generic evolutionary layer."""

    def __init__(
        self,
        evolvable_kinds: tuple[str, ...] = EVOLVABLE_OPERATORS,
        weight_range: tuple[float, float] = (-2.0, 2.0),
    ) -> None:
        unknown = set(evolvable_kinds) - set(OPERATOR_ARITY)
        if unknown:
            raise ValueError(f"Unknown signal operator(s): {', '.join(sorted(unknown))}")
        if not evolvable_kinds:
            raise ValueError("At least one evolvable signal operator is required")
        if weight_range[0] > weight_range[1]:
            raise ValueError("Invalid edge weight range")
        self.evolvable_kinds = evolvable_kinds
        self.weight_range = weight_range

    def create_node(self, rng: Random) -> Node:
        kind = rng.choice(self.evolvable_kinds)
        attributes = {
            name: _random_parameter(spec, rng)
            for name, spec in _NODE_PARAMETERS.get(kind, {}).items()
        }
        return Node("ignored", kind, attributes)

    def create_edge(self, source: str, target: str, rng: Random) -> Edge:
        return Edge(
            "ignored",
            source,
            target,
            {"weight": rng.uniform(*self.weight_range)},
        )

    def can_connect(self, graph: Graph, source: str, target: str) -> bool:
        if source not in graph.nodes or target not in graph.nodes or source == target:
            return False
        source_node = graph.nodes[source]
        target_node = graph.nodes[target]
        if source_node.kind == "output" or target_node.kind in {"input", "constant"}:
            return False
        if any(edge.source == source and edge.target == target for edge in graph.edges.values()):
            return False
        maximum = OPERATOR_ARITY.get(target_node.kind, (0, 0))[1]
        if maximum is not None and len(graph.incoming(target)) >= maximum:
            return False
        if target_node.kind in CYCLE_BREAKER_KINDS:
            return True
        return not self._combinational_path_exists(graph, target, source)

    def validate(self, graph: Graph) -> None:
        graph.validate_structure()
        if not graph.nodes:
            raise GraphError("Signal graph cannot be empty")

        inputs = [node for node in graph.nodes.values() if node.kind == "input"]
        outputs = [node for node in graph.nodes.values() if node.kind == "output"]
        if not inputs or not outputs:
            raise GraphError("Signal graph requires at least one input and one output")
        self._validate_indices(inputs, "input")
        self._validate_indices(outputs, "output")

        for node in graph.nodes.values():
            if node.kind == "input":
                limits = (0, 0)
            else:
                try:
                    limits = OPERATOR_ARITY[node.kind]
                except KeyError as exc:
                    raise GraphError(f"Unknown signal node kind: {node.kind}") from exc
            count = len(graph.incoming(node.id))
            minimum, maximum = limits
            if count < minimum or (maximum is not None and count > maximum):
                upper = "unbounded" if maximum is None else str(maximum)
                raise GraphError(
                    f"Node {node.id} ({node.kind}) requires {minimum}..{upper} inputs; received {count}"
                )
            if node.kind == "output" and graph.outgoing(node.id):
                raise GraphError(f"Output node {node.id} cannot have outgoing edges")
            self._validate_parameters(node)
            if node.kind in STATEFUL_KINDS:
                try:
                    create_state(node.kind, node.attributes)
                except (TypeError, ValueError) as exc:
                    raise GraphError(f"Invalid state for node {node.id}: {exc}") from exc

        for edge in graph.edges.values():
            weight = float(edge.attributes.get("weight", 1.0))
            if not math.isfinite(weight):
                raise GraphError(f"Edge {edge.id} has a non-finite weight")
            if not self.weight_range[0] <= weight <= self.weight_range[1]:
                raise GraphError(f"Edge {edge.id} weight is outside configured limits")

        if self._has_combinational_cycle(graph):
            raise GraphError("Every signal feedback loop must pass through a delay")

    def mutate_node(self, node: Node, rng: Random) -> Node:
        specs = _NODE_PARAMETERS.get(node.kind, {})
        mutable = [name for name in specs if name not in node.locked_attributes]
        if not mutable:
            raise ValueError(f"Node kind {node.kind} has no mutable parameters")
        name = rng.choice(mutable)
        lower, upper, integer, default = specs[name]
        current = float(node.attributes.get(name, default))
        if integer:
            delta = rng.choice((-1, 1))
            value: float | int = int(_clamp(round(current) + delta, lower, upper))
        else:
            value = _clamp(current + rng.uniform(-0.1, 0.1) * (upper - lower), lower, upper)
        mutated = deepcopy(node)
        mutated.attributes[name] = value
        return mutated

    def mutate_edge(self, edge: Edge, rng: Random) -> Edge:
        if "weight" in edge.locked_attributes:
            raise ValueError("Edge weight is locked")
        lower, upper = self.weight_range
        current = float(edge.attributes.get("weight", 1.0))
        mutated = deepcopy(edge)
        mutated.attributes["weight"] = _clamp(
            current + rng.uniform(-0.1, 0.1) * (upper - lower), lower, upper
        )
        return mutated

    def cross_node(self, left: Node, right: Node, rng: Random) -> Node:
        if left.kind != right.kind:
            raise ValueError("Signal node crossover requires equal node kinds")
        specs = _NODE_PARAMETERS.get(left.kind, {})
        mutable = [name for name in specs if name not in left.locked_attributes]
        if not mutable:
            raise ValueError(f"Node kind {left.kind} has no crossable parameters")
        name = rng.choice(mutable)
        lower, upper, integer, default = specs[name]
        value = (float(left.attributes.get(name, default)) + float(right.attributes.get(name, default))) / 2.0
        crossed = deepcopy(left)
        crossed.attributes[name] = int(round(_clamp(value, lower, upper))) if integer else _clamp(value, lower, upper)
        return crossed

    def cross_edge(self, left: Edge, right: Edge, rng: Random) -> Edge:
        if "weight" in left.locked_attributes:
            raise ValueError("Edge weight is locked")
        crossed = deepcopy(left)
        crossed.attributes["weight"] = _clamp(
            (float(left.attributes.get("weight", 1.0)) + float(right.attributes.get("weight", 1.0))) / 2.0,
            *self.weight_range,
        )
        return crossed

    @staticmethod
    def requires_more_inputs(graph: Graph, node_id: str) -> bool:
        """Whether a node is still short of the incoming edges its kind needs."""
        node = graph.nodes[node_id]
        if node.kind == "input":
            return False
        minimum, _ = OPERATOR_ARITY.get(node.kind, (0, 0))
        return len(graph.incoming(node_id)) < minimum

    @staticmethod
    def can_transfer_node(node: Node) -> bool:
        """Keep external input/output identities outside transferable subgraphs."""
        return node.kind not in {"input", "output", "constant"}

    @staticmethod
    def connection_type(graph: Graph, source: str, target: str) -> Hashable:
        """All current mathematical signal ports carry one scalar value."""
        if source not in graph.nodes or target not in graph.nodes:
            raise GraphError("Cannot type a connection with a missing endpoint")
        return "scalar"

    @staticmethod
    def _validate_indices(nodes: list[Node], kind: str) -> None:
        try:
            indices = sorted(int(node.attributes["index"]) for node in nodes)
        except (KeyError, TypeError, ValueError) as exc:
            raise GraphError(f"Every {kind} node requires an integer index") from exc
        if indices != list(range(len(nodes))):
            raise GraphError(f"{kind.capitalize()} indices must be unique and contiguous from zero")

    @staticmethod
    def _validate_parameters(node: Node) -> None:
        for name, (lower, upper, integer, _) in _NODE_PARAMETERS.get(node.kind, {}).items():
            if name not in node.attributes:
                continue
            value = node.attributes[name]
            if isinstance(value, bool) or not isinstance(value, (int, float)):
                raise GraphError(f"Node {node.id} parameter {name} must be numeric")
            numeric = float(value)
            if not math.isfinite(numeric) or not lower <= numeric <= upper:
                raise GraphError(f"Node {node.id} parameter {name} is outside valid limits")
            if integer and numeric != int(numeric):
                raise GraphError(f"Node {node.id} parameter {name} must be an integer")

    @staticmethod
    def _combinational_path_exists(graph: Graph, start: str, target: str) -> bool:
        pending = [start]
        visited: set[str] = set()
        while pending:
            node_id = pending.pop()
            if node_id == target:
                return True
            if node_id in visited:
                continue
            visited.add(node_id)
            pending.extend(
                edge.target
                for edge in graph.outgoing(node_id)
                if graph.nodes[edge.target].kind not in CYCLE_BREAKER_KINDS
            )
        return False

    def _has_combinational_cycle(self, graph: Graph) -> bool:
        return any(self._combinational_cycle_from(graph, node_id) for node_id in graph.nodes)

    @staticmethod
    def _combinational_cycle_from(graph: Graph, origin: str) -> bool:
        pending = [
            edge.target
            for edge in graph.outgoing(origin)
            if graph.nodes[edge.target].kind not in CYCLE_BREAKER_KINDS
        ]
        visited: set[str] = set()
        while pending:
            node_id = pending.pop()
            if node_id == origin:
                return True
            if node_id in visited:
                continue
            visited.add(node_id)
            pending.extend(
                edge.target
                for edge in graph.outgoing(node_id)
                if graph.nodes[edge.target].kind not in CYCLE_BREAKER_KINDS
            )
        return False


def _random_parameter(spec: _ParameterSpec, rng: Random) -> float | int:
    lower, upper, integer, default = spec
    if integer:
        return rng.randint(int(lower), int(upper))
    if lower <= default <= upper and rng.random() < 0.25:
        return default
    return rng.uniform(lower, upper)


def _clamp(value: float, lower: float, upper: float) -> float:
    if not math.isfinite(value):
        raise ValueError("Signal parameter must be finite")
    return min(max(value, lower), upper)
