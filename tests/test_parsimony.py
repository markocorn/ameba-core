"""Size pressure: the parsimony penalty and dead-structure pruning."""

import math
import unittest

from ameba_graph import Edge, Graph, Node, ParsimoniousEvaluator, live_nodes, prune
from ameba_signal import SignalSimulator


class ConstantEvaluator:
    def __init__(self, score: float) -> None:
        self.score = score

    def evaluate(self, graph: Graph) -> float:
        return self.score


def chain(length: int) -> Graph:
    graph = Graph(nodes=[Node("u", "input", {"index": 0})])
    previous = "u"
    for index in range(length):
        name = f"n{index}"
        graph.add_node(Node(name, "add"))
        graph.add_edge(Edge(f"e_{previous}_{name}", previous, name))
        previous = name
    graph.add_node(Node("y", "output", {"index": 0}))
    graph.add_edge(Edge("e_out", previous, "y"))
    return graph


class ParsimoniousEvaluatorTests(unittest.TestCase):
    def test_a_zero_weight_leaves_the_score_untouched(self) -> None:
        evaluator = ParsimoniousEvaluator(ConstantEvaluator(3.5))
        self.assertEqual(3.5, evaluator.evaluate(chain(4)))

    def test_an_absolute_penalty_is_proportional_to_size(self) -> None:
        evaluator = ParsimoniousEvaluator(
            ConstantEvaluator(0.0), node_weight=0.1, relative=False
        )
        small, large = chain(2), chain(7)
        self.assertAlmostEqual(0.1 * len(small.nodes), evaluator.evaluate(small), places=12)
        self.assertAlmostEqual(0.1 * len(large.nodes), evaluator.evaluate(large), places=12)
        self.assertLess(evaluator.evaluate(small), evaluator.evaluate(large))

    def test_a_relative_penalty_scales_with_the_score(self) -> None:
        """The weight is a fraction of accuracy, so it tracks the fitness scale."""
        graph = chain(5)
        for score in (1.0, 40.0):
            evaluator = ParsimoniousEvaluator(ConstantEvaluator(score), node_weight=0.01)
            expected = score + 0.01 * len(graph.nodes) * score
            self.assertAlmostEqual(expected, evaluator.evaluate(graph), places=12)

    def test_a_relative_penalty_vanishes_at_a_perfect_score(self) -> None:
        """Nothing is left to trade away, so size stops mattering."""
        evaluator = ParsimoniousEvaluator(ConstantEvaluator(0.0), node_weight=0.5)
        self.assertEqual(0.0, evaluator.evaluate(chain(9)))

    def test_a_relative_penalty_still_breaks_ties_by_size(self) -> None:
        """Bloat earns nothing, so even a minimal weight is enough to reject it."""
        evaluator = ParsimoniousEvaluator(ConstantEvaluator(1.5), node_weight=1e-9)
        self.assertLess(evaluator.evaluate(chain(2)), evaluator.evaluate(chain(7)))

    def test_relative_is_the_default(self) -> None:
        """An absolute weight has to be guessed against an unknown fitness scale."""
        self.assertTrue(ParsimoniousEvaluator(ConstantEvaluator(1.0)).relative)

    def test_edges_can_be_penalised_too(self) -> None:
        graph = chain(3)
        evaluator = ParsimoniousEvaluator(
            ConstantEvaluator(1.0), node_weight=0.1, edge_weight=0.01, relative=False
        )
        expected = 1.0 + 0.1 * len(graph.nodes) + 0.01 * len(graph.edges)
        self.assertAlmostEqual(expected, evaluator.evaluate(graph), places=12)

    def test_a_rejected_candidate_stays_rejected(self) -> None:
        """Ranking infinities by size would be meaningless."""
        evaluator = ParsimoniousEvaluator(
            ConstantEvaluator(math.inf), node_weight=1.0, relative=False
        )
        self.assertEqual(math.inf, evaluator.evaluate(chain(3)))

    def test_a_smaller_graph_wins_only_when_accuracy_is_close(self) -> None:
        """The weight is an exchange rate, not an override."""
        small, large = chain(2), chain(6)
        difference = 0.1 * (len(large.nodes) - len(small.nodes))

        class Accuracy:
            def evaluate(self, graph: Graph) -> float:
                # The larger graph is genuinely more accurate here.
                return 0.0 if len(graph.nodes) == len(large.nodes) else difference * 2

        evaluator = ParsimoniousEvaluator(Accuracy(), node_weight=0.1, relative=False)
        self.assertLess(evaluator.evaluate(large), evaluator.evaluate(small))

    def test_negative_weights_are_rejected(self) -> None:
        """A negative weight would reward bloat."""
        for kwargs in ({"node_weight": -0.1}, {"edge_weight": -0.1}):
            with self.assertRaises(ValueError):
                ParsimoniousEvaluator(ConstantEvaluator(1.0), **kwargs)


class PruningTests(unittest.TestCase):
    def bloated(self) -> Graph:
        """A working path plus a branch that never reaches the output."""
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("live", "add"),
                Node("dead_a", "add"),
                Node("dead_b", "multiply"),
                Node("y", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "u", "live", {"weight": 2.0}),
                Edge("e2", "live", "y"),
                Edge("e3", "u", "dead_a", {"weight": 3.0}),
                Edge("e4", "dead_a", "dead_b"),
                Edge("e5", "u", "dead_b", {"weight": 5.0}),
            ],
        )
        return graph

    def test_live_nodes_are_those_reaching_a_terminal(self) -> None:
        graph = self.bloated()
        self.assertEqual({"u", "live", "y"}, live_nodes(graph, {"y"}))

    def test_pruning_removes_only_the_unreachable(self) -> None:
        pruned = prune(self.bloated(), {"y"})
        self.assertEqual({"u", "live", "y"}, set(pruned.nodes))
        self.assertEqual(2, len(pruned.edges))

    def test_pruning_does_not_change_what_the_graph_produces(self) -> None:
        graph = self.bloated()
        rows = [(1.0,), (-2.0,), (0.5,)]
        simulator = SignalSimulator()
        self.assertEqual(
            simulator.run_series(graph, rows),
            simulator.run_series(prune(graph, {"y"}), rows),
        )

    def test_pruning_leaves_the_original_alone(self) -> None:
        graph = self.bloated()
        before = (set(graph.nodes), set(graph.edges))
        prune(graph, {"y"})
        self.assertEqual(before, (set(graph.nodes), set(graph.edges)))

    def test_a_graph_with_nothing_dead_is_returned_intact(self) -> None:
        graph = chain(4)
        pruned = prune(graph, {"y"})
        self.assertEqual(set(graph.nodes), set(pruned.nodes))
        self.assertEqual(set(graph.edges), set(pruned.edges))

    def test_pruning_keeps_a_delay_loop_that_feeds_the_output(self) -> None:
        """State reaches the output through the loop, so none of it is dead."""
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("sum", "add"),
                Node("d", "delay", {"steps": 1, "initial": 0.0}),
                Node("y", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "u", "sum"),
                Edge("e2", "d", "sum", {"weight": 0.5}),
                Edge("e3", "sum", "d"),
                Edge("e4", "sum", "y"),
            ],
        )
        self.assertEqual(set(graph.nodes), set(prune(graph, {"y"}).nodes))


if __name__ == "__main__":
    unittest.main()
