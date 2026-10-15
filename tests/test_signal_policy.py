import unittest
from random import Random

from ameba.cli import example_graph
from ameba_graph import Edge, EvolutionConfig, EvolutionEngine, Graph, GraphError, Node
from ameba_graph.crossover import UniformGraphCrossover
from ameba_graph.mutation import SplitEdge
from ameba_signal import (
    COARSE_ALLELES,
    PARAMETER_ALPHABETS,
    Dataset,
    SignalEvaluator,
    SignalGraphPolicy,
)


class SignalPolicyTests(unittest.TestCase):
    def setUp(self) -> None:
        self.policy = SignalGraphPolicy(weight_range=(1.0, 1.0))

    def test_example_graph_satisfies_signal_policy(self) -> None:
        self.policy.validate(example_graph())

    def test_policy_rejects_cycle(self) -> None:
        graph = Graph(
            nodes=[
                Node("x", "input", {"index": 0}),
                Node("a", "add"),
                Node("b", "add"),
                Node("y", "output", {"index": 0}),
            ],
            edges=[
                Edge("xa", "x", "a"),
                Edge("ab", "a", "b"),
                Edge("ba", "b", "a"),
                Edge("by", "b", "y"),
            ],
        )
        with self.assertRaisesRegex(GraphError, "feedback loop"):
            self.policy.validate(graph)

    def test_split_edge_is_transactionally_valid(self) -> None:
        child = SplitEdge().mutate(example_graph(), self.policy, Random(4))
        self.policy.validate(child)
        self.assertEqual(len(example_graph().nodes) + 1, len(child.nodes))
        self.assertEqual(len(example_graph().edges) + 1, len(child.edges))

    def test_niche_constraints_reject_foreign_or_missing_structure(self) -> None:
        graph = example_graph()
        requires_memory = SignalGraphPolicy(
            required_kind_groups=({"delay", "integral"},)
        )
        with self.assertRaisesRegex(GraphError, "requires at least one"):
            requires_memory.validate(graph)

        forbids_multiply = SignalGraphPolicy(forbidden_kinds={"multiply"})
        with self.assertRaisesRegex(GraphError, "forbidden"):
            forbids_multiply.validate(graph)

    def test_generic_evolution_accepts_signal_domain_adapter(self) -> None:
        dataset = Dataset(
            inputs=((0.0,), (1.0,), (2.0,)),
            outputs=((1.0,), (3.0,), (5.0,)),
        )
        initial = [example_graph() for _ in range(4)]
        engine = EvolutionEngine(
            evaluator=SignalEvaluator(dataset),
            policy=self.policy,
            mutations=[SplitEdge()],
            crossover=UniformGraphCrossover(),
            config=EvolutionConfig(
                population_size=4,
                elite_size=1,
                tournament_size=2,
                mutation_rate=1.0,
                crossover_rate=0.5,
            ),
            seed=12,
        )
        result = engine.run(initial, generations=2)
        self.assertEqual(0.0, result.best.score)
        self.assertEqual(4, len(result.population))


class ParameterAlphabetTests(unittest.TestCase):
    """A finite alphabet turns a parameter into a symbol, not a continuum."""

    def test_continuous_values_are_the_default(self) -> None:
        policy = SignalGraphPolicy()
        self.assertEqual({}, policy.alleles)
        weights = {
            policy.create_edge("a", "b", Random(seed)).attributes["weight"]
            for seed in range(40)
        }
        self.assertGreater(len(weights), 10)

    def test_alphabet_is_sorted_and_deduplicated(self) -> None:
        policy = SignalGraphPolicy(alleles={"weight": (1.0, -1.0, 1.0, 0.5)})
        self.assertEqual((-1.0, 0.5, 1.0), policy.alleles["weight"])

    def test_unknown_empty_and_out_of_range_alphabets_are_rejected(self) -> None:
        with self.assertRaisesRegex(ValueError, "Unknown parameter alphabet"):
            SignalGraphPolicy(alleles={"wobble": (1.0,)})
        with self.assertRaisesRegex(ValueError, "cannot be empty"):
            SignalGraphPolicy(alleles={"weight": ()})
        with self.assertRaisesRegex(ValueError, "outside"):
            SignalGraphPolicy(alleles={"weight": (5.0,)})
        with self.assertRaisesRegex(ValueError, "outside"):
            SignalGraphPolicy(alleles={"alpha": (2.0,)})

    def test_an_integer_parameter_accepts_only_whole_alleles(self) -> None:
        with self.assertRaisesRegex(ValueError, "must hold integers"):
            SignalGraphPolicy(alleles={"steps": (1.5,)})
        policy = SignalGraphPolicy(alleles={"steps": (1, 4)})
        self.assertEqual((1, 4), policy.alleles["steps"])

    def test_creation_draws_only_from_the_alphabet(self) -> None:
        policy = SignalGraphPolicy(alleles={"weight": (-1.0, 1.0), "alpha": (0.25,)})
        for seed in range(60):
            rng = Random(seed)
            edge = policy.create_edge("a", "b", rng)
            node = policy.create_node(rng)
            self.assertIn(edge.attributes["weight"], (-1.0, 1.0))
            if "alpha" in node.attributes:
                self.assertEqual(0.25, node.attributes["alpha"])

    def test_mutation_substitutes_rather_than_nudges(self) -> None:
        policy = SignalGraphPolicy(alleles={"weight": (-1.0, 0.0, 1.0)})
        edge = Edge("e", "a", "b", {"weight": 0.0})
        for seed in range(20):
            mutated = policy.mutate_edge(edge, Random(seed))
            self.assertIn(mutated.attributes["weight"], (-1.0, 1.0))
        self.assertEqual(0.0, edge.attributes["weight"], "the parent was modified")

    def test_a_single_allele_offers_nothing_to_substitute(self) -> None:
        policy = SignalGraphPolicy(alleles={"weight": (1.0,)})
        edge = Edge("e", "a", "b", {"weight": 1.0})
        with self.assertRaisesRegex(ValueError, "no alternative"):
            policy.mutate_edge(edge, Random(1))

    def test_crossover_gives_the_child_one_parents_allele(self) -> None:
        policy = SignalGraphPolicy(alleles={"weight": (-1.0, 1.0)})
        left = Edge("e", "a", "b", {"weight": -1.0})
        right = Edge("e", "a", "b", {"weight": 1.0})
        seen = {
            policy.cross_edge(left, right, Random(seed)).attributes["weight"]
            for seed in range(30)
        }
        self.assertEqual({-1.0, 1.0}, seen)
        self.assertEqual(-1.0, left.attributes["weight"], "a parent was modified")

    def test_an_off_alphabet_value_still_validates(self) -> None:
        """The alphabet governs what search creates, not what a load may hold."""
        policy = SignalGraphPolicy(alleles={"weight": (-1.0, 1.0)})
        graph = example_graph()
        for edge in graph.edges.values():
            edge.attributes["weight"] = 0.37
        policy.validate(graph)

    def test_every_preset_stays_inside_its_parameters_limits(self) -> None:
        """An alphabet narrows a range; a preset that widened one would raise."""
        for name in sorted(PARAMETER_ALPHABETS):
            with self.subTest(alphabet=name):
                policy = SignalGraphPolicy(alleles=PARAMETER_ALPHABETS[name])
                self.assertEqual(
                    tuple(sorted(PARAMETER_ALPHABETS[name])),
                    tuple(sorted(policy.alleles)),
                )

    def test_a_coarse_run_can_only_produce_alphabet_values(self) -> None:
        policy = SignalGraphPolicy(alleles=COARSE_ALLELES)
        for seed in range(40):
            rng = Random(seed)
            node = policy.create_node(rng)
            edge = policy.create_edge("a", "b", rng)
            self.assertIn(edge.attributes["weight"], COARSE_ALLELES["weight"])
            for name, value in node.attributes.items():
                self.assertIn(value, COARSE_ALLELES[name])

    def test_a_policy_pickled_without_the_alphabet_still_works(self) -> None:
        """A worker may import newer code than the state it unpickles.

        A process-pool worker imports the module from disk, while its parent
        sends a policy pickled earlier. If the parent predates the alphabet,
        the unpickled instance carries no such key, so the field has to have a
        class-level fallback as well as being set in ``__init__``.
        """
        state = dict(SignalGraphPolicy().__dict__)
        del state["alleles"]
        restored = SignalGraphPolicy.__new__(SignalGraphPolicy)
        restored.__dict__.update(state)

        node = Node("n", "filter_lp", {"alpha": 0.5})
        self.assertIn("alpha", restored.mutate_node(node, Random(1)).attributes)
        edge = Edge("e", "a", "b", {"weight": 1.0})
        self.assertIn("weight", restored.mutate_edge(edge, Random(1)).attributes)


if __name__ == "__main__":
    unittest.main()
