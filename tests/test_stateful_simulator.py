import unittest

from ameba_graph import Edge, Graph, GraphError, Node
from ameba_signal import Dataset, SignalEvaluator, SignalGraphPolicy, SignalSimulator


def unary_graph(kind: str, attributes: dict[str, object] | None = None) -> Graph:
    return Graph(
        nodes=[
            Node("x", "input", {"index": 0}),
            Node("state", kind, attributes or {}),
            Node("y", "output", {"index": 0}),
        ],
        edges=[Edge("in", "x", "state"), Edge("out", "state", "y")],
    )


class StatefulSimulatorTests(unittest.TestCase):
    def test_delay_has_isolated_session_state_and_reset(self) -> None:
        graph = unary_graph("delay", {"steps": 1, "initial": 0.0})
        simulator = SignalSimulator()
        session = simulator.start(graph)
        self.assertEqual([0.0], session.step([1.0]))
        self.assertEqual([1.0], session.step([2.0]))
        self.assertEqual([2.0], session.step([3.0]))
        session.reset()
        self.assertEqual([0.0], session.step([9.0]))
        self.assertEqual([0.0], simulator.start(graph).step([5.0]))

    def test_integral_derivative_and_filters_evolve_across_steps(self) -> None:
        simulator = SignalSimulator()
        integral = simulator.start(unary_graph("integral", {"gain": 0.5}))
        self.assertEqual([[1.0], [2.0], [3.0]], [integral.step([2.0]) for _ in range(3)])

        derivative = simulator.start(unary_graph("derivative", {"time_step": 0.5}))
        self.assertEqual([2.0], derivative.step([1.0]))
        self.assertEqual([4.0], derivative.step([3.0]))

        low_pass = simulator.start(unary_graph("filter_lp", {"alpha": 0.5}))
        self.assertEqual([5.0], low_pass.step([10.0]))
        self.assertEqual([7.5], low_pass.step([10.0]))

        high_pass = simulator.start(unary_graph("filter_hp", {"alpha": 0.5}))
        self.assertEqual([5.0], high_pass.step([10.0]))
        self.assertEqual([2.5], high_pass.step([10.0]))

    def test_feedback_is_valid_only_when_broken_by_delay(self) -> None:
        graph = Graph(
            nodes=[
                Node("x", "input", {"index": 0}),
                Node("memory", "delay", {"steps": 1, "initial": 0.0}),
                Node("sum", "add"),
                Node("y", "output", {"index": 0}),
            ],
            edges=[
                Edge("x_sum", "x", "sum"),
                Edge("memory_sum", "memory", "sum"),
                Edge("sum_memory", "sum", "memory"),
                Edge("sum_y", "sum", "y"),
            ],
        )
        SignalGraphPolicy().validate(graph)
        session = SignalSimulator().start(graph)
        self.assertEqual([[1.0], [2.0], [3.0]], [session.step([1.0]) for _ in range(3)])

    def test_dataset_evaluation_uses_one_session_for_all_rows(self) -> None:
        graph = unary_graph("delay", {"steps": 1, "initial": 0.0})
        dataset = Dataset(
            inputs=((1.0,), (2.0,), (3.0,)),
            outputs=((0.0,), (1.0,), (2.0,)),
        )
        self.assertEqual(0.0, SignalEvaluator(dataset).evaluate(graph))

    def test_policy_rejects_invalid_state_parameters(self) -> None:
        with self.assertRaisesRegex(GraphError, "steps"):
            SignalGraphPolicy().validate(unary_graph("delay", {"steps": 0}))
        with self.assertRaisesRegex(GraphError, "time_step"):
            SignalGraphPolicy().validate(unary_graph("derivative", {"time_step": 0.0}))


if __name__ == "__main__":
    unittest.main()
