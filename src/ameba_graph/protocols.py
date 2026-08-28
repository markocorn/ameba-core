"""Extension boundaries for graph domains and fitness evaluation."""

from __future__ import annotations

from random import Random
from typing import Hashable, Protocol

from .model import Edge, Graph, Node


class GraphPolicy(Protocol):
    """Domain rules used by generic graph generation and reproduction."""

    def create_node(self, rng: Random) -> Node: ...

    def create_edge(self, source: str, target: str, rng: Random) -> Edge: ...

    def can_connect(self, graph: Graph, source: str, target: str) -> bool: ...

    def validate(self, graph: Graph) -> None: ...

    def mutate_node(self, node: Node, rng: Random) -> Node: ...

    def mutate_edge(self, edge: Edge, rng: Random) -> Edge: ...

    def cross_node(self, left: Node, right: Node, rng: Random) -> Node: ...

    def cross_edge(self, left: Edge, right: Edge, rng: Random) -> Edge: ...

    def can_transfer_node(self, node: Node) -> bool: ...

    def connection_type(self, graph: Graph, source: str, target: str) -> Hashable: ...

    # Optional. Generation calls this, when a policy defines it, to repair
    # candidates whose nodes are still short of the inputs the domain requires.
    # A policy that omits it is treated as needing only that nodes be connected.
    def requires_more_inputs(self, graph: Graph, node_id: str) -> bool: ...


class Evaluator(Protocol):
    """Opaque mapping from a candidate graph to a minimization score."""

    def evaluate(self, graph: Graph) -> float: ...


class GraphMutation(Protocol):
    """A domain-neutral operation that derives one candidate from another."""

    def mutate(self, graph: Graph, policy: GraphPolicy, rng: Random) -> Graph: ...


class GraphCrossover(Protocol):
    """A domain-neutral operation that combines two candidate graphs."""

    def cross(
        self,
        left: Graph,
        right: Graph,
        policy: GraphPolicy,
        rng: Random,
    ) -> Graph: ...
