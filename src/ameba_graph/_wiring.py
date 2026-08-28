"""Shared edge-wiring helpers for generation and reproduction.

A node that a domain requires to have inputs cannot be introduced unconnected:
it would fail validation the moment it appears. Both random generation and the
node-adding mutations therefore need the same repair step, and it lives here so
the two cannot drift apart.
"""

from __future__ import annotations

from copy import deepcopy
from random import Random
from typing import Iterable

from ._identity import next_id
from .model import Edge, Graph
from .protocols import GraphPolicy


def connect(graph: Graph, policy: GraphPolicy, source: str, target: str, rng: Random) -> Edge:
    """Add a policy-built edge between two existing nodes."""
    proposed = policy.create_edge(source, target, rng)
    edge = Edge(
        next_id("e", graph.edges),
        source,
        target,
        deepcopy(proposed.attributes),
        proposed.source_locked,
        proposed.target_locked,
        proposed.locked_attributes,
    )
    graph.add_edge(edge)
    return edge


def satisfy_inputs(
    graph: Graph,
    policy: GraphPolicy,
    rng: Random,
    targets: Iterable[str] | None = None,
) -> None:
    """Give nodes that still need incoming edges a chance to receive them.

    A policy reports what it still needs through the optional
    ``requires_more_inputs`` hook. Without that hook the rule is simply that a
    node should not be left unconnected. Either way the work is bounded: a node
    cannot need more edges than the graph has nodes.
    """
    requires = getattr(policy, "requires_more_inputs", None)
    chosen = sorted(graph.nodes) if targets is None else list(targets)

    for target in chosen:
        for _ in range(len(graph.nodes)):
            if requires is not None:
                if not requires(graph, target):
                    break
            elif graph.incoming(target):
                break

            sources = sorted(graph.nodes)
            rng.shuffle(sources)
            source = next(
                (item for item in sources if policy.can_connect(graph, item, target)),
                None,
            )
            if source is None:
                break
            connect(graph, policy, source, target, rng)
