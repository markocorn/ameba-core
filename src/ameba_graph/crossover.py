"""Crossover operations for graphs with shared or unrelated identities."""

from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass
from random import Random
from typing import Hashable, Sequence

from ._identity import next_id
from .model import Edge, Graph, GraphError, Node
from .protocols import GraphCrossover, GraphPolicy


class CrossoverError(RuntimeError):
    """Raised when no valid child can be constructed from two parents."""


class UniformGraphCrossover:
    """Mix parent node/edge sets while retaining referential integrity."""

    def __init__(self, attempts: int = 20) -> None:
        if attempts < 1:
            raise ValueError("attempts must be positive")
        self.attempts = attempts

    def cross(self, left: Graph, right: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        for _ in range(self.attempts):
            child = self._candidate(left, right, rng)
            try:
                child.validate_structure()
                policy.validate(child)
                return child
            except (GraphError, ValueError):
                continue
        raise CrossoverError("Could not produce a child accepted by the graph policy")

    def _candidate(self, left: Graph, right: Graph, rng: Random) -> Graph:
        child = Graph(attributes=deepcopy(rng.choice((left, right)).attributes))
        required_left_nodes = {
            endpoint
            for edge in left.edges.values()
            if edge.locked
            for endpoint in (edge.source, edge.target)
        }
        node_ids = sorted(set(left.nodes) | set(right.nodes))
        for node_id in node_ids:
            choices = [parent.nodes[node_id] for parent in (left, right) if node_id in parent.nodes]
            left_node = left.nodes.get(node_id)
            if left_node is not None and (left_node.locked or node_id in required_left_nodes):
                child.add_node(deepcopy(left_node))
                continue
            if len(choices) == 1 and rng.random() < 0.5:
                continue
            node = deepcopy(rng.choice(choices))
            child.add_node(node)

        if not child.nodes and node_ids:
            available = [left.nodes.get(node_ids[0]) or right.nodes[node_ids[0]]]
            child.add_node(deepcopy(available[0]))

        edge_ids = sorted(set(left.edges) | set(right.edges))
        for edge_id in edge_ids:
            choices = [parent.edges[edge_id] for parent in (left, right) if edge_id in parent.edges]
            left_edge = left.edges.get(edge_id)
            edge = deepcopy(left_edge if left_edge is not None and left_edge.locked else rng.choice(choices))
            if edge.source not in child.nodes or edge.target not in child.nodes:
                continue
            if len(choices) == 1 and not edge.locked and rng.random() < 0.5:
                continue
            child.add_edge(edge)
        return child


class AlignedAttributeCrossover:
    """Cross attributes on same-identity nodes/edges using both parents."""

    def cross(self, left: Graph, right: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        shared_nodes = [
            node_id
            for node_id in sorted(set(left.nodes) & set(right.nodes))
            if left.nodes[node_id].kind == right.nodes[node_id].kind
            and not left.nodes[node_id].locked
        ]
        shared_edges = [
            edge_id
            for edge_id in sorted(set(left.edges) & set(right.edges))
            if (left.edges[edge_id].source, left.edges[edge_id].target)
            == (right.edges[edge_id].source, right.edges[edge_id].target)
        ]
        choices = [("node", identifier) for identifier in shared_nodes] + [
            ("edge", identifier) for identifier in shared_edges
        ]
        rng.shuffle(choices)
        for kind, identifier in choices:
            candidate = left.copy()
            try:
                if kind == "node":
                    original = left.nodes[identifier]
                    proposed = policy.cross_node(
                        deepcopy(original), deepcopy(right.nodes[identifier]), rng
                    )
                    _require_node_contract(original, proposed)
                    candidate.nodes[identifier] = proposed
                else:
                    original_edge = left.edges[identifier]
                    proposed_edge = policy.cross_edge(
                        deepcopy(original_edge), deepcopy(right.edges[identifier]), rng
                    )
                    _require_edge_contract(original_edge, proposed_edge)
                    candidate.edges[identifier] = proposed_edge
                policy.validate(candidate)
                return candidate
            except (GraphError, ValueError):
                continue
        raise CrossoverError("No aligned attributes can be crossed under the graph policy")


class CrossoverPortfolio:
    """Try crossover operators in seeded random order with a finite budget."""

    def __init__(self, crossovers: Sequence[GraphCrossover]) -> None:
        if not crossovers:
            raise ValueError("At least one crossover is required")
        self.crossovers = tuple(crossovers)

    def cross(self, left: Graph, right: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        candidates = list(self.crossovers)
        rng.shuffle(candidates)
        failures: list[str] = []
        for crossover in candidates:
            try:
                return crossover.cross(left, right, policy, rng)
            except (CrossoverError, GraphError, ValueError) as exc:
                failures.append(f"{type(crossover).__name__}: {exc}")
        raise CrossoverError("No crossover succeeded: " + "; ".join(failures))


@dataclass(frozen=True, slots=True)
class _Boundary:
    edge_id: str
    internal: str
    external: str
    incoming: bool
    connection_type: Hashable


class InducedSubgraphInsertionCrossover:
    """Copy a connected donor subgraph and recreate its typed boundary."""

    def __init__(self, max_nodes: int = 5, attempts: int = 30) -> None:
        _require_positive_limits(max_nodes, attempts)
        self.max_nodes = max_nodes
        self.attempts = attempts

    def cross(self, left: Graph, right: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        for _ in range(self.attempts):
            donor_ids = _connected_subset(right, policy, rng, self.max_nodes)
            if not donor_ids:
                break
            boundary = _boundary(right, donor_ids, policy)
            if not any(item.incoming for item in boundary) or not any(
                not item.incoming for item in boundary
            ):
                continue
            try:
                candidate, mapping = _copy_induced(left, right, donor_ids)
                original_ids = set(left.nodes)
                for item in boundary:
                    internal = mapping[item.internal]
                    endpoints = _typed_attachment_candidates(
                        candidate,
                        original_ids,
                        internal,
                        item.incoming,
                        item.connection_type,
                        policy,
                    )
                    rng.shuffle(endpoints)
                    attached = False
                    for external in endpoints:
                        source, target = (
                            (external, internal) if item.incoming else (internal, external)
                        )
                        if not policy.can_connect(candidate, source, target):
                            continue
                        proposed = policy.create_edge(source, target, rng)
                        candidate.add_edge(_new_edge(candidate, proposed, source, target))
                        attached = True
                        break
                    if not attached:
                        raise CrossoverError("No typed recipient endpoint for donor boundary")
                candidate.validate_structure()
                policy.validate(candidate)
                return candidate
            except (CrossoverError, GraphError, ValueError):
                continue
        raise CrossoverError("No connected donor subgraph can be inserted")


class TypedSubgraphReplacementCrossover:
    """Replace a connected primary subgraph through a complete typed boundary map."""

    def __init__(self, max_nodes: int = 5, attempts: int = 40) -> None:
        _require_positive_limits(max_nodes, attempts)
        self.max_nodes = max_nodes
        self.attempts = attempts

    def cross(self, left: Graph, right: Graph, policy: GraphPolicy, rng: Random) -> Graph:
        for _ in range(self.attempts):
            replaced_ids = _connected_subset(left, policy, rng, self.max_nodes, primary=True)
            donor_ids = _connected_subset(right, policy, rng, self.max_nodes)
            if not replaced_ids or not donor_ids:
                break
            left_boundary = _boundary(left, replaced_ids, policy)
            donor_boundary = _boundary(right, donor_ids, policy)
            mapping = _match_boundaries(left_boundary, donor_boundary, rng)
            if mapping is None:
                continue
            try:
                candidate = left.copy()
                preserved_edges = {
                    item.edge_id: deepcopy(left.edges[item.edge_id]) for item in left_boundary
                }
                for node_id in sorted(replaced_ids):
                    candidate.remove_node(node_id)
                candidate, node_mapping = _copy_induced(candidate, right, donor_ids)
                donor_by_key = {
                    (item.edge_id, item.internal, item.external, item.incoming): item
                    for item in donor_boundary
                }
                for left_item, donor_key in mapping:
                    donor_item = donor_by_key[donor_key]
                    edge = preserved_edges[left_item.edge_id]
                    internal = node_mapping[donor_item.internal]
                    if left_item.incoming:
                        edge.target = internal
                    else:
                        edge.source = internal
                    candidate.add_edge(edge)
                candidate.validate_structure()
                policy.validate(candidate)
                return candidate
            except (CrossoverError, GraphError, ValueError):
                continue
        raise CrossoverError("No typed connected subgraphs can be replaced")


def _require_positive_limits(max_nodes: int, attempts: int) -> None:
    if max_nodes < 1 or attempts < 1:
        raise ValueError("max_nodes and attempts must be positive")


def _connected_subset(
    graph: Graph,
    policy: GraphPolicy,
    rng: Random,
    max_nodes: int,
    primary: bool = False,
) -> set[str]:
    candidates = sorted(
        node.id
        for node in graph.nodes.values()
        if policy.can_transfer_node(node) and (not primary or not node.locked)
    )
    if primary:
        candidates = [
            node_id
            for node_id in candidates
            if not any(
                edge.locked
                for edge in graph.incoming(node_id) + graph.outgoing(node_id)
            )
        ]
    if not candidates:
        return set()
    seed = rng.choice(candidates)
    limit = rng.randint(1, min(max_nodes, len(candidates)))
    selected = {seed}
    while len(selected) < limit:
        neighbors = sorted(
            {
                edge.target if edge.source in selected else edge.source
                for edge in graph.edges.values()
                if (edge.source in selected) != (edge.target in selected)
                and (edge.target if edge.source in selected else edge.source) in candidates
            }
        )
        if not neighbors:
            break
        rng.shuffle(neighbors)
        selected.add(neighbors[0])
    return selected


def _boundary(graph: Graph, selected: set[str], policy: GraphPolicy) -> list[_Boundary]:
    result: list[_Boundary] = []
    for edge in sorted(graph.edges.values(), key=lambda item: item.id):
        source_inside = edge.source in selected
        target_inside = edge.target in selected
        if source_inside == target_inside:
            continue
        incoming = target_inside
        result.append(
            _Boundary(
                edge.id,
                edge.target if incoming else edge.source,
                edge.source if incoming else edge.target,
                incoming,
                policy.connection_type(graph, edge.source, edge.target),
            )
        )
    return result


def _copy_induced(
    recipient: Graph, donor: Graph, selected: set[str]
) -> tuple[Graph, dict[str, str]]:
    candidate = recipient.copy()
    mapping: dict[str, str] = {}
    for node_id in sorted(selected):
        node = deepcopy(donor.nodes[node_id])
        node.id = next_id("n", candidate.nodes)
        mapping[node_id] = node.id
        candidate.add_node(node)
    for edge in sorted(donor.edges.values(), key=lambda item: item.id):
        if edge.source not in selected or edge.target not in selected:
            continue
        copied = deepcopy(edge)
        copied.id = next_id("e", candidate.edges)
        copied.source = mapping[edge.source]
        copied.target = mapping[edge.target]
        candidate.add_edge(copied)
    return candidate, mapping


def _typed_attachment_candidates(
    graph: Graph,
    original_ids: set[str],
    internal: str,
    incoming: bool,
    expected: Hashable,
    policy: GraphPolicy,
) -> list[str]:
    result: list[str] = []
    for external in sorted(original_ids):
        source, target = (external, internal) if incoming else (internal, external)
        try:
            if policy.connection_type(graph, source, target) == expected:
                result.append(external)
        except (GraphError, ValueError):
            continue
    return result


def _new_edge(graph: Graph, proposed: Edge, source: str, target: str) -> Edge:
    return Edge(
        next_id("e", graph.edges),
        source,
        target,
        deepcopy(proposed.attributes),
        proposed.source_locked,
        proposed.target_locked,
        proposed.locked_attributes,
    )


def _match_boundaries(
    primary: list[_Boundary], donor: list[_Boundary], rng: Random
) -> list[tuple[_Boundary, tuple[str, str, str, bool]]] | None:
    remaining = donor.copy()
    rng.shuffle(remaining)
    result: list[tuple[_Boundary, tuple[str, str, str, bool]]] = []
    for item in primary:
        match = next(
            (
                candidate
                for candidate in remaining
                if candidate.incoming == item.incoming
                and candidate.connection_type == item.connection_type
            ),
            None,
        )
        if match is None:
            return None
        remaining.remove(match)
        result.append(
            (item, (match.edge_id, match.internal, match.external, match.incoming))
        )
    if remaining:
        return None
    return result


def _require_node_contract(original: Node, proposed: Node) -> None:
    if (proposed.id, proposed.kind, proposed.locked, proposed.locked_attributes) != (
        original.id,
        original.kind,
        original.locked,
        original.locked_attributes,
    ):
        raise CrossoverError("Node crossover changed identity or locks")
    if any(
        proposed.attributes.get(key) != original.attributes.get(key)
        for key in original.locked_attributes
    ):
        raise CrossoverError("Node crossover changed a locked attribute")


def _require_edge_contract(original: Edge, proposed: Edge) -> None:
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
        raise CrossoverError("Edge crossover changed identity, endpoints, or locks")
    if any(
        proposed.attributes.get(key) != original.attributes.get(key)
        for key in original.locked_attributes
    ):
        raise CrossoverError("Edge crossover changed a locked attribute")
