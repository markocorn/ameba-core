import math
import unittest
from random import Random

from ameba_genome import Grammar
from ameba_genome.colony import ColonyEncoding
from ameba_genome.statespace import StateSpaceConfig, StateSpaceEncoding
from ameba_genome.tags import TagEncoding
from ameba_signal import SignalSimulator

#: Kinds that never fail on any input, so every random model simulates.
SAFE = ("add", "multiply", "negate", "tanh", "sin", "delay", "constant")
CONFIG = StateSpaceConfig(output_kinds="static", output_sources=2, source_layout="inputs-last", inheritance="maternal")


def response(graph, inputs=1, steps=30):
    session = SignalSimulator().start(graph)
    return [session.step(tuple(math.sin(0.3 * k + c) for c in range(inputs))) for k in range(steps)]


class ColonyTests(unittest.TestCase):
    def test_a_sum_colony_adds_its_cells_outputs(self) -> None:
        make = lambda g: StateSpaceEncoding(g, config=CONFIG)
        colony = ColonyEncoding(Grammar(kinds=SAFE), make, count=2, mode="sum")
        rng = Random(1)
        for _ in range(20):
            genome = colony.random_genome(rng)
            a, b = colony.parts(genome)
            single = make(Grammar(kinds=SAFE))
            together = response(colony.build(genome))
            apart = [(x[0] + y[0],) for x, y in zip(response(single.build(a)), response(single.build(b)))]
            for (got,), (want,) in zip(together, apart):
                self.assertAlmostEqual(want, got, places=12)

    def test_a_chain_cell_reads_the_earlier_cells(self) -> None:
        colony = ColonyEncoding(Grammar(kinds=SAFE), lambda g: StateSpaceEncoding(g, config=CONFIG), count=3, mode="chain")
        self.assertEqual([1, 2, 3], [cell.grammar.inputs for cell in colony.cells])
        rng = Random(2)
        reads_other = 0
        for _ in range(40):
            graph = colony.build(colony.random_genome(rng))
            reads_other += any(e.source.startswith("k0_y") and e.target.startswith("k1_") for e in graph.edges.values())
            response(graph)  # one-way readings: never an algebraic loop
        self.assertGreater(reads_other, 0)

    def test_variation_touches_one_cell_and_keeps_the_layout(self) -> None:
        for make in (lambda g: StateSpaceEncoding(g, config=CONFIG), lambda g: TagEncoding(g, CONFIG, 32)):
            colony = ColonyEncoding(Grammar(2, 2, SAFE), make, count=3, mode="chain")
            rng = Random(3)
            genome, other = colony.random_genome(rng), colony.random_genome(rng)
            for _ in range(60):
                child = colony.mutate(genome, rng)
                changed = sum(p != q for p, q in zip(colony.parts(genome), colony.parts(child)))
                self.assertLessEqual(changed, 1)
                genome = colony.crossover(child, other, rng) if rng.random() < 0.3 else child
                self.assertEqual(len(genome), 3 + sum(genome[:3]))
                response(colony.build(genome), inputs=2)

    def test_bad_settings_are_rejected(self) -> None:
        make = lambda g: StateSpaceEncoding(g, config=CONFIG)
        with self.assertRaises(ValueError):
            ColonyEncoding(Grammar(kinds=SAFE), make, count=0)
        with self.assertRaises(ValueError):
            ColonyEncoding(Grammar(kinds=SAFE), make, mode="ring")


if __name__ == "__main__":
    unittest.main()
