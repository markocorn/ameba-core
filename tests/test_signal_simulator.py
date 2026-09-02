import unittest

from ameba.cli import example_graph
from ameba_graph import Edge, Graph, Node
from ameba_signal import Dataset, SignalEvaluator, SignalSimulationError, SignalSimulator


class SignalSimulatorTests(unittest.TestCase):
    def test_executes_graph(self) -> None:
        self.assertEqual([7.0], SignalSimulator().run(example_graph(), [3.0]))

    def test_evaluator_implements_graph_boundary(self) -> None:
        dataset = Dataset(
            inputs=((0.0,), (1.0,), (2.0,)),
            outputs=((1.0,), (3.0,), (5.0,)),
        )
        self.assertEqual(0.0, SignalEvaluator(dataset).evaluate(example_graph()))

    def test_evaluator_can_normalize_scores_for_comparable_islands(self) -> None:
        dataset = Dataset(inputs=((1.0,),), outputs=((0.0,),))
        raw = SignalEvaluator(dataset).evaluate(example_graph())
        self.assertEqual(
            raw / 2.0,
            SignalEvaluator(dataset, normalization=2.0).evaluate(example_graph()),
        )
        with self.assertRaises(ValueError):
            SignalEvaluator(dataset, normalization=0.0)

    def test_rejects_cycle_until_stateful_runtime_exists(self) -> None:
        graph = Graph(
            nodes=[Node("a", "add"), Node("b", "output")],
            edges=[Edge("ab", "a", "b"), Edge("ba", "b", "a")],
        )
        with self.assertRaisesRegex(SignalSimulationError, "algebraic cycle"):
            SignalSimulator().run(graph, [])


if __name__ == "__main__":
    unittest.main()
