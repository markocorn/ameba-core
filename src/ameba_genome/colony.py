"""Symbiosis of cells: a model made of several whole models working together.

A colony holds ``count`` cells, each a complete genome of another encoding
(state-space, tagged or codon) with states and outputs of its own. The model's
output is the sum of the cells' outputs, so the cells can share the work --
one the linear response, another a nonlinear correction -- and selection
judges only the whole.

``sum``    the cells see the plant's inputs only and add their outputs
``chain``  cell k also sees the outputs of cells 0..k-1, as extra inputs at
           the same step, so a later cell can work on what an earlier one
           made: a correction of its error, a gain on its output. The
           readings run one way only, so no loop can form between cells.

    genome   [length of cell 0, ..., length of cell K-1, cell 0..., cell K-1...]

The leading lengths are not loci. A mutation changes one cell; crossover
recombines one cell with the same cell of the other parent, or takes that
cell over whole, a symbiont passed between hosts.
"""

from __future__ import annotations

from random import Random
from typing import Callable

from ameba_graph import Edge, Graph, Node
from ameba_signal import interface_scaffold

from .decoding import Grammar

Genome = tuple[int, ...]


class ColonyEncoding:
    def __init__(self, grammar: Grammar, make: Callable[[Grammar], object], count: int = 2,
                 mode: str = "sum") -> None:
        if count < 1:
            raise ValueError("a colony needs at least one cell")
        if mode not in ("sum", "chain"):
            raise ValueError("mode must be sum or chain")
        self.grammar = grammar
        self.count = count
        self.mode = mode
        extra = grammar.outputs if mode == "chain" else 0
        self.cells = [make(Grammar(grammar.inputs + k * extra, grammar.outputs, grammar.kinds))
                      for k in range(count)]
        self.width = getattr(self.cells[0], "width", 6)
        self.config = getattr(self.cells[0], "config", None)

    # --- layout ------------------------------------------------------------

    def parts(self, genome: Genome) -> list[Genome]:
        lengths = genome[: self.count]
        parts, at = [], self.count
        for length in lengths:
            parts.append(tuple(genome[at: at + length]))
            at += length
        return parts

    @staticmethod
    def join(parts: list[Genome]) -> Genome:
        return tuple(len(part) for part in parts) + tuple(g for part in parts for g in part)

    # --- building -------------------------------------------------------------

    def build(self, genome: Genome) -> Graph:
        inputs, outputs = self.grammar.inputs, self.grammar.outputs
        graph = interface_scaffold(inputs, outputs)
        for k, (cell, part) in enumerate(zip(self.cells, self.parts(genome))):
            inner = cell.build(part)
            names = {}
            for node in inner.nodes.values():
                if node.kind == "input":
                    index = node.attributes["index"]
                    if index < inputs:
                        names[node.id] = f"u{index}"
                    else:  # an earlier cell's output, in chain mode
                        j = index - inputs
                        names[node.id] = f"k{j // outputs}_y{j % outputs}"
                elif node.kind == "output":
                    names[node.id] = f"k{k}_y{node.attributes['index']}"
                    graph.add_node(Node(names[node.id], "add"))
                else:
                    names[node.id] = f"k{k}_{node.id}"
                    graph.add_node(Node(names[node.id], node.kind, dict(node.attributes)))
            for edge in inner.edges.values():
                graph.add_edge(Edge(f"k{k}_{edge.id}", names[edge.source], names[edge.target], dict(edge.attributes)))
        for channel in range(outputs):
            total = f"sum_y{channel}"
            graph.add_node(Node(total, "add"))
            for k in range(self.count):
                graph.add_edge(Edge(f"e_k{k}_y{channel}_{total}", f"k{k}_y{channel}", total, {"weight": 1.0}))
            graph.add_edge(Edge(f"e_{total}_y{channel}", total, f"y{channel}", {"weight": 1.0}))
        return graph

    # --- creation and variation ---------------------------------------------------

    def random_genome(self, rng: Random) -> Genome:
        return self.join([cell.random_genome(rng) for cell in self.cells])

    def mutate(self, genome: Genome, rng: Random) -> Genome:
        parts = self.parts(genome)
        k = rng.randrange(self.count)
        parts[k] = self.cells[k].mutate(parts[k], rng)
        return self.join(parts)

    def crossover(self, left: Genome, right: Genome, rng: Random) -> Genome:
        parts, others = self.parts(left), self.parts(right)
        k = rng.randrange(self.count)
        parts[k] = self.cells[k].crossover(parts[k], others[k], rng) if rng.random() < 0.5 else others[k]
        return self.join(parts)

    def translate(self, genome: Genome):
        raise NotImplementedError("a colony builds its graph directly; use build()")
