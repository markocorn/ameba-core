import math
import unittest
from random import Random

from ameba_genome import Grammar
from ameba_genome.codon import CodonConfig, CodonEncoding
from ameba_genome.statespace import StateSpaceConfig, StateSpaceEncoding
from ameba_genome.tags import TagEncoding
from ameba_graph.serialization import graph_dumps
from ameba_signal import SignalSimulator

CONFIG = StateSpaceConfig(output_kinds="static", output_sources=1, source_layout="inputs-last")
W1 = 13  # weight 1.0


def response(graph, steps=40):
    session = SignalSimulator().start(graph)
    return [session.step((math.sin(0.3 * k) + 0.2,))[0] for k in range(steps)]


class TagTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tags = TagEncoding(Grammar(), CONFIG, tags=16)
        self.kind = {chromosome: {kind: i for i, kind in enumerate(kinds)}
                     for chromosome, kinds in self.tags.kinds.items()}
        self.u_state = 16 + 1  # tags, then y0, then u0
        self.u_output = 16  # tags, then u0

    def state(self, kind, tag, source, other=None):
        return [self.kind["state"][kind], 0, tag, source, W1, self.u_state if other is None else other, W1]

    def output(self, kind, tag, a, b):
        return [self.kind["output"][kind], 0, tag, a, W1, b, W1]

    def genome(self, states, outputs, terminal):
        return (len(states),) + tuple(g for r in states for g in r) + tuple(g for r in outputs for g in r) + (terminal, W1)

    def test_sources_bind_to_the_nearest_tag(self) -> None:
        # x0 (tag 5) reads u; x1 (tag 9) squares it. h0 wants tag 6: x0 fits best.
        states = [self.state("add", 5, self.u_state), self.state("square", 9, self.u_state)]
        near = self.genome(states, [self.output("multiply", 3, 6, self.u_output)], 3)
        exact = self.genome(states, [self.output("multiply", 3, 5, self.u_output)], 3)
        self.assertEqual(response(self.tags.build(near)), response(self.tags.build(exact)))
        far = self.genome(states, [self.output("multiply", 3, 8, self.u_output)], 3)  # x1 fits best
        self.assertNotEqual(response(self.tags.build(near)), response(self.tags.build(far)))

    def test_a_record_nothing_binds_to_changes_nothing(self) -> None:
        states = [self.state("add", 5, self.u_state)]
        outputs = [self.output("multiply", 9, 5, self.u_output), self.output("add", 11, 9, self.u_output)]
        base = self.genome(states, outputs, 11)
        crowded = self.genome([self.state("tanh", 14, self.u_state)] + states,
                              [self.output("sin", 1, 1, self.u_output)] + outputs, 11)
        self.assertEqual(response(self.tags.build(base)), response(self.tags.build(crowded)))

    def test_position_addressing_rewires_where_tags_do_not(self) -> None:
        """The same insertion in the state-space encoding moves what x0 means."""
        plain = StateSpaceEncoding(Grammar(), config=CONFIG)
        kind = {k: i for i, k in enumerate(plain.kinds["state"])}
        okind = {k: i for i, k in enumerate(plain.kinds["output"])}
        u_state, u_output = 17, 24  # inputs-last codes
        x0, back1 = 0, 16
        state = [kind["add"], 0, u_state, W1, 15, W1]
        extra = [kind["tanh"], 0, u_state, W1, 15, W1]
        out = [okind["multiply"], 0, x0, W1, u_output, W1]
        base = (1,) + tuple(state) + tuple(out) + (back1, W1)
        crowded = (2,) + tuple(extra) + tuple(state) + tuple(out) + (back1, W1)
        self.assertNotEqual(response(plain.build(base)), response(plain.build(crowded)))

    def test_any_genome_builds_and_variation_keeps_the_layout(self) -> None:
        for config in (CONFIG, StateSpaceConfig(output_kinds="static", output_sources=2, inheritance="maternal")):
            tags = TagEncoding(Grammar(), config, tags=8)
            rng = Random(1)
            for _ in range(20):
                genome, other = tags.random_genome(rng), tags.random_genome(rng)
                for _ in range(40):
                    genome = tags.mutate(genome, rng)
                    if rng.random() < 0.3:
                        genome = tags.crossover(genome, other, rng)
                    self.assertEqual(0, (len(genome) - 1 - tags.tail()) % 7)
                    for position in range(1, len(genome)):
                        self.assertLess(genome[position], tags.size_at(genome, position))
                    tags.build(genome)

    def test_fresh_genomes_wire_like_the_state_space_ones(self) -> None:
        """Fresh records read their targets by exact tag, so they use as many nodes."""
        config = StateSpaceConfig(output_kinds="static", output_sources=2, source_layout="inputs-last")
        plain = StateSpaceEncoding(Grammar(), config=config)
        tags = TagEncoding(Grammar(), config, tags=64)  # few tag collisions
        rng_a, rng_b = Random(2), Random(2)
        size = lambda graph: len(graph.nodes)
        a = sum(size(plain.build(plain.random_genome(rng_a))) for _ in range(300)) / 300
        b = sum(size(tags.build(tags.random_genome(rng_b))) for _ in range(300)) / 300
        self.assertAlmostEqual(a, b, delta=0.15 * a)

    def test_codon_genomes_can_carry_tags(self) -> None:
        codon = CodonEncoding(Grammar(), CONFIG, CodonConfig(tags=16, indels="both", junk=2, reset="role"))
        self.assertIsInstance(codon.inner, TagEncoding)
        rng = Random(3)
        for _ in range(20):
            genome = codon.random_genome(rng)
            self.assertEqual(graph_dumps(codon.inner.build(codon.fixed(genome))), graph_dumps(codon.build(genome)))
            for _ in range(30):
                genome = codon.mutate(genome, rng)
                codon.build(genome)

    def test_role_reset_draws_inputs_as_often_as_the_state_space_encoding(self) -> None:
        codon = CodonEncoding(Grammar(), CONFIG, CodonConfig(indels="record", reset="role"))
        genome = codon.random_genome(Random(4))
        cut = 1 + genome[0]
        source = 1 + codon.scan(genome[1:cut])[0][0] + 3  # first state record, first source
        self.assertEqual(18, codon._role_size(genome, source))
        self.assertEqual(codon.codon.alphabet, codon._role_size(genome, 1 + codon.scan(genome[1:cut])[0][0]))


if __name__ == "__main__":
    unittest.main()
