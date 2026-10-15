import unittest
from random import Random

from ameba_genome import Grammar
from ameba_genome.codon import CodonConfig, CodonEncoding
from ameba_genome.statespace import WIDTH, StateSpaceConfig, StateSpaceEncoding
from ameba_graph.serialization import graph_dumps

LAST = StateSpaceConfig(output_kinds="static", output_sources=2, source_layout="inputs-last", inheritance="maternal")


def wrap(encoding: CodonEncoding, fixed, junk=()):
    """A state-space genome written with start codons, ``junk`` before each record."""
    tail = encoding.tail()
    records = [list(fixed[1 + i * WIDTH: 1 + (i + 1) * WIDTH]) for i in range((len(fixed) - 1 - tail) // WIDTH)]
    state, output = [], []
    for index, record in enumerate(records):
        (state if index < fixed[0] else output).extend(list(junk) + [encoding.codon.start] + record)
    return (len(state),) + tuple(state) + tuple(output) + tuple(fixed[len(fixed) - tail:])


class CodonTests(unittest.TestCase):
    def setUp(self) -> None:
        self.plain = StateSpaceEncoding(Grammar(), config=LAST)
        self.codon = CodonEncoding(Grammar(), LAST)
        rng = Random(1)
        self.fixed = [self.plain.random_genome(rng) for _ in range(40)]

    def test_records_behind_start_codons_build_the_state_space_graph(self) -> None:
        for fixed in self.fixed:
            self.assertEqual(graph_dumps(self.plain.build(fixed)), graph_dumps(self.codon.build(wrap(self.codon, fixed))))

    def test_numbers_between_records_are_not_read(self) -> None:
        for fixed in self.fixed:
            self.assertEqual(graph_dumps(self.codon.build(wrap(self.codon, fixed))),
                             graph_dumps(self.codon.build(wrap(self.codon, fixed, junk=(3, 0, 17)))))

    def test_a_frameshift_does_not_reach_past_the_next_start_codons(self) -> None:
        for fixed in self.fixed:
            genome = wrap(self.codon, fixed)
            state, output, _ = self.codon._parts(genome)
            if len(self.codon.scan(output)) < 4:
                continue
            cut = 1 + genome[0] + 3  # inside the first output record
            shifted = genome[:cut] + genome[cut + 1:]
            before = self.codon.records(output, "output")
            after = self.codon.records(self.codon._parts(shifted)[1], "output")
            self.assertEqual(before[2:], after[-len(before[2:]):])

    def test_any_genome_builds_and_variation_keeps_the_layout(self) -> None:
        for indels in ("point", "record", "both"):
            codon = CodonEncoding(Grammar(), LAST, CodonConfig(indels=indels, junk=3))
            rng = Random(2)
            for _ in range(20):
                genome = codon.random_genome(rng)
                other = codon.random_genome(rng)
                for _ in range(40):
                    genome = codon.mutate(genome, rng)
                    if rng.random() < 0.3:
                        genome = codon.crossover(genome, other, rng)
                    self.assertTrue(all(0 <= v < codon.codon.alphabet for v in genome[1:]))
                    self.assertEqual(len(genome) - 1 - codon.tail(), genome[0] + len(codon._parts(genome)[1]))
                    codon.build(genome)

    def test_a_fresh_genome_reads_its_records(self) -> None:
        codon = CodonEncoding(Grammar(), LAST, CodonConfig(junk=4))
        rng = Random(3)
        for _ in range(20):
            genome = codon.random_genome(rng)
            state, output, _ = codon._parts(genome)
            self.assertGreaterEqual(len(codon.scan(state)), 1)
            self.assertGreaterEqual(len(codon.scan(output)), 2)

    def test_maternal_crossover_keeps_the_mothers_states(self) -> None:
        rng = Random(4)
        genomes = [self.codon.random_genome(rng) for _ in range(10)]
        for mother, father in zip(genomes, genomes[1:]):
            child = self.codon.crossover(mother, father, rng)
            self.assertEqual(self.codon._parts(mother)[0], self.codon._parts(child)[0])

    def test_bad_settings_are_rejected(self) -> None:
        with self.assertRaises(ValueError):
            CodonConfig(indels="random")
        with self.assertRaises(ValueError):
            CodonEncoding(Grammar(), LAST, CodonConfig(alphabet=16))


if __name__ == "__main__":
    unittest.main()
