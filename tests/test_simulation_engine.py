"""Verification of the signal simulator against known dynamic systems.

Every test here builds an AMEBA graph for a system whose response is known
independently -- in most cases from a closed-form solution rather than from a
second implementation -- and requires the simulated output to match.

The closed forms matter. Checking a simulator against another hand-written loop
only proves the two agree; checking it against `r^k sin((k+1)w)/sin(w)` proves
the simulator computes the system it claims to.
"""

import math
import unittest

from ameba_graph import Edge, Graph, Node
from ameba_signal import SignalSimulationError, SignalSimulator


def iir_graph(feedback: list[float], gain: float = 1.0) -> Graph:
    """Direct-form recursion y(k) = sum_i feedback[i]*y(k-1-i) + gain*u(k)."""
    graph = Graph()
    graph.add_node(Node("u", "input", {"index": 0}))
    graph.add_node(Node("y", "add"))
    graph.add_edge(Edge("e_u_y", "u", "y", {"weight": gain}))

    previous = "y"
    for order, coefficient in enumerate(feedback):
        name = f"d{order}"
        graph.add_node(Node(name, "delay", {"steps": 1, "initial": 0.0}))
        graph.add_edge(Edge(f"e_{previous}_{name}", previous, name))
        graph.add_edge(Edge(f"e_{name}_y", name, "y", {"weight": coefficient}))
        previous = name

    graph.add_node(Node("out", "output", {"index": 0}))
    graph.add_edge(Edge("e_y_out", "y", "out"))
    return graph


def impulse(count: int) -> list[tuple[float, ...]]:
    return [(1.0,)] + [(0.0,)] * (count - 1)


def step(count: int, level: float = 1.0) -> list[tuple[float, ...]]:
    return [(level,)] * count


def run(graph: Graph, rows: list[tuple[float, ...]]) -> list[float]:
    return [row[0] for row in SignalSimulator().run_series(graph, rows)]


class ClosedFormResponseTests(unittest.TestCase):
    """Systems whose exact response is known analytically."""

    def test_pure_gain_scales_the_input(self) -> None:
        graph = iir_graph([], gain=2.5)
        produced = run(graph, [(value,) for value in (1.0, -2.0, 0.5, 0.0)])
        self.assertEqual([2.5, -5.0, 1.25, 0.0], produced)

    def test_accumulator_of_a_unit_step_counts_the_steps(self) -> None:
        """y(k) = y(k-1) + 1 has the exact solution y(k) = k + 1."""
        produced = run(iir_graph([1.0]), step(50))
        self.assertEqual([float(k + 1) for k in range(50)], produced)

    def test_first_order_impulse_response_is_a_geometric_sequence(self) -> None:
        """y(k) = a*y(k-1) + b*u(k) has impulse response b*a^k."""
        for a, b in ((0.5, 1.0), (-0.8, 2.0), (0.95, 0.1)):
            produced = run(iir_graph([a], gain=b), impulse(60))
            for k, value in enumerate(produced):
                self.assertAlmostEqual(b * a**k, value, places=12, msg=f"a={a} k={k}")

    def test_first_order_step_response_is_the_geometric_series(self) -> None:
        """The same system driven by a unit step: y(k) = b*(1-a^(k+1))/(1-a)."""
        for a, b in ((0.5, 1.0), (-0.3, 1.5), (0.9, 0.25)):
            produced = run(iir_graph([a], gain=b), step(80))
            for k, value in enumerate(produced):
                expected = b * (1.0 - a ** (k + 1)) / (1.0 - a)
                self.assertAlmostEqual(expected, value, places=12, msg=f"a={a} k={k}")

    def test_first_order_settles_at_its_dc_gain(self) -> None:
        a, b = 0.7, 0.3
        produced = run(iir_graph([a], gain=b), step(400))
        self.assertAlmostEqual(b / (1.0 - a), produced[-1], places=12)

    def test_undamped_resonator_is_an_exact_sinusoid(self) -> None:
        """y(k) = 2cos(w)y(k-1) - y(k-2) + u(k) rings at sin((k+1)w)/sin(w).

        This is the marginally stable case, so any leak in the delay chain shows
        up as drift over a long horizon.
        """
        for omega in (math.pi / 8, math.pi / 3, 1.0):
            graph = iir_graph([2.0 * math.cos(omega), -1.0])
            produced = run(graph, impulse(300))
            for k, value in enumerate(produced):
                expected = math.sin((k + 1) * omega) / math.sin(omega)
                self.assertAlmostEqual(expected, value, places=9, msg=f"w={omega} k={k}")

    def test_damped_resonator_matches_its_closed_form(self) -> None:
        """A complex pole pair at radius r: h(k) = r^k sin((k+1)w)/sin(w)."""
        for radius, omega in ((0.9, math.pi / 6), (0.99, 0.4), (0.5, 1.2)):
            graph = iir_graph([2.0 * radius * math.cos(omega), -(radius**2)])
            produced = run(graph, impulse(200))
            for k, value in enumerate(produced):
                expected = radius**k * math.sin((k + 1) * omega) / math.sin(omega)
                self.assertAlmostEqual(expected, value, places=10, msg=f"k={k}")

    def test_fibonacci_recursion_is_exact_in_floating_point(self) -> None:
        """y(k) = y(k-1) + y(k-2) with an impulse gives Fibonacci numbers.

        Integer-valued and exactly representable below 2^53, so this catches any
        rounding introduced by the delay chain rather than by the arithmetic.
        """
        produced = run(iir_graph([1.0, 1.0]), impulse(40))
        previous, current = 0, 1
        for k, value in enumerate(produced):
            self.assertEqual(float(current), value, msg=f"k={k}")
            previous, current = current, previous + current

    def test_alternating_system_flips_sign_every_step(self) -> None:
        produced = run(iir_graph([-1.0]), impulse(20))
        self.assertEqual([(-1.0) ** k for k in range(20)], produced)

    def test_moving_average_is_the_mean_of_the_last_three_inputs(self) -> None:
        graph = Graph()
        graph.add_node(Node("u", "input", {"index": 0}))
        graph.add_node(Node("d1", "delay", {"steps": 1, "initial": 0.0}))
        graph.add_node(Node("d2", "delay", {"steps": 2, "initial": 0.0}))
        graph.add_node(Node("y", "add"))
        graph.add_node(Node("out", "output", {"index": 0}))
        third = 1.0 / 3.0
        graph.add_edge(Edge("e1", "u", "d1"))
        graph.add_edge(Edge("e2", "u", "d2"))
        graph.add_edge(Edge("e3", "u", "y", {"weight": third}))
        graph.add_edge(Edge("e4", "d1", "y", {"weight": third}))
        graph.add_edge(Edge("e5", "d2", "y", {"weight": third}))
        graph.add_edge(Edge("e6", "y", "out"))

        controls = [0.0, 3.0, 6.0, 9.0, 12.0, 0.0, 0.0, 0.0]
        produced = run(graph, [(value,) for value in controls])
        history = [0.0, 0.0] + controls
        for k, value in enumerate(produced):
            expected = (history[k + 2] + history[k + 1] + history[k]) / 3.0
            self.assertAlmostEqual(expected, value, places=12, msg=f"k={k}")

    def test_logistic_map_matches_direct_iteration(self) -> None:
        """A nonlinear feedback system, exercising multiply inside a delay loop.

        y(k+1) = r*y(k)*(1 - y(k)), seeded by the delay's initial condition.
        """
        rate, start = 3.7, 0.4
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("state", "delay", {"steps": 1, "initial": start}),
                Node("complement", "add"),
                Node("one", "constant", {"value": 1.0}),
                Node("product", "multiply"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "one", "complement"),
                Edge("e2", "state", "complement", {"weight": -1.0}),
                Edge("e3", "state", "product", {"weight": rate}),
                Edge("e4", "complement", "product"),
                Edge("e5", "product", "state"),
                Edge("e6", "product", "out"),
            ],
        )
        produced = run(graph, [(0.0,)] * 60)

        value = start
        for k, actual in enumerate(produced):
            value = rate * value * (1.0 - value)
            self.assertAlmostEqual(value, actual, places=12, msg=f"k={k}")


class DelaySemanticsTests(unittest.TestCase):
    def test_a_unit_delay_shifts_the_input_by_one_step(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("d", "delay", {"steps": 1, "initial": 0.0}),
                Node("out", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "d"), Edge("e2", "d", "out")],
        )
        controls = [1.0, 2.0, 3.0, 4.0, 5.0]
        produced = run(graph, [(value,) for value in controls])
        self.assertEqual([0.0, 1.0, 2.0, 3.0, 4.0], produced)

    def test_a_multi_step_delay_shifts_by_its_step_count(self) -> None:
        for steps in (1, 2, 3, 5):
            graph = Graph(
                nodes=[
                    Node("u", "input", {"index": 0}),
                    Node("d", "delay", {"steps": steps, "initial": 0.0}),
                    Node("out", "output", {"index": 0}),
                ],
                edges=[Edge("e1", "u", "d"), Edge("e2", "d", "out")],
            )
            controls = [float(value) for value in range(1, 13)]
            produced = run(graph, [(value,) for value in controls])
            padded = [0.0] * steps + controls
            self.assertEqual(padded[: len(controls)], produced, msg=f"steps={steps}")

    def test_the_initial_condition_fills_the_delay_line(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("d", "delay", {"steps": 3, "initial": 7.0}),
                Node("out", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "d"), Edge("e2", "d", "out")],
        )
        produced = run(graph, [(1.0,), (2.0,), (3.0,), (4.0,), (5.0,)])
        self.assertEqual([7.0, 7.0, 7.0, 1.0, 2.0], produced)

    def test_a_delay_loop_without_input_holds_its_initial_condition(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("d", "delay", {"steps": 1, "initial": 4.0}),
                Node("relay", "add"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "d", "relay"),
                Edge("e2", "relay", "d"),
                Edge("e3", "relay", "out"),
            ],
        )
        self.assertEqual([4.0] * 10, run(graph, [(0.0,)] * 10))

    def test_an_algebraic_loop_is_rejected(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("a", "add"),
                Node("b", "add"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "u", "a"),
                Edge("e2", "a", "b"),
                Edge("e3", "b", "a"),
                Edge("e4", "b", "out"),
            ],
        )
        with self.assertRaises(SignalSimulationError):
            run(graph, [(1.0,)])


class StatefulOperatorTests(unittest.TestCase):
    """Each stateful operator against the difference equation it implements."""

    def _single_operator(self, kind: str, attributes: dict) -> Graph:
        return Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("s", kind, attributes),
                Node("out", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "s"), Edge("e2", "s", "out")],
        )

    def test_integral_of_a_unit_step_is_a_ramp(self) -> None:
        graph = self._single_operator("integral", {"gain": 1.0, "initial": 0.0})
        self.assertEqual([float(k + 1) for k in range(30)], run(graph, step(30)))

    def test_integral_applies_its_gain(self) -> None:
        graph = self._single_operator("integral", {"gain": 0.25, "initial": 2.0})
        produced = run(graph, step(20))
        for k, value in enumerate(produced):
            self.assertAlmostEqual(2.0 + 0.25 * (k + 1), value, places=12)

    def test_derivative_of_a_ramp_is_its_slope(self) -> None:
        graph = self._single_operator("derivative", {"time_step": 1.0, "initial": 0.0})
        ramp = [(float(3 * k),) for k in range(20)]
        produced = run(graph, ramp)
        self.assertEqual([0.0] + [3.0] * 19, produced)

    def test_derivative_divides_by_its_time_step(self) -> None:
        graph = self._single_operator("derivative", {"time_step": 0.5, "initial": 0.0})
        produced = run(graph, [(0.0,), (1.0,), (2.0,)])
        self.assertEqual([0.0, 2.0, 2.0], produced)

    def test_low_pass_follows_its_difference_equation(self) -> None:
        alpha, initial = 0.3, 0.0
        graph = self._single_operator("filter_lp", {"alpha": alpha, "initial": initial})
        controls = [1.0, 1.0, 0.0, -1.0, 2.0, 2.0]
        produced = run(graph, [(value,) for value in controls])

        state = initial
        for k, (control, actual) in enumerate(zip(controls, produced)):
            state = state + alpha * (control - state)
            self.assertAlmostEqual(state, actual, places=12, msg=f"k={k}")

    def test_low_pass_step_response_has_a_closed_form(self) -> None:
        """From rest, y(k) = 1 - (1-alpha)^(k+1) for a unit step."""
        alpha = 0.25
        graph = self._single_operator("filter_lp", {"alpha": alpha, "initial": 0.0})
        produced = run(graph, step(60))
        for k, value in enumerate(produced):
            self.assertAlmostEqual(1.0 - (1.0 - alpha) ** (k + 1), value, places=12)

    def test_high_pass_follows_its_difference_equation(self) -> None:
        alpha = 0.8
        graph = self._single_operator(
            "filter_hp", {"alpha": alpha, "initial": 0.0, "initial_input": 0.0}
        )
        controls = [1.0, 1.0, 1.0, 0.0, 0.0]
        produced = run(graph, [(value,) for value in controls])

        previous_output, previous_input = 0.0, 0.0
        for k, (control, actual) in enumerate(zip(controls, produced)):
            previous_output = alpha * (previous_output + control - previous_input)
            previous_input = control
            self.assertAlmostEqual(previous_output, actual, places=12, msg=f"k={k}")

    def test_high_pass_rejects_a_constant_and_low_pass_passes_it(self) -> None:
        high = self._single_operator(
            "filter_hp", {"alpha": 0.7, "initial": 0.0, "initial_input": 0.0}
        )
        low = self._single_operator("filter_lp", {"alpha": 0.4, "initial": 0.0})
        self.assertAlmostEqual(0.0, run(high, step(200))[-1], places=9)
        self.assertAlmostEqual(1.0, run(low, step(200))[-1], places=9)


class SessionSemanticsTests(unittest.TestCase):
    def setUp(self) -> None:
        self.graph = iir_graph([0.8])

    def test_two_sessions_do_not_share_state(self) -> None:
        simulator = SignalSimulator()
        first = simulator.start(self.graph)
        second = simulator.start(self.graph)
        first_values = [first.step(row)[0] for row in step(10)]
        second_values = [second.step(row)[0] for row in step(10)]
        self.assertEqual(first_values, second_values)

    def test_interleaved_sessions_stay_independent(self) -> None:
        simulator = SignalSimulator()
        first, second = simulator.start(self.graph), simulator.start(self.graph)
        interleaved = [(first.step((1.0,))[0], second.step((1.0,))[0]) for _ in range(10)]
        for left, right in interleaved:
            self.assertEqual(left, right)

    def test_reset_restores_the_initial_conditions(self) -> None:
        session = SignalSimulator().start(self.graph)
        first = [session.step(row)[0] for row in step(15)]
        session.reset()
        self.assertEqual(first, [session.step(row)[0] for row in step(15)])

    def test_a_session_does_not_mutate_the_source_graph(self) -> None:
        before = {node.id: dict(node.attributes) for node in self.graph.nodes.values()}
        run(self.graph, step(25))
        after = {node.id: dict(node.attributes) for node in self.graph.nodes.values()}
        self.assertEqual(before, after)

    def test_repeated_runs_of_the_same_graph_agree(self) -> None:
        self.assertEqual(run(self.graph, step(30)), run(self.graph, step(30)))


class StructuralTests(unittest.TestCase):
    def test_inputs_are_read_by_their_index(self) -> None:
        graph = Graph(
            nodes=[
                Node("a", "input", {"index": 0}),
                Node("b", "input", {"index": 1}),
                Node("difference", "add"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "a", "difference"),
                Edge("e2", "b", "difference", {"weight": -1.0}),
                Edge("e3", "difference", "out"),
            ],
        )
        produced = SignalSimulator().run_series(graph, [(5.0, 3.0), (1.0, 4.0)])
        self.assertEqual([[2.0], [-3.0]], produced)

    def test_outputs_are_emitted_in_index_order(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("double", "add"),
                Node("negated", "negate"),
                Node("second", "output", {"index": 1}),
                Node("first", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "u", "double", {"weight": 2.0}),
                Edge("e2", "u", "negated"),
                Edge("e3", "double", "first"),
                Edge("e4", "negated", "second"),
            ],
        )
        self.assertEqual([[6.0, -3.0]], SignalSimulator().run_series(graph, [(3.0,)]))

    def test_edge_weights_scale_each_contribution(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("scaled", "add"),
                Node("relay", "add"),
                Node("total", "add"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "u", "scaled", {"weight": 3.0}),
                Edge("e2", "u", "relay", {"weight": -0.5}),
                Edge("e3", "scaled", "total", {"weight": 2.0}),
                Edge("e4", "relay", "total"),
                Edge("e5", "total", "out"),
            ],
        )
        # (2 * 3 * u) + (-0.5 * u) = 5.5 * u
        self.assertEqual([[11.0]], SignalSimulator().run_series(graph, [(2.0,)]))

    def test_a_fan_in_product_multiplies_every_branch(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("a", "add"),
                Node("b", "add"),
                Node("c", "add"),
                Node("product", "multiply"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "u", "a", {"weight": 2.0}),
                Edge("e2", "u", "b", {"weight": 3.0}),
                Edge("e3", "u", "c", {"weight": 5.0}),
                Edge("e4", "a", "product"),
                Edge("e5", "b", "product"),
                Edge("e6", "c", "product"),
                Edge("e7", "product", "out"),
            ],
        )
        # 2u * 3u * 5u = 30 * u^3
        self.assertEqual([[30.0]], SignalSimulator().run_series(graph, [(1.0,)]))
        self.assertEqual([[240.0]], SignalSimulator().run_series(graph, [(2.0,)]))

    def test_a_diverging_system_is_reported_rather_than_returned(self) -> None:
        """An unstable recursion overflows; the engine must not return infinity."""
        with self.assertRaises(SignalSimulationError):
            run(iir_graph([1.9, 0.9]), step(4000))

    def test_a_non_finite_intermediate_is_rejected(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("inverse", "reciprocal"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "inverse"), Edge("e2", "inverse", "out")],
        )
        with self.assertRaises(SignalSimulationError):
            run(graph, [(0.0,)])


if __name__ == "__main__":
    unittest.main()
