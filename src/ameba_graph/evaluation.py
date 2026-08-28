"""Evaluator wrappers that shape selection without touching a domain.

Accuracy alone does not constrain size, so a search that can add structure will:
neutral additions cost nothing and accumulate. These wrappers stay
domain-neutral -- they only count nodes and edges -- and compose with any
evaluator, because evolution asks for nothing but a number.
"""

from __future__ import annotations

from dataclasses import dataclass
from math import isfinite

from .model import Graph
from .protocols import Evaluator


@dataclass(frozen=True, slots=True)
class ParsimoniousEvaluator:
    """Add a size penalty to another evaluator's score.

    Choosing the weight is a judgement about exchange rate: it is the accuracy
    a candidate must buy to justify one more node. Too small and bloat carries
    on; too large and the search trades away real accuracy for compactness, and
    can stall before it has enough structure to fit anything.

    Candidates already scoring infinity are left alone. They have been rejected
    outright, and ranking rejected candidates by size is meaningless.
    """

    evaluator: Evaluator
    node_weight: float = 0.0
    edge_weight: float = 0.0
    relative: bool = True

    def __post_init__(self) -> None:
        if self.node_weight < 0.0 or self.edge_weight < 0.0:
            raise ValueError("Parsimony weights cannot be negative")

    def evaluate(self, graph: Graph) -> float:
        score = float(self.evaluator.evaluate(graph))
        if not isfinite(score):
            return score
        return score + self.penalty(graph, score)

    def penalty(self, graph: Graph, score: float = 1.0) -> float:
        """Size cost, as a fraction of the score when ``relative`` is set.

        An absolute weight has to be guessed against a fitness scale nobody
        knows in advance, and guessing high is destructive: a weight of 0.05
        against a local gradient of 0.08 vetoes genuine improvements. Guessing
        low is not safe either, because a fixed weight that registers at a
        score of 1 disappears into rounding at a score of 40.

        Scaling by the score removes the guess. The weight becomes the fraction
        of accuracy a node has to earn, which is comparable across problems and
        across a run as the score falls.
        """
        size = self.node_weight * len(graph.nodes) + self.edge_weight * len(graph.edges)
        return size * abs(score) if self.relative else size


def live_nodes(graph: Graph, terminals: frozenset[str] | set[str]) -> set[str]:
    """Nodes that can reach one of ``terminals`` by following edges forward."""
    reached: set[str] = set()
    pending = [node_id for node_id in terminals if node_id in graph.nodes]
    while pending:
        node_id = pending.pop()
        if node_id in reached:
            continue
        reached.add(node_id)
        pending.extend(edge.source for edge in graph.incoming(node_id))
    return reached


def prune(graph: Graph, terminals: frozenset[str] | set[str]) -> Graph:
    """Drop every node that cannot reach a terminal.

    Such a node is computed on every step and its value is never read, so
    removing it cannot change what the graph produces. This is behaviour
    preserving by construction, which is what separates it from a size penalty:
    pruning costs nothing, whereas a penalty trades accuracy for size.
    """
    keep = live_nodes(graph, terminals)
    pruned = graph.copy()
    for node_id in [item for item in pruned.nodes if item not in keep]:
        pruned.remove_node(node_id)
    return pruned
