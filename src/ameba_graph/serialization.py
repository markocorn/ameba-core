"""Versioned, deterministic JSON serialization for graph genomes."""

from __future__ import annotations

import json
from copy import deepcopy
from typing import Any, Mapping

from .model import Edge, Graph, GraphError, Node

GRAPH_SCHEMA = "ameba.graph"
GRAPH_SCHEMA_VERSION = 1


class SerializationError(ValueError):
    """Raised when serialized AMEBA data is malformed or unsupported."""


def graph_to_dict(graph: Graph) -> dict[str, Any]:
    graph.validate_structure()
    return {
        "schema": GRAPH_SCHEMA,
        "version": GRAPH_SCHEMA_VERSION,
        "attributes": deepcopy(graph.attributes),
        "nodes": [
            {
                "id": node.id,
                "kind": node.kind,
                "attributes": deepcopy(node.attributes),
                "locks": {
                    "structure": node.locked,
                    "attributes": sorted(node.locked_attributes),
                },
            }
            for node in sorted(graph.nodes.values(), key=lambda item: item.id)
        ],
        "edges": [
            {
                "id": edge.id,
                "source": edge.source,
                "target": edge.target,
                "attributes": deepcopy(edge.attributes),
                "locks": {
                    "source": edge.source_locked,
                    "target": edge.target_locked,
                    "attributes": sorted(edge.locked_attributes),
                },
            }
            for edge in sorted(graph.edges.values(), key=lambda item: item.id)
        ],
    }


def graph_from_dict(payload: Mapping[str, Any]) -> Graph:
    _require_schema(payload, GRAPH_SCHEMA, GRAPH_SCHEMA_VERSION)
    attributes = _mapping(payload.get("attributes", {}), "graph attributes")
    nodes_data = _list(payload.get("nodes"), "nodes")
    edges_data = _list(payload.get("edges"), "edges")
    try:
        nodes = [
            _node_from_dict(item)
            for item in (_mapping(value, "node") for value in nodes_data)
        ]
        edges = [
            _edge_from_dict(item)
            for item in (_mapping(value, "edge") for value in edges_data)
        ]
        return Graph(nodes, edges, deepcopy(dict(attributes)))
    except GraphError as exc:
        raise SerializationError(str(exc)) from exc


def graph_dumps(graph: Graph, *, indent: int | None = 2) -> str:
    try:
        return json.dumps(
            graph_to_dict(graph),
            allow_nan=False,
            indent=indent,
            sort_keys=True,
        )
    except (TypeError, ValueError) as exc:
        raise SerializationError(f"Graph contains non-JSON data: {exc}") from exc


def graph_loads(data: str) -> Graph:
    try:
        payload = json.loads(data)
    except json.JSONDecodeError as exc:
        raise SerializationError(f"Invalid JSON: {exc}") from exc
    return graph_from_dict(_mapping(payload, "graph document"))


def _require_schema(payload: Mapping[str, Any], schema: str, version: int) -> None:
    if payload.get("schema") != schema:
        raise SerializationError(f"Expected schema {schema!r}")
    if payload.get("version") != version:
        raise SerializationError(
            f"Unsupported {schema} version {payload.get('version')!r}; expected {version}"
        )


def _mapping(value: Any, name: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        raise SerializationError(f"{name} must be an object")
    return value


def _list(value: Any, name: str) -> list[Any]:
    if not isinstance(value, list):
        raise SerializationError(f"{name} must be an array")
    return value


def _string(item: Mapping[str, Any], key: str, owner: str) -> str:
    value = item.get(key)
    if not isinstance(value, str) or not value:
        raise SerializationError(f"{owner} {key} must be a non-empty string")
    return value


def _node_from_dict(item: Mapping[str, Any]) -> Node:
    locks = _mapping(item.get("locks", {}), "node locks")
    return Node(
        _string(item, "id", "node"),
        _string(item, "kind", "node"),
        deepcopy(dict(_mapping(item.get("attributes", {}), "node attributes"))),
        _boolean(locks.get("structure", False), "node structure lock"),
        frozenset(_string_list(locks.get("attributes", []), "node attribute locks")),
    )


def _edge_from_dict(item: Mapping[str, Any]) -> Edge:
    locks = _mapping(item.get("locks", {}), "edge locks")
    return Edge(
        _string(item, "id", "edge"),
        _string(item, "source", "edge"),
        _string(item, "target", "edge"),
        deepcopy(dict(_mapping(item.get("attributes", {}), "edge attributes"))),
        _boolean(locks.get("source", False), "edge source lock"),
        _boolean(locks.get("target", False), "edge target lock"),
        frozenset(_string_list(locks.get("attributes", []), "edge attribute locks")),
    )


def _boolean(value: object, name: str) -> bool:
    if not isinstance(value, bool):
        raise SerializationError(f"{name} must be boolean")
    return value


def _string_list(value: object, name: str) -> list[str]:
    if not isinstance(value, list) or not all(isinstance(item, str) for item in value):
        raise SerializationError(f"{name} must be an array of strings")
    return value
