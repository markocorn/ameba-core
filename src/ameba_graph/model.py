"""Domain-neutral graph genome types.

This module deliberately contains no simulation or fitness concepts. Node and
edge attributes are opaque data owned by whichever domain interprets the graph.
"""

from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass, field
from typing import Any, Iterable, Mapping


class GraphError(ValueError):
    """Raised when an operation would make a graph structurally invalid."""


@dataclass(slots=True)
class Node:
    id: str
    kind: str
    attributes: dict[str, Any] = field(default_factory=dict)
    locked: bool = False
    locked_attributes: frozenset[str] = field(default_factory=frozenset)

    def __post_init__(self) -> None:
        self.locked_attributes = frozenset(self.locked_attributes)


@dataclass(slots=True)
class Edge:
    id: str
    source: str
    target: str
    attributes: dict[str, Any] = field(default_factory=dict)
    source_locked: bool = False
    target_locked: bool = False
    locked_attributes: frozenset[str] = field(default_factory=frozenset)

    def __post_init__(self) -> None:
        self.locked_attributes = frozenset(self.locked_attributes)

    @property
    def locked(self) -> bool:
        return self.source_locked or self.target_locked or bool(self.locked_attributes)


class Graph:
    """A mutable directed multigraph used as an evolutionary genome."""

    def __init__(
        self,
        nodes: Iterable[Node] = (),
        edges: Iterable[Edge] = (),
        attributes: Mapping[str, Any] | None = None,
    ) -> None:
        self.nodes: dict[str, Node] = {}
        self.edges: dict[str, Edge] = {}
        self.attributes = dict(attributes or {})
        for node in nodes:
            self.add_node(node)
        for edge in edges:
            self.add_edge(edge)

    def add_node(self, node: Node) -> None:
        if node.id in self.nodes:
            raise GraphError(f"Node id already exists: {node.id}")
        self.nodes[node.id] = node

    def add_edge(self, edge: Edge) -> None:
        if edge.id in self.edges:
            raise GraphError(f"Edge id already exists: {edge.id}")
        missing = [node_id for node_id in (edge.source, edge.target) if node_id not in self.nodes]
        if missing:
            raise GraphError(f"Edge {edge.id} references missing node(s): {', '.join(missing)}")
        self.edges[edge.id] = edge

    def remove_edge(self, edge_id: str) -> Edge:
        try:
            return self.edges.pop(edge_id)
        except KeyError as exc:
            raise GraphError(f"Unknown edge: {edge_id}") from exc

    def remove_node(self, node_id: str) -> Node:
        if node_id not in self.nodes:
            raise GraphError(f"Unknown node: {node_id}")
        for edge_id in [
            edge.id
            for edge in self.edges.values()
            if edge.source == node_id or edge.target == node_id
        ]:
            self.remove_edge(edge_id)
        return self.nodes.pop(node_id)

    def incoming(self, node_id: str) -> list[Edge]:
        self._require_node(node_id)
        return [edge for edge in self.edges.values() if edge.target == node_id]

    def outgoing(self, node_id: str) -> list[Edge]:
        self._require_node(node_id)
        return [edge for edge in self.edges.values() if edge.source == node_id]

    def copy(self) -> Graph:
        return deepcopy(self)

    def validate_structure(self) -> None:
        for edge in self.edges.values():
            if edge.source not in self.nodes or edge.target not in self.nodes:
                raise GraphError(f"Edge {edge.id} references a missing node")

    def _require_node(self, node_id: str) -> None:
        if node_id not in self.nodes:
            raise GraphError(f"Unknown node: {node_id}")
