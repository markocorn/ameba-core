import unittest
from random import Random

from ameba.cli import example_graph
from ameba_graph import Edge, EvolutionConfig, EvolutionEngine, Graph, GraphError, Node
from ameba_graph.crossover import UniformGraphCrossover
from ameba_graph.mutation import SplitEdge
from ameba_signal import Dataset, SignalEvaluator, SignalGraphPolicy


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


if __name__ == "__main__":
    unittest.main()
