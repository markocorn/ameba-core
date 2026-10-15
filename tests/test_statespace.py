import unittest
from random import Random

from ameba.benchmarks import identification_dataset, narendra, static_gain_floor, training_controls
from ameba_genome import Grammar
from ameba_genome.statespace import OUTPUT_REACH, STATE_SLOTS, WIDTH, StateSpaceConfig, StateSpaceEncoding
from ameba_signal import SignalEvaluator, SignalGraphPolicy, SignalSimulationError, SignalSimulator
from ameba_signal.stateful import STATEFUL_KINDS

GRAMMAR = Grammar()
KIND = {kind: index for index, kind in enumerate(GRAMMAR.kinds)}
W1 = 13  # weight 1.0
NONE = 1 + STATE_SLOTS - 1  # a state slot no genome below has: reads nothing
U, Y0 = 0, 1 + STATE_SLOTS


def state(s):
    return 1 + s


def back(k):
    return 1 + STATE_SLOTS + k - 1


def record(kind, a, b=NONE, parameter=0):
    return [KIND[kind], parameter, a, W1, b, W1]


def narendra_genome():
    """The exact Narendra plant in state-space form.

    States hold y(k), y(k-1), y(k-2) and u(k-1); the output record chain is
    the plant's rational map of them and u(k).
    """
    states = [record("add", Y0), record("add", state(0)), record("add", state(1)), record("add", U)]
    outputs = [
        record("multiply", state(0), state(1)),
        record("multiply", back(1), state(2)),
        record("multiply", back(1), state(3)),
        record("constant", NONE, parameter=1),  # -1
        record("add", state(2), back(1)),
        record("multiply", back(3), back(1)),
        record("add", back(1), U),
        record("square", state(1)),
        record("square", state(2)),
        record("constant", NONE, parameter=5),  # +1
        record("add", back(3), back(2)),
        record("add", back(1), back(2)),
        record("reciprocal", back(1)),
        record("multiply", back(7), back(1)),
    ]
    return tuple([len(states)] + sum(states, []) + sum(outputs, []) + [back(1), W1])


class StateSpaceTests(unittest.TestCase):
    def setUp(self) -> None:
        self.encoding = StateSpaceEncoding(GRAMMAR)

    def test_exact_narendra_model_is_expressible(self) -> None:
        trajectory = narendra.narendra_trajectory(training_controls(150, Random(7)))
        graph = self.encoding.build(narendra_genome())
        SignalGraphPolicy().validate(graph)
        score = SignalEvaluator(identification_dataset(trajectory)).evaluate(graph)
        self.assertLess(score / static_gain_floor(trajectory), 1e-20)

    def test_output_feedback_is_one_source(self) -> None:
        """A state reading the model output closes a loop through its delay."""
        genome = (1, *record("tanh", Y0), *record("add", state(0), U), back(1), W1)
        graph = self.encoding.build(genome)
        SignalGraphPolicy().validate(graph)
        self.assertEqual(["h0"], [edge.source for edge in graph.incoming("p0")])
        self.assertIn("x0", [edge.source for edge in graph.incoming("h0")])

    def test_states_may_read_themselves(self) -> None:
        genome = (1, *record("add", state(0), U), state(0), W1)
        graph = self.encoding.build(genome)
        SignalGraphPolicy().validate(graph)
        modelled = SignalSimulator().run_series(graph, [[1.0]] * 4)
        self.assertEqual([[0.0], [1.0], [2.0], [3.0]], modelled)  # an accumulator: x(k+1) = x(k) + u(k)

    def test_any_genome_builds_a_valid_graph(self) -> None:
        rng = Random(3)
        policy = SignalGraphPolicy()
        for grammar in (Grammar(), Grammar(inputs=2, outputs=2)):
            encoding = StateSpaceEncoding(grammar)
            for _ in range(400):
                genome = encoding.random_genome(rng)
                for _ in range(rng.randint(0, 25)):
                    genome = encoding.mutate(genome, rng)
                    if rng.random() < 0.2:
                        genome = getattr(encoding, rng.choice(("crossover", "organelle")))(
                            genome, encoding.random_genome(rng), rng)
                graph = encoding.build(genome)
                policy.validate(graph)
                try:
                    SignalSimulator().run_series(graph, [[0.3] * grammar.inputs] * 4)
                except SignalSimulationError:
                    pass  # a domain error such as log(-1) is a score, not invalidity

    def test_variation_keeps_the_header_and_record_shape(self) -> None:
        rng = Random(5)
        genome = self.encoding.random_genome(rng)
        for _ in range(300):
            genome = self.encoding.mutate(genome, rng)
            states, outputs = self.encoding.split(genome)
            self.assertEqual(len(genome), 1 + WIDTH * (states + outputs) + self.encoding.tail())
            self.assertGreaterEqual(outputs, 0)
            self.assertLessEqual(states, STATE_SLOTS)

    def test_point_mutations_never_touch_the_header(self) -> None:
        rng = Random(6)
        genome = self.encoding.random_genome(rng)
        for _ in range(300):
            child = self.encoding.creep(genome, rng)
            self.assertEqual(genome[0], child[0])

    def test_organelle_takes_states_from_one_parent_and_output_from_the_other(self) -> None:
        rng = Random(7)
        left, right = self.encoding.random_genome(rng), self.encoding.random_genome(rng)
        child = self.encoding.organelle(left, right, rng)
        ls, lo, lt = self.encoding._chromosomes(left)
        rs, ro, _ = self.encoding._chromosomes(right)
        cs, co, ct = self.encoding._chromosomes(child)
        self.assertEqual((rs, lo, lt), (cs, co, ct))

    def test_build_is_deterministic(self) -> None:
        rng = Random(8)
        for _ in range(30):
            genome = self.encoding.random_genome(rng)
            first, second = self.encoding.build(genome), self.encoding.build(genome)
            self.assertEqual(
                [(e.source, e.target, e.attributes["weight"]) for e in first.edges.values()],
                [(e.source, e.target, e.attributes["weight"]) for e in second.edges.values()],
            )

    def test_output_records_reach_only_backwards(self) -> None:
        """An output record naming a record ahead of it reads nothing."""
        self.assertEqual(8, OUTPUT_REACH)
        genome = (0, *record("tanh", back(1)), back(1), W1)  # the first record has nothing behind it
        graph = self.encoding.build(genome)
        self.assertEqual({"u0", "y0"}, set(graph.nodes))


class StateSpaceConfigTests(unittest.TestCase):
    STRICT = StateSpaceConfig(state_kinds="memory3", output_kinds="static", output_sources=2)

    def _fuzz(self, config, check):
        rng = Random(2)
        policy = SignalGraphPolicy()
        for grammar in (Grammar(), Grammar(inputs=2, outputs=2)):
            encoding = StateSpaceEncoding(grammar, config=config)
            for _ in range(200):
                genome = encoding.random_genome(rng)
                for _ in range(rng.randint(0, 20)):
                    genome = encoding.mutate(genome, rng)
                    if rng.random() < 0.2:
                        genome = encoding.crossover(genome, encoding.random_genome(rng), rng)
                graph = encoding.build(genome)
                policy.validate(graph)
                check(graph)

    def test_static_output_chromosome_holds_no_memory(self) -> None:
        def check(graph):
            for node in graph.nodes.values():
                if node.id.startswith("h"):
                    self.assertNotIn(node.kind, STATEFUL_KINDS)
        self._fuzz(StateSpaceConfig(output_kinds="static"), check)

    def test_memory3_states_have_no_filters(self) -> None:
        def check(graph):
            self.assertFalse({"filter_lp", "filter_hp"} & {node.kind for node in graph.nodes.values()})
        self._fuzz(self.STRICT, check)

    def test_summing_outputs_read_a_sum_node(self) -> None:
        def check(graph):
            for channel in (0, 1):
                if f"y{channel}" in graph.nodes:
                    (edge,) = graph.incoming(f"y{channel}")
                    if edge.source.startswith("s"):
                        self.assertEqual("add", graph.nodes[edge.source].kind)
        self._fuzz(self.STRICT, check)

    def test_exact_narendra_model_under_the_strictest_config(self) -> None:
        encoding = StateSpaceEncoding(GRAMMAR, config=StateSpaceConfig(state_kinds="memory3", output_kinds="static"))
        ks = {kind: i for i, kind in enumerate(encoding.kinds["state"])}
        ko = {kind: i for i, kind in enumerate(encoding.kinds["output"])}
        genome = list(narendra_genome())
        states = genome[0]
        for index in range(states + (len(genome) - 3) // WIDTH - states):
            table = ks if index < states else ko
            position = 1 + index * WIDTH
            genome[position] = table[GRAMMAR.kinds[genome[position]]]
        trajectory = narendra.narendra_trajectory(training_controls(150, Random(7)))
        score = SignalEvaluator(identification_dataset(trajectory)).evaluate(encoding.build(tuple(genome)))
        self.assertLess(score / static_gain_floor(trajectory), 1e-20)

    def test_unknown_kind_sets_are_rejected(self) -> None:
        with self.assertRaises(ValueError):
            StateSpaceEncoding(GRAMMAR, config=StateSpaceConfig(output_kinds=("teleport",)))
        with self.assertRaises(ValueError):
            StateSpaceConfig(output_sources=3)


class InheritanceTests(unittest.TestCase):
    """The two chromosomes as a cell's two genomes: nucleus and mitochondrion."""

    def setUp(self) -> None:
        self.maternal = StateSpaceEncoding(GRAMMAR, config=StateSpaceConfig(inheritance="maternal"))
        rng = Random(5)
        self.parents = [self.maternal.random_genome(rng) for _ in range(20)]

    def test_maternal_children_carry_the_mothers_states_whole(self) -> None:
        rng = Random(1)
        for mother, father in zip(self.parents, self.parents[1:]):
            child = self.maternal.crossover(mother, father, rng)
            self.assertEqual(self.maternal._chromosomes(mother)[0], self.maternal._chromosomes(child)[0])

    def test_maternal_children_still_recombine_the_nucleus(self) -> None:
        rng = Random(2)
        mixed = 0
        for mother, father in zip(self.parents, self.parents[1:]):
            child = self.maternal.crossover(mother, father, rng)
            output = self.maternal._chromosomes(child)[1]
            mixed += any(r not in self.maternal._chromosomes(mother)[1] for r in output)
        self.assertGreater(mixed, 0)

    def test_a_faster_mitochondrion_mutates_its_states_more_often(self) -> None:
        def state_share(rate):
            encoding = StateSpaceEncoding(GRAMMAR, config=StateSpaceConfig(state_rate=rate))
            genome = self.parents[0]
            rng = Random(3)
            positions = [encoding._locus(genome, rng) for _ in range(4000)]
            return sum(p <= WIDTH * genome[0] for p in positions) / len(positions)

        self.assertGreater(state_share(4.0), state_share(1.0) + 0.1)
        self.assertLess(state_share(0.25), state_share(1.0) - 0.05)

    def test_point_mutations_keep_the_header_at_any_rate(self) -> None:
        encoding = StateSpaceEncoding(GRAMMAR, config=StateSpaceConfig(state_rate=5.0))
        rng = Random(4)
        for genome in self.parents:
            for _ in range(20):
                child = encoding.creep(genome, rng)
                self.assertEqual(genome[0], child[0])

    def test_bad_inheritance_settings_are_rejected(self) -> None:
        with self.assertRaises(ValueError):
            StateSpaceConfig(inheritance="paternal")
        with self.assertRaises(ValueError):
            StateSpaceConfig(state_rate=0.0)


class DnaMechanismTests(unittest.TestCase):
    """Duplication, synonymous codes and repeat hotspots."""

    def test_synonyms_build_the_same_graph_as_their_meaning(self) -> None:
        from ameba_graph.serialization import graph_dumps

        plain = StateSpaceEncoding(GRAMMAR)
        redundant = StateSpaceEncoding(GRAMMAR, config=StateSpaceConfig(redundancy=3))
        rng = Random(6)
        for _ in range(30):
            genome = plain.random_genome(rng)
            coded = genome[:1] + tuple(3 * v + rng.randrange(3) for v in genome[1:])
            self.assertEqual(graph_dumps(plain.build(genome)), graph_dumps(redundant.build(coded)))

    def test_redundant_genomes_stay_valid_under_variation(self) -> None:
        encoding = StateSpaceEncoding(GRAMMAR, config=StateSpaceConfig(
            redundancy=4, source_layout="inputs-last", duplication=0.2, hotspot=2.0))
        rng = Random(7)
        for _ in range(30):
            genome = encoding.random_genome(rng)
            for _ in range(30):
                genome = encoding.mutate(genome, rng)
                for position in range(1, len(genome)):
                    self.assertLess(genome[position], encoding.size_at(genome, position))
                encoding.build(genome)

    def test_duplication_copies_a_record_next_to_itself(self) -> None:
        encoding = StateSpaceEncoding(GRAMMAR, config=StateSpaceConfig(duplication=1.0))
        rng = Random(8)
        genome = encoding.random_genome(rng)
        child = encoding.duplicate_record(genome, rng)
        before = encoding._chromosomes(genome)
        after = encoding._chromosomes(child)
        grown = 0 if len(after[0]) > len(before[0]) else 1
        records = after[grown]
        self.assertTrue(any(records[i] == records[i + 1] for i in range(len(records) - 1)))
        self.assertEqual(len(before[grown]) + 1, len(records))

    def test_repeats_mutate_more_often_with_a_hotspot(self) -> None:
        encoding = StateSpaceEncoding(GRAMMAR, config=StateSpaceConfig(hotspot=5.0))
        genome = (1,) + (3, 3, 3, 3, 0, 1) + (0, 1, 2, 4, 5, 6) + (7, 8)
        rng = Random(9)
        hits = [encoding._locus(genome, rng) for _ in range(3000)]
        repeat_share = sum(1 <= p <= 4 for p in hits) / len(hits)
        self.assertGreater(repeat_share, 0.6)  # 4 of 14 loci without a hotspot


class SourceResetTests(unittest.TestCase):
    """How ``reset`` draws a source when there are extra inputs."""

    LAST = dict(output_kinds="static", output_sources=2, source_layout="inputs-last")

    def test_pooled_is_uniform_exactly_with_one_input(self) -> None:
        for layout in ("inputs-first", "inputs-last"):
            uniform = StateSpaceEncoding(GRAMMAR, config=StateSpaceConfig(source_layout=layout))
            pooled = StateSpaceEncoding(GRAMMAR, config=StateSpaceConfig(source_layout=layout, source_reset="pooled"))
            a, b = Random(3), Random(3)
            genome = uniform.random_genome(Random(4))
            for _ in range(500):
                self.assertEqual(uniform.reset(genome, a), pooled.reset(genome, b))

    def test_extra_inputs_take_no_share_from_states_when_pooled(self) -> None:
        def state_share(inputs, mode):
            encoding = StateSpaceEncoding(Grammar(inputs, 1), config=StateSpaceConfig(**self.LAST, source_reset=mode))
            genome = encoding.random_genome(Random(5))
            source = next(p for p in range(1, len(genome)) if encoding.role_at(genome, p) == ("state", "source"))
            rng = Random(6)
            codes = [encoding._pooled_source(genome, source, rng) if mode == "pooled"
                     else rng.randrange(encoding.size_at(genome, source)) for _ in range(6000)]
            return sum(code < STATE_SLOTS for code in codes) / len(codes)

        self.assertAlmostEqual(state_share(1, "pooled"), state_share(7, "pooled"), delta=0.03)
        self.assertLess(state_share(7, "uniform"), state_share(1, "uniform") - 0.15)

    def test_pooled_reaches_every_input(self) -> None:
        encoding = StateSpaceEncoding(Grammar(4, 1), config=StateSpaceConfig(**self.LAST, source_reset="pooled"))
        genome = encoding.random_genome(Random(7))
        source = next(p for p in range(1, len(genome)) if encoding.role_at(genome, p) == ("output", "source"))
        rng = Random(8)
        codes = {encoding._pooled_source(genome, source, rng) for _ in range(4000)}
        span = encoding._span("output")
        self.assertEqual(set(range(encoding.size_at(genome, source))), codes)
        self.assertTrue({span, span + 1, span + 2, span + 3} <= codes)

    def test_near_steps_are_mostly_small_and_stay_inside(self) -> None:
        encoding = StateSpaceEncoding(Grammar(3, 1), config=StateSpaceConfig(**self.LAST, source_reset="near"))
        genome = encoding.random_genome(Random(9))
        source = next(p for p in range(1, len(genome)) if encoding.role_at(genome, p)[1] == "source")
        size = encoding.size_at(genome, source)
        rng = Random(10)
        values = [encoding._near(genome, source, rng) for _ in range(4000)]
        self.assertTrue(all(0 <= v < size for v in values))
        small = sum(abs(v - genome[source]) <= 2 for v in values) / len(values)
        self.assertGreater(small, 0.6)

    def test_every_reset_mode_keeps_genomes_valid(self) -> None:
        for mode in ("pooled", "near"):
            encoding = StateSpaceEncoding(Grammar(5, 1), config=StateSpaceConfig(
                **self.LAST, source_reset=mode, redundancy=2))
            rng = Random(11)
            for _ in range(20):
                genome = encoding.random_genome(rng)
                for _ in range(40):
                    genome = encoding.mutate(genome, rng)
                    for position in range(1, len(genome)):
                        self.assertLess(genome[position], encoding.size_at(genome, position))
                    encoding.build(genome)
        with self.assertRaises(ValueError):
            StateSpaceConfig(source_reset="far")


if __name__ == "__main__":
    unittest.main()
