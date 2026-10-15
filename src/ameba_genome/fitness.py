"""Scoring genomes through their decoded graphs.

Many genomes decode to the same graph -- introns, duplicate genes, a weight
gene overwritten by a later one -- so scores are cached by *phenotype*, not by
genome. On a converging population most children are neutral variants of their
parents, and those cost one decode and no simulation at all.
"""

from __future__ import annotations

from collections import OrderedDict
from math import isfinite
from typing import Callable, Iterable, Sequence

from ameba_graph import Graph
from ameba_signal import SignalEvaluator

from .codebook import Codebook
from .decoding import Genome, Grammar, decode

MapFunction = Callable[[Callable[[Graph], float], Iterable[Graph]], Iterable[float]]


def phenotype_key(graph: Graph) -> tuple:
    """A hashable identity for a decoded graph; equal keys simulate identically."""
    nodes = tuple(
        (node.id, node.kind, tuple(sorted(node.attributes.items())))
        for node in graph.nodes.values()
    )
    edges = tuple(
        (edge.source, edge.target, float(edge.attributes.get("weight", 1.0)))
        for edge in graph.edges.values()
    )
    return nodes, edges


def model_size(graph: Graph) -> int:
    """Nodes other than the fixed interface terminals."""
    return sum(1 for node in graph.nodes.values() if node.kind not in ("input", "output"))


class GenomeFitness:
    """Decode, simulate, and shape: the score the GA selects on.

    ``node_penalty`` is relative, as in the graph engine: the fraction of its
    score each node has to earn. ``raw`` keeps the unshaped error, which is
    what a result is reported and judged against the floor with.
    """

    def __init__(
        self,
        evaluator: SignalEvaluator,
        grammar: Grammar,
        node_penalty: float = 0.0,
        map_function: MapFunction = map,
        codebook: Codebook | None = None,
        max_cache: int = 50_000,
    ) -> None:
        if node_penalty < 0.0:
            raise ValueError("node_penalty cannot be negative")
        self.evaluator = evaluator
        self.grammar = grammar
        self.node_penalty = node_penalty
        self.map_function = map_function
        #: Genomes are in this codebook's numbers; None means dictionary IDs.
        self.codebook = codebook
        # Bounded, least recently used out first: a long, diverse run otherwise
        # keeps every phenotype it ever scored, and two dozen such runs at once
        # exhaust memory. Scoring is deterministic, so an evicted phenotype
        # scores the same when it returns; only ``simulations`` counts it again.
        self._cache: OrderedDict[tuple, float] = OrderedDict()
        self.max_cache = max_cache
        self.simulations = 0

    def decode(self, genome: Genome) -> Graph:
        # An encoding that builds its graph itself -- the state-space one --
        # is used as is; every other one is translated to dictionary genes.
        if self.codebook is not None and hasattr(self.codebook, "build"):
            return self.codebook.build(genome)
        genes = self.codebook.translate(genome) if self.codebook is not None else genome
        return decode(genes, self.grammar)

    def raw(self, genome: Genome) -> float:
        return self.raw_many([genome])[0]

    def raw_many(self, genomes: Sequence[Genome]) -> list[float]:
        graphs = [self.decode(genome) for genome in genomes]
        keys = [phenotype_key(graph) for graph in graphs]
        pending: dict[tuple, Graph] = {}
        for key, graph in zip(keys, graphs):
            if key in self._cache:
                self._cache.move_to_end(key)
            elif key not in pending:
                pending[key] = graph
        known = {key: self._cache[key] for key in keys if key in self._cache}
        if pending:
            scores = self.map_function(self.evaluator.evaluate, list(pending.values()))
            for key, score in zip(pending, scores):
                known[key] = self._cache[key] = float(score)
            self.simulations += len(pending)
            while len(self._cache) > self.max_cache:
                self._cache.popitem(last=False)
        return [known[key] for key in keys]

    def __call__(self, genomes: Sequence[Genome]) -> list[float]:
        """Shaped scores, in the form :class:`GeneticAlgorithm` asks for."""
        raw = self.raw_many(genomes)
        if self.node_penalty == 0.0:
            return raw
        shaped: list[float] = []
        for genome, score in zip(genomes, raw):
            if not isfinite(score):
                shaped.append(float("inf"))
                continue
            size = model_size(self.decode(genome))
            shaped.append(score * (1.0 + self.node_penalty * size))
        return shaped
