"""Domain-neutral random graph generation."""

from __future__ import annotations

from copy import deepcopy
from collections.abc import Callable
from dataclasses import dataclass
from random import Random

from ._identity import next_id
from ._wiring import connect, satisfy_inputs
from .model import Edge, Graph, GraphError, Node
from .protocols import GraphPolicy


class GraphGenerationError(RuntimeError):
    """Raised when a policy cannot produce a valid graph within its budget."""


@dataclass(frozen=True, slots=True)
class GenerationConfig:
    min_nodes: int = 1
    max_nodes: int = 8
    edge_probability: float = 0.2
    attempts: int = 100
    connect_isolated: bool = True

    def __post_init__(self) -> None:
        if self.min_nodes < 0 or self.max_nodes < self.min_nodes:
            raise ValueError("Invalid node limits")
        if not 0.0 <= self.edge_probability <= 1.0:
            raise ValueError("edge_probability must be between zero and one")
        if self.attempts < 1:
            raise ValueError("attempts must be positive")


class GraphGenerator:
    def __init__(self, policy: GraphPolicy, config: GenerationConfig | None = None) -> None:
        self.policy = policy
        self.config = config or GenerationConfig()

    def generate(
        self,
        rng: Random,
        scaffold: Graph | None = None,
        accept: Callable[[Graph], bool] | None = None,
    ) -> Graph:
        """Generate a valid graph, optionally populating around a fixed scaffold.

        The scaffold carries whatever the domain fixes in advance and does not
        evolve -- for a signal model, the input and output terminals, whose
        counts are part of the problem rather than something to discover. It is
        copied into every candidate, so the caller's graph is never modified.

        ``accept`` is an optional second gate applied after policy validation.
        Structural validity does not imply a graph can actually be executed --
        a division by zero is only discoverable by running it -- so a caller
        that needs a working graph should pass a check that runs one.
        """
        last_error: Exception | None = None
        rejected = 0
        for _ in range(self.config.attempts):
            try:
                graph = self._generate_candidate(rng, scaffold)
                self.policy.validate(graph)
            except (GraphError, ValueError) as exc:
                last_error = exc
                continue
            if accept is None or accept(graph):
                return graph
            rejected += 1

        if rejected and last_error is None:
            raise GraphGenerationError(
                f"No generated graph passed the acceptance check "
                f"({rejected} of {self.config.attempts} rejected)"
            )
        detail = f": {last_error}" if last_error else ""
        raise GraphGenerationError(f"Could not generate a valid graph{detail}")

    def _generate_candidate(self, rng: Random, scaffold: Graph | None = None) -> Graph:
        graph = scaffold.copy() if scaffold is not None else Graph()
        count = rng.randint(self.config.min_nodes, self.config.max_nodes)
        for _ in range(count):
            proposed = self.policy.create_node(rng)
            node = Node(
                next_id("n", graph.nodes),
                proposed.kind,
                deepcopy(proposed.attributes),
                proposed.locked,
                proposed.locked_attributes,
            )
            graph.add_node(node)

        pairs = [(source, target) for source in graph.nodes for target in graph.nodes]
        rng.shuffle(pairs)
        for source, target in pairs:
            if rng.random() > self.config.edge_probability:
                continue
            if not self.policy.can_connect(graph, source, target):
                continue
            connect(graph, self.policy, source, target, rng)

        if self.config.connect_isolated:
            satisfy_inputs(graph, self.policy, rng)
        return graph


