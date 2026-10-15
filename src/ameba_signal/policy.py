"""Graph constraints for stateless mathematical signal networks."""

from __future__ import annotations

import math
from copy import deepcopy
from random import Random
from typing import Collection, Hashable, Mapping, Sequence

from ameba_graph import Edge, Graph, GraphError, Node

from .operators import EVOLVABLE_OPERATORS, OPERATOR_ARITY, SOFT_OPERATORS
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
    **{kind: {"bias": (-10.0, 10.0, False, 0.0)} for kind in SOFT_OPERATORS},
}


def _parameters_by_name() -> dict[str, _ParameterSpec]:
    """Index the per-kind parameter specs by name.

    Names are shared across kinds wherever the meaning is shared -- ``initial``
    on every stateful kind, ``alpha`` on both filters -- and each shared name
    carries one range. An alphabet is therefore declared per *name*, so a
    single declaration covers every kind that uses that parameter.
    """
    merged: dict[str, _ParameterSpec] = {}
    for specs in _NODE_PARAMETERS.values():
        for name, spec in specs.items():
            existing = merged.get(name)
            if existing is not None and existing[:3] != spec[:3]:
                raise ValueError(
                    f"Parameter {name!r} has conflicting limits across node kinds"
                )
            merged[name] = spec
    return merged


_NODE_PARAMETERS_BY_NAME = _parameters_by_name()

#: A coarse alphabet for every signal parameter, for runs where a parameter is a
#: symbol rather than a point on a continuum. Size is the whole point: with six
#: weights a redraw lands on any given one time in five, which mutation and
#: selection can search, while a continuum of values makes a redraw a lottery
#: that almost never lands. ``initial`` and ``phase`` are single-valued because
#: they are starting offsets rather than dynamics; leaving them free would add
#: loci that a replacement can only disrupt.
COARSE_ALLELES: dict[str, tuple[float | int, ...]] = {
    "weight": (-2.0, -1.0, -0.5, 0.5, 1.0, 2.0),
    "alpha": (0.1, 0.25, 0.5, 0.75, 0.9),
    "initial": (0.0,),
    "initial_input": (0.0,),
    "gain": (-2.0, -1.0, 1.0, 2.0),
    "value": (-1.0, 0.0, 1.0),
    "phase": (0.0,),
    "steps": (1, 2, 4, 8),
    "time_step": (0.1, 1.0, 10.0),
}

#: The named alphabets ``--parameter-alphabet`` can select.
PARAMETER_ALPHABETS: dict[str, dict[str, tuple[float | int, ...]]] = {
    "coarse": COARSE_ALLELES,
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

    #: Parameter alphabet per name; empty means continuous values. Declared
    #: here as well as set in ``__init__`` so an instance restored from state
    #: that predates the field still resolves it. A policy crosses the
    #: process-pool boundary as a pickle, and a worker that unpickles older
    #: state has no such key in it.
    alleles: Mapping[str, tuple[float | int, ...]] = {}

    def __init__(
        self,
        evolvable_kinds: tuple[str, ...] = EVOLVABLE_OPERATORS,
        weight_range: tuple[float, float] = (-2.0, 2.0),
        alleles: Mapping[str, Sequence[float | int]] | None = None,
        required_kind_groups: Sequence[Collection[str]] = (),
        forbidden_kinds: Collection[str] = (),
    ) -> None:
        unknown = set(evolvable_kinds) - set(OPERATOR_ARITY)
        if unknown:
            raise ValueError(f"Unknown signal operator(s): {', '.join(sorted(unknown))}")
        if not evolvable_kinds:
            raise ValueError("At least one evolvable signal operator is required")
        if weight_range[0] > weight_range[1]:
            raise ValueError("Invalid edge weight range")
        alphabets = _validate_alleles(alleles, weight_range)
        groups = tuple(frozenset(group) for group in required_kind_groups)
        if any(not group for group in groups):
            raise ValueError("Required node-kind groups cannot be empty")
        constrained = set().union(*groups, forbidden_kinds) if groups else set(forbidden_kinds)
        unknown_constraints = constrained - set(OPERATOR_ARITY)
        if unknown_constraints:
            raise ValueError(
                f"Unknown constrained operator(s): {', '.join(sorted(unknown_constraints))}"
            )
        self.evolvable_kinds = evolvable_kinds
        self.weight_range = weight_range
        self.alleles = alphabets
        self.required_kind_groups = groups
        self.forbidden_kinds = frozenset(forbidden_kinds)

    def create_node(self, rng: Random) -> Node:
        kind = rng.choice(self.evolvable_kinds)
        attributes = {
            name: self._draw(name, spec, rng)
            for name, spec in _NODE_PARAMETERS.get(kind, {}).items()
        }
        return Node("ignored", kind, attributes)

    def create_edge(self, source: str, target: str, rng: Random) -> Edge:
        alphabet = self.alleles.get("weight")
        weight = (
            rng.uniform(*self.weight_range) if alphabet is None else rng.choice(alphabet)
        )
        return Edge(
            "ignored",
            source,
            target,
            {"weight": weight},
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

        kinds = {node.kind for node in graph.nodes.values()}
        forbidden = kinds & self.forbidden_kinds
        if forbidden:
            raise GraphError(
                f"Graph contains forbidden node kind(s): {', '.join(sorted(forbidden))}"
            )
        for required in self.required_kind_groups:
            if not kinds & required:
                raise GraphError(
                    "Graph requires at least one of: " + ", ".join(sorted(required))
                )

    def mutate_node(self, node: Node, rng: Random) -> Node:
        specs = _NODE_PARAMETERS.get(node.kind, {})
        mutable = [name for name in specs if name not in node.locked_attributes]
        if not mutable:
            raise ValueError(f"Node kind {node.kind} has no mutable parameters")
        name = rng.choice(mutable)
        lower, upper, integer, default = specs[name]
        current = float(node.attributes.get(name, default))
        mutated = deepcopy(node)
        if name in self.alleles:
            mutated.attributes[name] = self._substitute(name, current, rng)
            return mutated
        if integer:
            delta = rng.choice((-1, 1))
            value: float | int = int(_clamp(round(current) + delta, lower, upper))
        else:
            value = _clamp(current + rng.uniform(-0.1, 0.1) * (upper - lower), lower, upper)
        mutated.attributes[name] = value
        return mutated

    def mutate_edge(self, edge: Edge, rng: Random) -> Edge:
        if "weight" in edge.locked_attributes:
            raise ValueError("Edge weight is locked")
        lower, upper = self.weight_range
        current = float(edge.attributes.get("weight", 1.0))
        mutated = deepcopy(edge)
        if "weight" in self.alleles:
            mutated.attributes["weight"] = self._substitute("weight", current, rng)
            return mutated
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
        left_value = float(left.attributes.get(name, default))
        right_value = float(right.attributes.get(name, default))
        crossed = deepcopy(left)
        if name in self.alleles:
            crossed.attributes[name] = self._segregate(name, left_value, right_value, rng)
            return crossed
        value = (left_value + right_value) / 2.0
        crossed.attributes[name] = int(round(_clamp(value, lower, upper))) if integer else _clamp(value, lower, upper)
        return crossed

    def cross_edge(self, left: Edge, right: Edge, rng: Random) -> Edge:
        if "weight" in left.locked_attributes:
            raise ValueError("Edge weight is locked")
        crossed = deepcopy(left)
        left_weight = float(left.attributes.get("weight", 1.0))
        right_weight = float(right.attributes.get("weight", 1.0))
        if "weight" in self.alleles:
            crossed.attributes["weight"] = self._segregate(
                "weight", left_weight, right_weight, rng
            )
            return crossed
        crossed.attributes["weight"] = _clamp(
            (left_weight + right_weight) / 2.0,
            *self.weight_range,
        )
        return crossed

    def _draw(self, name: str, spec: _ParameterSpec, rng: Random) -> float | int:
        """A fresh value for a new carrier: an allele, or the whole range."""
        alphabet = self.alleles.get(name)
        return _random_parameter(spec, rng) if alphabet is None else rng.choice(alphabet)

    def _substitute(self, name: str, current: float | int, rng: Random) -> float | int:
        """A different value from ``name``'s alphabet: a substitution.

        Never the value already there, because a substitution that returns its
        own allele is a wasted mutation. Unlike a nudge, which moves by a
        tenth of the range, this redraws the locus from its alphabet.
        """
        alphabet = self.alleles[name]
        alternatives = tuple(value for value in alphabet if value != current)
        if not alternatives:
            raise ValueError(f"Parameter alphabet {name!r} offers no alternative value")
        return rng.choice(alternatives)

    def _segregate(
        self, name: str, left: float, right: float, rng: Random
    ) -> float | int:
        """Give the child one parent's value for ``name``, placed back on the alphabet.

        Segregation rather than averaging: with a finite alphabet the midpoint
        of two alleles is usually not an allele, and always rounding it the
        same way makes the operator predictable instead of combinatorial. The
        child therefore inherits, and only mutation can invent. A parent value
        that is off the alphabet -- an imported graph, say -- is repaired onto
        it rather than propagated.
        """
        chosen = rng.choice((left, right))
        alphabet = self.alleles[name]
        return min(alphabet, key=lambda allele: (abs(float(allele) - chosen), float(allele)))

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


def _validate_alleles(
    alleles: Mapping[str, Sequence[float | int]] | None,
    weight_range: tuple[float, float],
) -> dict[str, tuple[float | int, ...]]:
    """Normalise a parameter alphabet into sorted, deduplicated tuples.

    ``None`` keeps the continuous behaviour, where a value is drawn uniformly
    over its range. Supplying an alphabet makes every created, mutated and
    crossed value a choice between the listed values, so a parameter becomes a
    symbol from a finite set rather than a point on a continuum -- which is
    what makes replacing a carrier a sensible event rather than a lottery.

    A value must already lie inside the parameter's own limits: an alphabet
    narrows a range, it does not widen one. Membership is deliberately not
    enforced on values already in a graph, so an imported model carrying an
    off-alphabet weight still validates; the alphabet governs what search may
    create, not what a loaded model may contain.
    """
    if alleles is None:
        return {}
    known = ", ".join(sorted({"weight"} | set(_NODE_PARAMETERS_BY_NAME)))
    normalised: dict[str, tuple[float | int, ...]] = {}
    for name, values in alleles.items():
        if name == "weight":
            lower, upper, integer = weight_range[0], weight_range[1], False
        else:
            spec = _NODE_PARAMETERS_BY_NAME.get(name)
            if spec is None:
                raise ValueError(
                    f"Unknown parameter alphabet {name!r}; known parameters: {known}"
                )
            lower, upper, integer, _ = spec
        entries = list(values)
        if not entries:
            raise ValueError(f"Parameter alphabet {name!r} cannot be empty")
        cleaned: list[float | int] = []
        for value in entries:
            if isinstance(value, bool) or not isinstance(value, (int, float)):
                raise ValueError(f"Parameter alphabet {name!r} must hold numbers")
            numeric = float(value)
            if not math.isfinite(numeric) or not lower <= numeric <= upper:
                raise ValueError(
                    f"Parameter alphabet {name!r} value {value!r} is outside {lower}..{upper}"
                )
            if integer and numeric != int(numeric):
                raise ValueError(f"Parameter alphabet {name!r} must hold integers")
            cleaned.append(int(numeric) if integer else numeric)
        normalised[name] = tuple(sorted(set(cleaned)))
    return normalised


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
