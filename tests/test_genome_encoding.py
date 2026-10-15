import hashlib
import unittest
from random import Random

from ameba.benchmarks import identification_dataset, linear, training_controls
from ameba_genome import (
    NUMERIC_MUTATIONS,
    GAConfig,
    GENES,
    GeneticAlgorithm,
    GenomeFitness,
    Grammar,
    allowed_at,
    decode,
    dense,
    hashed,
    mutate,
    numeric_mutate,
    one_point,
    ordered,
    random_genome,
    splice,
)
from ameba_genome.dictionary import (
    CORE_GENES,
    CONNECTION,
    PARAMETER,
    PARAMETER_LADDERS,
    STRUCTURAL,
    TEMPORAL,
    input_gene,
    node_gene,
    output_gene,
    parameter_gene,
    source_gene,
    weight_gene,
)
from ameba_genome.fixed import GENETIC_CODE, SCHEMES, SILENT, FixedConfig, FixedEncoding
from ameba_genome.variation import MUTATIONS
from ameba_graph.serialization import graph_dumps
from ameba_signal import (
    SignalEvaluator,
    SignalGraphPolicy,
    SignalSimulationError,
    SignalSimulator,
)


def first_order_genome() -> tuple[int, ...]:
    """y(k+1) = 0.5 y(k) + 0.5 u(k), written as genes.

    n0 sums the input and the delayed response, which lives one record ahead
    in n1 -- legal because the source is a delay. n1 delays n0.
    """
    return (
        node_gene("add"), input_gene(0), weight_gene(0.5), source_gene(+1), weight_gene(0.5),
        node_gene("delay"), parameter_gene("steps", 1), source_gene(-1),
        output_gene(0), source_gene(-2),
    )


class DictionaryTests(unittest.TestCase):
    def test_ids_are_permanent(self) -> None:
        """Pinned: changing any of these breaks every saved genome."""
        pinned = {
            1: "node:add", 12: "node:delay", 17: "node:constant",
            50: "out:y0", 100: "src:u0", 110: "src:-1", 150: "src:+1",
            200: "w=-2", 211: "w=0.5", 300: "gain=-2", 323: "alpha=0.5",
            600: "steps=1", 620: "time_step=0.1",
        }
        for gene_id, text in pinned.items():
            self.assertEqual(text, GENES[gene_id].label)

    def test_every_category_is_present(self) -> None:
        categories = {gene.category for gene in GENES.values()}
        self.assertEqual({STRUCTURAL, CONNECTION, PARAMETER, TEMPORAL}, categories)

    def test_parameter_values_are_inside_policy_limits(self) -> None:
        policy = SignalGraphPolicy()
        for gene in GENES.values():
            if gene.family == "weight":
                self.assertTrue(policy.weight_range[0] <= gene.value <= policy.weight_range[1])


class DecodingTests(unittest.TestCase):
    def test_empty_genome_is_the_static_seed(self) -> None:
        graph = decode((), Grammar())
        self.assertEqual({"u0", "y0"}, set(graph.nodes))
        self.assertEqual([("u0", "y0")], [(e.source, e.target) for e in graph.edges.values()])

    def test_decoding_is_deterministic(self) -> None:
        rng = Random(5)
        grammar = Grammar(2, 2)
        for _ in range(50):
            genome = random_genome(rng, grammar)
            self.assertEqual(graph_dumps(decode(genome, grammar)), graph_dumps(decode(genome, grammar)))

    def test_feedback_through_a_delay_reproduces_the_recursion(self) -> None:
        graph = decode(first_order_genome(), Grammar())
        controls = list(training_controls(60, Random(2)))
        modelled = SignalSimulator().run_series(graph, [[u] for u in controls])
        state = 0.0
        for u, (got,) in zip(controls, modelled):
            state = 0.5 * state + 0.5 * u
            self.assertAlmostEqual(state, got, places=12)

    def test_forward_reference_without_a_delay_is_not_expressed(self) -> None:
        genome = (
            node_gene("add"), input_gene(0), source_gene(+1),
            node_gene("tanh"), source_gene(-1),
            output_gene(0), source_gene(-1),
        )
        graph = decode(genome, Grammar())
        self.assertEqual(["u0"], [edge.source for edge in graph.incoming("n0")])

    def test_unary_node_keeps_only_its_first_source(self) -> None:
        genome = (node_gene("tanh"), input_gene(1), input_gene(0), output_gene(0), source_gene(-1))
        graph = decode(genome, Grammar(inputs=2))
        self.assertEqual(["u1"], [edge.source for edge in graph.incoming("n0")])

    def test_node_without_sources_is_dead_and_so_are_its_readers(self) -> None:
        genome = (node_gene("sqrt"), node_gene("tanh"), source_gene(-1), output_gene(0), source_gene(-1))
        graph = decode(genome, Grammar())
        self.assertEqual({"u0", "y0"}, set(graph.nodes))

    def test_any_integer_sequence_decodes_to_a_valid_graph(self) -> None:
        """The decoder is total: validity never needs checking downstream."""
        rng = Random(11)
        ids = sorted(GENES) + [0, -3, 999, 10_000]
        policy = SignalGraphPolicy()
        for trial in range(1500):
            grammar = Grammar(rng.randint(1, 3), rng.randint(1, 3))
            if trial % 2:
                genome = tuple(rng.choice(ids) for _ in range(rng.randint(0, 80)))
            else:
                genome = random_genome(rng, grammar, (0, 10))
                for _ in range(rng.randint(0, 20)):
                    genome = mutate(genome, rng, grammar)
            graph = decode(genome, grammar)
            policy.validate(graph)
            try:
                SignalSimulator().run_series(graph, [[0.3] * grammar.inputs] * 4)
            except SignalSimulationError:
                pass  # a domain error such as log(-1) is a score, not invalidity

    def test_restricted_kinds_are_not_expressed(self) -> None:
        grammar = Grammar(kinds=("add",))
        genome = (node_gene("delay"), input_gene(0), output_gene(0), source_gene(-1))
        self.assertEqual({"u0", "y0"}, set(decode(genome, grammar).nodes))


class VariationTests(unittest.TestCase):
    def test_context_offers_only_parameters_the_kind_owns(self) -> None:
        genome = (node_gene("integral"), input_gene(0))
        allowed = set(allowed_at(genome, 2, Grammar()))
        self.assertTrue(set(PARAMETER_LADDERS["gain"]) <= allowed)
        self.assertFalse(set(PARAMETER_LADDERS["alpha"]) & allowed)
        self.assertFalse(set(PARAMETER_LADDERS["steps"]) & allowed)

    def test_context_offers_forward_sources_only_through_a_delay(self) -> None:
        genome = (node_gene("add"), input_gene(0), node_gene("tanh"), source_gene(-1), node_gene("delay"))
        allowed = set(allowed_at(genome, 2, Grammar()))
        self.assertNotIn(source_gene(+1), allowed)  # add -> tanh: combinational
        self.assertIn(source_gene(+2), allowed)  # add <- delay: a loop breaker

    def test_unary_record_stops_offering_sources_when_full(self) -> None:
        genome = (node_gene("tanh"), input_gene(0))
        allowed = set(allowed_at(genome, 2, Grammar()))
        self.assertNotIn(input_gene(0), allowed)

    def test_every_mutation_and_crossover_stays_decodable(self) -> None:
        rng = Random(3)
        grammar = Grammar()
        policy = SignalGraphPolicy()
        for name, operator in MUTATIONS.items():
            for _ in range(100):
                genome = random_genome(rng, grammar)
                child = operator(genome, rng, grammar)
                if child is not None:
                    policy.validate(decode(child, grammar))
        for _ in range(100):
            child = splice(random_genome(rng, grammar), random_genome(rng, grammar), rng)
            policy.validate(decode(child, grammar))

    def test_parameter_step_moves_to_a_neighbouring_value(self) -> None:
        genome = (node_gene("filter_lp"), parameter_gene("alpha", 0.5), input_gene(0))
        child = MUTATIONS["step_parameter"](genome, Random(1), Grammar())
        self.assertIn(child[1], {parameter_gene("alpha", 0.25), parameter_gene("alpha", 0.75)})


PINNED_SHUFFLE = "d6ca3f2adf66ce90"


def fingerprint(codebook) -> str:
    return hashlib.sha256(repr(sorted(codebook.to_gene.items())).encode()).hexdigest()[:16]


class CodebookTests(unittest.TestCase):
    def test_tables_are_permanent(self) -> None:
        """Pinned: a changed table would rewrite the meaning of saved genomes."""
        self.assertEqual("16e000fa4687a390", fingerprint(hashed()))
        self.assertEqual("1bac7a58007e88cd", fingerprint(dense()))

    def test_dense_has_no_gaps(self) -> None:
        book = dense()
        self.assertEqual(set(range(book.low, book.high + 1)), set(book.to_gene))
        self.assertEqual(1.0, book.density)

    def test_hashed_numbers_are_four_digit_and_unique(self) -> None:
        book = hashed()
        self.assertEqual(len(CORE_GENES), len(book.to_gene))
        self.assertTrue(all(1000 <= number <= 9999 for number in book.to_gene))

    def test_translation_round_trips_and_unknown_numbers_are_introns(self) -> None:
        genome = first_order_genome()
        for book in (ordered(), dense(), hashed()):
            self.assertEqual(genome, book.translate(book.encode(genome)))
        self.assertEqual((0,), dense().translate((999,)))

    def test_same_model_under_every_codebook(self) -> None:
        trajectory = linear.linear_trajectory(training_controls(40, Random(1)))
        evaluator = SignalEvaluator(identification_dataset(trajectory))
        genome = first_order_genome()
        scores = {
            GenomeFitness(evaluator, Grammar(), codebook=book).raw(book.encode(genome))
            for book in (ordered(), dense(), hashed())
        }
        self.assertEqual(1, len(scores))


class NumericVariationTests(unittest.TestCase):
    def test_creep_moves_one_number_by_a_small_step(self) -> None:
        rng = Random(2)
        book = dense()
        genome = book.encode(first_order_genome())
        for _ in range(200):
            child = NUMERIC_MUTATIONS["creep"](genome, rng, book)
            changed = [(a, b) for a, b in zip(genome, child) if a != b]
            self.assertLessEqual(len(changed), 1)
            for before, after in changed:
                self.assertTrue(book.low <= after <= book.high)

    def test_numeric_offspring_always_decode_validly(self) -> None:
        rng = Random(8)
        policy = SignalGraphPolicy()
        for book in (ordered(), dense(), hashed()):
            genome = book.encode(random_genome(rng, Grammar()))
            for _ in range(300):
                genome = numeric_mutate(genome, rng, book)
                if rng.random() < 0.2:
                    genome = one_point(genome, book.encode(random_genome(rng, Grammar())), rng)
                policy.validate(decode(book.translate(genome), Grammar()))


class FixedEncodingTests(unittest.TestCase):
    def test_shuffled_tables_are_permanent(self) -> None:
        """Pinned: a reshuffle would change what every saved genome means."""
        encoding = FixedEncoding(Grammar(), "shuffle")
        digest = hashlib.sha256(repr(sorted(encoding.tables.items())).encode()).hexdigest()[:16]
        self.assertEqual(PINNED_SHUFFLE, digest)

    def test_widened_loci_hold_every_symbol(self) -> None:
        for scheme in ("runs", "shuffle", "mixed"):
            encoding = FixedEncoding(Grammar(), scheme)
            for role, table in encoding.tables.items():
                self.assertEqual(999, len(table))
                if SCHEMES[scheme][role] != "fine":
                    self.assertEqual(set(range(encoding.symbols[role])), set(table))

    def test_runs_make_most_unit_steps_synonymous(self) -> None:
        encoding = FixedEncoding(Grammar(), "runs")
        table = encoding.tables["kind"]
        same = sum(1 for a, b in zip(table, table[1:]) if a == b)
        self.assertGreater(same / (len(table) - 1), 0.95)

    def test_fine_weights_step_by_a_small_amount(self) -> None:
        encoding = FixedEncoding(Grammar(), "mixed")
        low, high = GENES[encoding._weight(500)].value, GENES[encoding._weight(501)].value
        self.assertAlmostEqual(4.0 / 998, high - low, places=9)

    def test_every_scheme_decodes_validly_under_variation(self) -> None:
        rng = Random(12)
        policy = SignalGraphPolicy()
        for scheme in SCHEMES:
            encoding = FixedEncoding(Grammar(inputs=2), scheme)
            for _ in range(60):
                genome = encoding.random_genome(rng)
                for _ in range(20):
                    genome = encoding.mutate(genome, rng)
                genome = encoding.crossover(genome, encoding.random_genome(rng), rng)
                policy.validate(decode(encoding.translate(genome), Grammar(inputs=2)))

    def test_every_crossover_keeps_records_whole_and_decodes_validly(self) -> None:
        rng = Random(21)
        grammar = Grammar(inputs=2, outputs=2)
        policy = SignalGraphPolicy()
        encoding = FixedEncoding(grammar, "runs")
        for name in FixedEncoding.CROSSOVERS:
            operator = getattr(encoding, name)
            for _ in range(80):
                child = operator(encoding.random_genome(rng), encoding.random_genome(rng), rng)
                self.assertEqual(0, (len(child) - encoding.tail()) % 6)
                policy.validate(decode(encoding.translate(child), grammar))

    def test_two_point_can_grow_and_shrink(self) -> None:
        rng = Random(5)
        encoding = FixedEncoding(Grammar())
        left, right = encoding.random_genome(rng, (6, 6)), encoding.random_genome(rng, (6, 6))
        lengths = {len(encoding.two_point(left, right, rng)) for _ in range(200)}
        self.assertTrue(min(lengths) < len(left) < max(lengths))

    def test_mutation_never_changes_a_locus_role(self) -> None:
        rng = Random(4)
        encoding = FixedEncoding(Grammar(), "shuffle")
        genome = encoding.random_genome(rng, (5, 5))
        for _ in range(300):
            child = encoding.creep(genome, rng)
            self.assertEqual(len(genome), len(child))
            for position, (a, b) in enumerate(zip(genome, child)):
                if a != b:
                    self.assertLess(b, encoding.size(encoding.role_at(child, position)))


class ExoticEncodingTests(unittest.TestCase):
    def test_default_config_is_exactly_the_base_scheme(self) -> None:
        rng_a, rng_b = Random(3), Random(3)
        plain = FixedEncoding(Grammar(), "base")
        configured = FixedEncoding(Grammar(), config=FixedConfig())
        for _ in range(20):
            genome = plain.random_genome(rng_a)
            self.assertEqual(genome, configured.random_genome(rng_b))
            self.assertEqual(plain.translate(genome), configured.translate(genome))

    def test_codon_layout_reads_the_standard_genetic_code(self) -> None:
        encoding = FixedEncoding(Grammar(), config=FixedConfig(layouts={"kind": "codon"}))
        table = encoding.tables["kind"]
        self.assertEqual(64, len(table))
        # TAA, TAG and TGA are stop codons: on the kind locus they silence the node.
        for codon in ("TAA", "TAG", "TGA"):
            index = sum("TCAG".index(base) * 4 ** (2 - i) for i, base in enumerate(codon))
            self.assertEqual(SILENT, table[index])
        # Leucine has six codons, so they all read alike.
        leucine = [i for i, amino in enumerate(GENETIC_CODE) if amino == "L"]
        self.assertEqual(6, len(leucine))
        self.assertEqual(1, len({table[i] for i in leucine}))

    def test_silenced_node_keeps_its_place_but_is_not_expressed(self) -> None:
        grammar = Grammar()
        encoding = FixedEncoding(grammar, config=FixedConfig(layouts={"kind": "epigenetic"}))
        width = encoding.config.width
        tanh = grammar.kinds.index("tanh")
        # node 0: silenced; node 1: tanh of node 0; output reads node 1.
        genome = (width + 5, 0, 1, 13, 1, 13, tanh, 0, 1, 13, 1, 13, 1, 13)
        graph = decode(encoding.translate(genome), grammar)
        self.assertEqual({"u0", "y0"}, set(graph.nodes))

    def test_diploid_expresses_the_dominant_allele(self) -> None:
        encoding = FixedEncoding(Grammar(), config=FixedConfig(layouts={"weight": "diploid:32"}))
        table = encoding.tables["weight"]
        dominant_high, recessive_changed = table[3 * 32 + 30], table[20 * 32 + 30]
        self.assertEqual(dominant_high, recessive_changed)  # a hidden change below the dominant allele

    def test_majority_ignores_a_single_mutated_copy(self) -> None:
        encoding = FixedEncoding(Grammar(), config=FixedConfig(layouts={"kind": "majority:10"}))
        table = encoding.tables["kind"]
        self.assertEqual(table[777], table[797])  # 7,7,7 -> 7,9,7: the vote still says 7

    def test_tag_addressing_binds_the_nearest_tag(self) -> None:
        grammar = Grammar()
        encoding = FixedEncoding(grammar, config=FixedConfig(addressing="tag"))
        tanh, square = grammar.kinds.index("tanh"), grammar.kinds.index("square")
        inputs = grammar.inputs
        # Nodes tagged 3 and 20; the third, tagged 9, asks for tag 19 and binds
        # node 1. The output asks for tag 9, so it reads the third node.
        genome = (tanh, 0, 3, 0, 13, 0, 13,
                  tanh, 0, 20, 0, 13, 0, 13,
                  square, 0, 9, inputs + 19, 13, 0, 13,
                  inputs + 9, 13)
        graph = decode(encoding.translate(genome), grammar)
        self.assertEqual(["n1"], [edge.source for edge in graph.incoming("n2")])

    def test_every_mutation_and_crossover_on_exotic_configs_decodes_validly(self) -> None:
        rng = Random(9)
        grammar = Grammar(inputs=2, outputs=2)
        policy = SignalGraphPolicy()
        configs = [
            FixedConfig(layouts={"kind": "codon", "source": "diploid:32", "weight": "log"}, addressing="tag"),
            FixedConfig(layouts={"kind": "gradient:5", "source": "cumulative", "parameter": "difference"}),
            FixedConfig(layouts={"kind": "logistic:3.99", "source": "sin:37", "parameter": "collatz", "weight": "golden"},
                        sources=3, output_sources=2),
            FixedConfig(layouts={"kind": "epigenetic", "weight": "majority:6"}, addressing="absolute", reach=4),
        ]
        mutations = ("creep", "reset", "bitflip", "base", "insert_record", "remove_record",
                     "duplicate_record", "transpose", "invert", "burst")
        for config in configs:
            encoding = FixedEncoding(grammar, config=config)
            for _ in range(15):
                genome = encoding.random_genome(rng)
                for name in mutations:
                    genome = getattr(encoding, name)(genome, rng) or genome
                    policy.validate(decode(encoding.translate(genome), grammar))
                for name in FixedEncoding.CROSSOVERS:
                    child = getattr(encoding, name)(genome, encoding.random_genome(rng), rng)
                    policy.validate(decode(encoding.translate(child), grammar))

    def test_context_layouts_start_from_the_genome_they_were_drawn_as(self) -> None:
        """Rewriting for an integral or derivative keeps fresh genomes as drawn."""
        grammar = Grammar()
        plain = FixedEncoding(grammar, config=FixedConfig(layouts="runs"))
        for layout in ("cumulative", "difference", "gradient:7"):
            layouts = {"kind": layout, "source": layout, "parameter": "runs", "weight": "runs"}
            context = FixedEncoding(grammar, config=FixedConfig(layouts=layouts))
            for seed in range(5):
                self.assertEqual(
                    plain.translate(plain.random_genome(Random(seed))),
                    context.translate(context.random_genome(Random(seed))),
                    layout,
                )

    def test_unknown_layouts_and_mutations_are_rejected(self) -> None:
        for bad in ({"layouts": {"kind": "fine"}}, {"layouts": {"weight": "epigenetic"}},
                    {"layouts": {"kind": "xor"}}, {"mutation": {"teleport": 1.0}}, {"sources": 4}):
            with self.assertRaises(ValueError, msg=str(bad)):
                FixedConfig(**bad)

    def test_the_weekend_catalog_builds_and_decodes(self) -> None:
        import importlib.util
        from pathlib import Path

        path = Path(__file__).resolve().parent.parent / "scripts" / "make_encoding_catalog.py"
        spec = importlib.util.spec_from_file_location("catalog", path)
        catalog = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(catalog)
        self.assertGreaterEqual(len(catalog.CATALOG), 100)
        catalog.validate()


class GeneticAlgorithmTests(unittest.TestCase):
    def setUp(self) -> None:
        trajectory = linear.linear_trajectory(training_controls(80, Random(7)))
        self.evaluator = SignalEvaluator(identification_dataset(trajectory))
        self.grammar = Grammar()

    def _run(self, seed: int):
        fitness = GenomeFitness(self.evaluator, self.grammar)
        rng = Random(seed)
        population = [random_genome(rng, self.grammar) for _ in range(20)]

        def breed(first, second, rng):
            child = splice(first, second, rng) if second is not None else first
            return mutate(child, rng, self.grammar)

        engine = GeneticAlgorithm(breed, fitness, GAConfig(population_size=20), seed)
        return engine.run(population, 30), fitness

    def test_search_improves_on_the_static_seed(self) -> None:
        result, fitness = self._run(1)
        self.assertLess(result.score, fitness.raw(()))
        self.assertEqual(sorted(result.history, reverse=True), result.history)

    def test_same_seed_same_run(self) -> None:
        first, _ = self._run(4)
        second, _ = self._run(4)
        self.assertEqual(first.best, second.best)
        self.assertEqual(first.history, second.history)

    def test_scores_are_cached_by_phenotype(self) -> None:
        fitness = GenomeFitness(self.evaluator, self.grammar)
        genome = (node_gene("tanh"), input_gene(0), output_gene(0), source_gene(-1))
        intron = genome + (node_gene("sqrt"),)
        fitness.raw_many([genome, intron, genome])
        self.assertEqual(1, fitness.simulations)


if __name__ == "__main__":
    unittest.main()
