"""Reusable structural mutations for arbitrary directed graphs."""

from __future__ import annotations

from copy import deepcopy
from random import Random
from typing import Sequence

from ._identity import next_id
from ._wiring import satisfy_inputs
from .model import Edge, Graph, GraphError, Node
from .protocols import GraphMutation, GraphPolicy


class MutationError(RuntimeError):
    """Raised when a requested mutation has no valid result."""


class AddNode:
    """Introduce a node and wire up whatever inputs the domain requires.

    An unconnected node cannot satisfy an arity minimum, so adding one without
    connecting it would fail validation every time.
    """

    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        child = graph.copy()
        proposed = policy.create_node(rng)
        node = Node(
            next_id("n", child.nodes),
            proposed.kind,
            deepcopy(proposed.attributes),
            proposed.locked,
            proposed.locked_attributes,
        )
        child.add_node(node)
        satisfy_inputs(child, policy, rng, targets=[node.id])
        return _validated(child, policy)


class RemoveNode:
    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        candidates = sorted(
            node.id
            for node in graph.nodes.values()
            if not node.locked and not any(edge.locked for edge in graph.incoming(node.id) + graph.outgoing(node.id))
        )
        rng.shuffle(candidates)
        for node_id in candidates:
            child = graph.copy()
            child.remove_node(node_id)
            if _is_valid(child, policy):
                return child
        raise MutationError("No node can be removed without violating the graph policy")


class AddEdge:
    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        pairs = [(source, target) for source in sorted(graph.nodes) for target in sorted(graph.nodes)]
        rng.shuffle(pairs)
        for source, target in pairs:
            child = graph.copy()
            if not policy.can_connect(child, source, target):
                continue
            proposed = policy.create_edge(source, target, rng)
            child.add_edge(
                Edge(
                    next_id("e", child.edges),
                    source,
                    target,
                    deepcopy(proposed.attributes),
                    proposed.source_locked,
                    proposed.target_locked,
                    proposed.locked_attributes,
                )
            )
            if _is_valid(child, policy):
                return child
        raise MutationError("No valid edge can be added")


class RemoveEdge:
    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        candidates = sorted(edge.id for edge in graph.edges.values() if not edge.locked)
        rng.shuffle(candidates)
        for edge_id in candidates:
            child = graph.copy()
            child.remove_edge(edge_id)
            if _is_valid(child, policy):
                return child
        raise MutationError("No edge can be removed without violating the graph policy")


class SplitEdge:
    """Replace one edge with source -> new node -> target."""

    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        candidates = sorted(edge.id for edge in graph.edges.values() if not edge.locked)
        rng.shuffle(candidates)
        for edge_id in candidates:
            child = graph.copy()
            old_edge = child.remove_edge(edge_id)
            proposed_node = policy.create_node(rng)
            node = Node(
                next_id("n", child.nodes),
                proposed_node.kind,
                deepcopy(proposed_node.attributes),
                proposed_node.locked,
                proposed_node.locked_attributes,
            )
            child.add_node(node)
            if not policy.can_connect(child, old_edge.source, node.id):
                continue
            first = policy.create_edge(old_edge.source, node.id, rng)
            child.add_edge(
                Edge(
                    next_id("e", child.edges),
                    old_edge.source,
                    node.id,
                    deepcopy(first.attributes),
                    first.source_locked,
                    first.target_locked,
                    first.locked_attributes,
                )
            )
            if not policy.can_connect(child, node.id, old_edge.target):
                continue
            second = policy.create_edge(node.id, old_edge.target, rng)
            child.add_edge(
                Edge(
                    next_id("e", child.edges),
                    node.id,
                    old_edge.target,
                    deepcopy(second.attributes),
                    second.source_locked,
                    second.target_locked,
                    second.locked_attributes,
                )
            )
            if _is_valid(child, policy):
                return child
        raise MutationError("No edge can be split with a policy-provided node")


class MutateNodeAttributes:
    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        candidates = sorted(node.id for node in graph.nodes.values() if not node.locked)
        rng.shuffle(candidates)
        for node_id in candidates:
            child = graph.copy()
            original = child.nodes[node_id]
            try:
                proposed = policy.mutate_node(deepcopy(original), rng)
                _require_same_node_identity(original, proposed)
                _require_locked_attributes(original.attributes, proposed.attributes, original.locked_attributes)
                child.nodes[node_id] = proposed
                return _validated(child, policy)
            except (MutationError, GraphError, ValueError):
                continue
        raise MutationError("No node has mutable attributes accepted by the graph policy")


class MutateEdgeAttributes:
    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        candidates = sorted(edge.id for edge in graph.edges.values())
        rng.shuffle(candidates)
        for edge_id in candidates:
            child = graph.copy()
            original = child.edges[edge_id]
            try:
                proposed = policy.mutate_edge(deepcopy(original), rng)
                _require_same_edge_identity(original, proposed)
                _require_locked_attributes(original.attributes, proposed.attributes, original.locked_attributes)
                child.edges[edge_id] = proposed
                return _validated(child, policy)
            except (MutationError, GraphError, ValueError):
                continue
        raise MutationError("No edge has mutable attributes accepted by the graph policy")


class MoveEdgeSource:
    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        edge_ids = sorted(edge.id for edge in graph.edges.values() if not edge.source_locked)
        rng.shuffle(edge_ids)
        for edge_id in edge_ids:
            sources = sorted(graph.nodes)
            rng.shuffle(sources)
            for source in sources:
                original = graph.edges[edge_id]
                if source == original.source:
                    continue
                child = graph.copy()
                edge = child.remove_edge(edge_id)
                if not policy.can_connect(child, source, edge.target):
                    continue
                edge.source = source
                child.add_edge(edge)
                if _is_valid(child, policy):
                    return child
        raise MutationError("No edge source can be moved without violating the graph policy")


class MoveEdgeTarget:
    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        edge_ids = sorted(edge.id for edge in graph.edges.values() if not edge.target_locked)
        rng.shuffle(edge_ids)
        for edge_id in edge_ids:
            targets = sorted(graph.nodes)
            rng.shuffle(targets)
            for target in targets:
                original = graph.edges[edge_id]
                if target == original.target:
                    continue
                child = graph.copy()
                edge = child.remove_edge(edge_id)
                if not policy.can_connect(child, edge.source, target):
                    continue
                edge.target = target
                child.add_edge(edge)
                if _is_valid(child, policy):
                    return child
        raise MutationError("No edge target can be moved without violating the graph policy")


class ReplaceNode:
    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        candidates = sorted(
            node.id
            for node in graph.nodes.values()
            if not node.locked
            and not any(edge.target_locked for edge in graph.incoming(node.id))
            and not any(edge.source_locked for edge in graph.outgoing(node.id))
        )
        rng.shuffle(candidates)
        for node_id in candidates:
            child = graph.copy()
            proposed = policy.create_node(rng)
            replacement = Node(
                node_id,
                proposed.kind,
                deepcopy(proposed.attributes),
                proposed.locked,
                proposed.locked_attributes,
            )
            child.nodes[node_id] = replacement
            if _is_valid(child, policy):
                return child
        raise MutationError("No node can be replaced without violating the graph policy")


class RemoveNodeBypass:
    """Remove a node and reconnect each former target to an incoming source."""

    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        candidates = sorted(
            node.id
            for node in graph.nodes.values()
            if not node.locked
            and graph.incoming(node.id)
            and graph.outgoing(node.id)
            and not any(edge.locked for edge in graph.incoming(node.id) + graph.outgoing(node.id))
        )
        rng.shuffle(candidates)
        for node_id in candidates:
            incoming_sources = sorted({edge.source for edge in graph.incoming(node_id)})
            outgoing_targets = sorted({edge.target for edge in graph.outgoing(node_id)})
            child = graph.copy()
            child.remove_node(node_id)
            valid = True
            for target in outgoing_targets:
                sources = incoming_sources.copy()
                rng.shuffle(sources)
                source = next(
                    (candidate for candidate in sources if policy.can_connect(child, candidate, target)),
                    None,
                )
                if source is None:
                    valid = False
                    break
                proposed = policy.create_edge(source, target, rng)
                child.add_edge(
                    Edge(
                        next_id("e", child.edges),
                        source,
                        target,
                        deepcopy(proposed.attributes),
                        proposed.source_locked,
                        proposed.target_locked,
                        proposed.locked_attributes,
                    )
                )
            if valid and _is_valid(child, policy):
                return child
        raise MutationError("No node can be bypassed without violating the graph policy")


class RandomMutation:
    """Choose one mutation uniformly using the supplied deterministic RNG."""

    def __init__(self, mutations: Sequence[GraphMutation]) -> None:
        if not mutations:
            raise ValueError("At least one mutation is required")
        self.mutations = tuple(mutations)

    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        return rng.choice(self.mutations).mutate(graph, policy, rng)


class MutationPortfolio:
    """Try mutation operators in seeded random order with a finite budget."""

    def __init__(self, mutations: Sequence[GraphMutation]) -> None:
        if not mutations:
            raise ValueError("At least one mutation is required")
        self.mutations = tuple(mutations)

    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        candidates = list(self.mutations)
        rng.shuffle(candidates)
        failures: list[str] = []
        for mutation in candidates:
            try:
                return mutation.mutate(graph, policy, rng)
            except (MutationError, GraphError, ValueError) as exc:
                failures.append(f"{type(mutation).__name__}: {exc}")
        raise MutationError("No mutation succeeded: " + "; ".join(failures))


def _validated(graph: Graph, policy: GraphPolicy) -> Graph:
    graph.validate_structure()
    try:
        policy.validate(graph)
    except (GraphError, ValueError) as exc:
        raise MutationError(str(exc)) from exc
    return graph


def _is_valid(graph: Graph, policy: GraphPolicy) -> bool:
    try:
        _validated(graph, policy)
    except MutationError:
        return False
    return True


def _require_same_node_identity(original: Node, proposed: Node) -> None:
    if (proposed.id, proposed.kind, proposed.locked, proposed.locked_attributes) != (
        original.id,
        original.kind,
        original.locked,
        original.locked_attributes,
    ):
        raise MutationError("Node attribute mutation changed node identity or locks")


def _require_same_edge_identity(original: Edge, proposed: Edge) -> None:
    if (
        proposed.id,
        proposed.source,
        proposed.target,
        proposed.source_locked,
        proposed.target_locked,
        proposed.locked_attributes,
    ) != (
        original.id,
        original.source,
        original.target,
        original.source_locked,
        original.target_locked,
        original.locked_attributes,
    ):
        raise MutationError("Edge attribute mutation changed edge identity, endpoints, or locks")


def _require_locked_attributes(
    original: dict[str, object],
    proposed: dict[str, object],
    locked: frozenset[str],
) -> None:
    if any(original.get(key) != proposed.get(key) for key in locked):
        raise MutationError("Mutation changed a locked attribute")
