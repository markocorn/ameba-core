"""Every node kind, checked individually against a known signal.

The final test is a completeness guard: it fails if an operator is added to the
registry without a verification here, so coverage cannot silently rot.
"""

import math
import unittest

from ameba_graph import Edge, Graph, Node
from ameba_signal import SignalSimulationError, SignalSimulator
from ameba_signal.operators import OPERATOR_ARITY, STATELESS_OPERATORS
from ameba_signal.stateful import STATEFUL_KINDS

#: Kinds verified below. Kept in sync with the registry by the last test.
COVERED = {
    "abs",
    "add",
    "constant",
    "delay",
    "derivative",
    "exp",
    "filter_hp",
    "filter_lp",
    "input",
    "integral",
    "log",
    "multiply",
    "negate",
    "output",
    "reciprocal",
    "sin",
    "sqrt",
    "square",
    "tanh",
}


def unary(kind: str, attributes: dict | None = None) -> Graph:
    """u -> <kind> -> output."""
    return Graph(
        nodes=[
            Node("u", "input", {"index": 0}),
            Node("op", kind, attributes or {}),
            Node("out", "output", {"index": 0}),
        ],
        edges=[Edge("e1", "u", "op"), Edge("e2", "op", "out")],
    )


def drive(graph: Graph, values: list[float]) -> list[float]:
    rows = [(value,) for value in values]
    return [row[0] for row in SignalSimulator().run_series(graph, rows)]


class StatelessOperatorTests(unittest.TestCase):
    def test_input_routes_by_index(self) -> None:
        graph = Graph(
            nodes=[
                Node("a", "input", {"index": 0}),
                Node("b", "input", {"index": 1}),
                Node("c", "input", {"index": 2}),
                Node("pick", "add"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "b", "pick"), Edge("e2", "pick", "out")],
        )
        # Only input index 1 is wired, so only its value reaches the output.
        self.assertEqual([[20.0]], SignalSimulator().run_series(graph, [(10.0, 20.0, 30.0)]))

    def test_output_passes_its_input_through_unchanged(self) -> None:
        signal = [0.0, 1.5, -2.25, 1e6, 1e-6]
        self.assertEqual(signal, drive(unary("add"), signal))

    def test_constant_ignores_the_input(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("c", "constant", {"value": -4.25}),
                Node("out", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "c", "out")],
        )
        self.assertEqual([-4.25] * 4, drive(graph, [0.0, 5.0, -5.0, 100.0]))

    def test_add_sums_its_weighted_inputs(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("a", "add"),
                Node("b", "add"),
                Node("total", "add"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "u", "a", {"weight": 2.0}),
                Edge("e2", "u", "b", {"weight": -0.5}),
                Edge("e3", "a", "total"),
                Edge("e4", "b", "total"),
                Edge("e5", "total", "out"),
            ],
        )
        for value in (0.0, 1.0, -3.0, 7.5):
            self.assertAlmostEqual(1.5 * value, drive(graph, [value])[0], places=12)

    def test_multiply_takes_the_product_of_its_branches(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("a", "add"),
                Node("b", "add"),
                Node("product", "multiply"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "u", "a", {"weight": 3.0}),
                Edge("e2", "u", "b", {"weight": -2.0}),
                Edge("e3", "a", "product"),
                Edge("e4", "b", "product"),
                Edge("e5", "product", "out"),
            ],
        )
        for value in (0.0, 1.0, 2.0, -1.5):
            self.assertAlmostEqual(-6.0 * value * value, drive(graph, [value])[0], places=12)

    def test_negate_flips_the_sign(self) -> None:
        signal = [0.0, 1.0, -2.5, 1e3]
        self.assertEqual([-value for value in signal], drive(unary("negate"), signal))

    def test_reciprocal_inverts_its_input(self) -> None:
        signal = [1.0, 2.0, -4.0, 0.5, 1e6]
        produced = drive(unary("reciprocal"), signal)
        for value, actual in zip(signal, produced):
            self.assertAlmostEqual(1.0 / value, actual, places=12)

    def test_reciprocal_is_its_own_inverse(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("first", "reciprocal"),
                Node("second", "reciprocal"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "u", "first"),
                Edge("e2", "first", "second"),
                Edge("e3", "second", "out"),
            ],
        )
        signal = [1.0, 3.0, -7.0, 0.25]
        for value, actual in zip(signal, drive(graph, signal)):
            self.assertAlmostEqual(value, actual, places=12)

    def test_sin_matches_the_trigonometric_values(self) -> None:
        angles = [0.0, math.pi / 6, math.pi / 4, math.pi / 2, math.pi, -math.pi / 2]
        produced = drive(unary("sin"), angles)
        for angle, actual in zip(angles, produced):
            self.assertAlmostEqual(math.sin(angle), actual, places=12)

    def test_sin_applies_its_phase_offset(self) -> None:
        phase = math.pi / 2
        angles = [0.0, math.pi / 6, math.pi / 3]
        produced = drive(unary("sin", {"phase": phase}), angles)
        for angle, actual in zip(angles, produced):
            # sin(x + pi/2) == cos(x)
            self.assertAlmostEqual(math.cos(angle), actual, places=12)

    def test_square_matches_the_product_of_a_value_with_itself(self) -> None:
        signal = [0.0, 1.0, -3.0, 2.5, 0.5]
        produced = drive(unary("square"), signal)
        self.assertEqual([value * value for value in signal], produced)

    def test_sqrt_inverts_square_for_non_negative_input(self) -> None:
        signal = [0.0, 1.0, 4.0, 9.0, 2.25]
        produced = drive(unary("sqrt"), signal)
        for value, actual in zip(signal, produced):
            self.assertAlmostEqual(math.sqrt(value), actual, places=12)
            self.assertAlmostEqual(value, actual * actual, places=12)

    def test_sqrt_rejects_a_negative_input(self) -> None:
        with self.assertRaises(SignalSimulationError):
            drive(unary("sqrt"), [-1.0])

    def test_abs_folds_the_sign_away(self) -> None:
        signal = [0.0, 2.0, -2.0, -1e6, 0.5]
        self.assertEqual([abs(value) for value in signal], drive(unary("abs"), signal))

    def test_exp_matches_the_exponential(self) -> None:
        signal = [0.0, 1.0, -1.0, 2.5, -3.0]
        produced = drive(unary("exp"), signal)
        for value, actual in zip(signal, produced):
            self.assertAlmostEqual(math.exp(value), actual, places=12)

    def test_exp_overflow_is_reported_rather_than_returned(self) -> None:
        with self.assertRaises(SignalSimulationError):
            drive(unary("exp"), [1000.0])

    def test_log_matches_the_natural_logarithm(self) -> None:
        signal = [1.0, math.e, 10.0, 0.5]
        produced = drive(unary("log"), signal)
        for value, actual in zip(signal, produced):
            self.assertAlmostEqual(math.log(value), actual, places=12)

    def test_log_rejects_a_non_positive_input(self) -> None:
        for value in (0.0, -1.0):
            with self.assertRaises(SignalSimulationError):
                drive(unary("log"), [value])

    def test_log_and_exp_are_inverses(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("l", "log", {}),
                Node("e", "exp", {}),
                Node("out", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "l"), Edge("e2", "l", "e"), Edge("e3", "e", "out")],
        )
        signal = [1.0, 2.0, 7.5, 0.25]
        for value, actual in zip(signal, drive(graph, signal)):
            self.assertAlmostEqual(value, actual, places=10)

    def test_tanh_saturates_between_minus_one_and_one(self) -> None:
        signal = [0.0, 0.5, -0.5, 20.0, -20.0]
        produced = drive(unary("tanh"), signal)
        for value, actual in zip(signal, produced):
            self.assertAlmostEqual(math.tanh(value), actual, places=12)
        self.assertAlmostEqual(1.0, produced[3], places=12)
        self.assertAlmostEqual(-1.0, produced[4], places=12)

    def test_sin_is_bounded(self) -> None:
        produced = drive(unary("sin"), [float(value) for value in range(-50, 50)])
        self.assertTrue(all(-1.0 <= value <= 1.0 for value in produced))


class StatefulOperatorSignalTests(unittest.TestCase):
    def test_delay_reproduces_a_shifted_copy_of_a_sinusoid(self) -> None:
        graph = unary("delay", {"steps": 4, "initial": 0.0})
        signal = [math.sin(0.3 * k) for k in range(40)]
        produced = drive(graph, signal)
        self.assertEqual([0.0] * 4, produced[:4])
        for k in range(4, 40):
            self.assertAlmostEqual(signal[k - 4], produced[k], places=12, msg=f"k={k}")

    def test_integral_of_a_cosine_is_its_sine(self) -> None:
        """Integrating cos must give sin, to within the rectangle-rule error.

        ``integral`` is a right-endpoint Riemann sum, whose truncation error is
        O(dt), so the tolerance is a multiple of dt rather than a fixed number
        of decimal places.
        """
        dt = 0.001
        graph = unary("integral", {"gain": dt, "initial": 0.0})
        signal = [math.cos(dt * (k + 1)) for k in range(3000)]
        produced = drive(graph, signal)
        for k in (500, 1500, 2999):
            expected = math.sin(dt * (k + 1))
            self.assertLess(abs(expected - produced[k]), dt, msg=f"k={k}")

    def test_the_integral_error_shrinks_in_proportion_to_the_step(self) -> None:
        """First-order accuracy: halving dt must roughly halve the error."""
        target = 1.0
        errors = []
        for dt in (0.01, 0.005, 0.0025):
            count = int(round(target / dt))
            graph = unary("integral", {"gain": dt, "initial": 0.0})
            signal = [math.cos(dt * (k + 1)) for k in range(count)]
            errors.append(abs(math.sin(target) - drive(graph, signal)[-1]))

        for coarse, fine in zip(errors, errors[1:]):
            self.assertLess(fine, coarse)
            # A first-order method halves its error; allow a margin either way.
            self.assertAlmostEqual(2.0, coarse / fine, delta=0.35)

    def test_derivative_of_a_sine_is_its_cosine(self) -> None:
        dt = 0.001
        graph = unary("derivative", {"time_step": dt, "initial": 0.0})
        signal = [math.sin(dt * k) for k in range(3000)]
        produced = drive(graph, signal)
        for k in (500, 1500, 2999):
            self.assertAlmostEqual(math.cos(dt * k), produced[k], places=3, msg=f"k={k}")

    def test_derivative_then_integral_recovers_the_signal(self) -> None:
        """The two operators are inverses up to the initial condition."""
        dt = 0.01
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("d", "derivative", {"time_step": dt, "initial": 0.0}),
                Node("i", "integral", {"gain": dt, "initial": 0.0}),
                Node("out", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "d"), Edge("e2", "d", "i"), Edge("e3", "i", "out")],
        )
        signal = [math.sin(0.05 * k) + 0.3 * k for k in range(200)]
        produced = drive(graph, signal)
        for k in range(1, 200):
            self.assertAlmostEqual(signal[k], produced[k], places=9, msg=f"k={k}")

    def test_low_pass_attenuates_a_fast_sinusoid_more_than_a_slow_one(self) -> None:
        def amplitude(frequency: float) -> float:
            graph = unary("filter_lp", {"alpha": 0.1, "initial": 0.0})
            produced = drive(graph, [math.sin(frequency * k) for k in range(600)])
            return max(abs(value) for value in produced[300:])

        slow, fast = amplitude(0.01), amplitude(1.5)
        self.assertGreater(slow, 0.8)
        self.assertLess(fast, 0.2)
        self.assertGreater(slow, fast)

    def test_high_pass_attenuates_a_slow_sinusoid_more_than_a_fast_one(self) -> None:
        def amplitude(frequency: float) -> float:
            graph = unary(
                "filter_hp", {"alpha": 0.9, "initial": 0.0, "initial_input": 0.0}
            )
            produced = drive(graph, [math.sin(frequency * k) for k in range(600)])
            return max(abs(value) for value in produced[300:])

        slow, fast = amplitude(0.01), amplitude(2.0)
        self.assertLess(slow, 0.2)
        self.assertGreater(fast, 0.8)

    def test_the_filters_split_a_two_tone_signal(self) -> None:
        """A slow tone plus a fast tone: each filter should recover one of them."""
        signal = [math.sin(0.01 * k) + math.sin(2.0 * k) for k in range(800)]
        low = drive(unary("filter_lp", {"alpha": 0.05, "initial": 0.0}), signal)
        high = drive(
            unary("filter_hp", {"alpha": 0.95, "initial": 0.0, "initial_input": 0.0}),
            signal,
        )
        window = slice(400, 800)
        slow_reference = [math.sin(0.01 * k) for k in range(800)][window]
        fast_reference = [math.sin(2.0 * k) for k in range(800)][window]

        self.assertLess(_rms(_difference(low[window], slow_reference)), 0.25)
        self.assertLess(_rms(_difference(high[window], fast_reference)), 0.25)


class RegistryCoverageTests(unittest.TestCase):
    def test_every_operator_kind_is_verified(self) -> None:
        known = set(STATELESS_OPERATORS) | set(STATEFUL_KINDS) | {"input"}
        self.assertEqual(
            known,
            COVERED,
            "an operator was added or removed without updating these tests",
        )

    def test_every_operator_kind_declares_an_arity(self) -> None:
        known = set(STATELESS_OPERATORS) | set(STATEFUL_KINDS)
        self.assertEqual(known, set(OPERATOR_ARITY))

    def test_declared_arities_are_self_consistent(self) -> None:
        for kind, (minimum, maximum) in OPERATOR_ARITY.items():
            self.assertGreaterEqual(minimum, 0, msg=kind)
            if maximum is not None:
                self.assertLessEqual(minimum, maximum, msg=kind)


def _difference(left, right) -> list[float]:
    return [a - b for a, b in zip(left, right)]


def _rms(values) -> float:
    return (sum(value * value for value in values) / len(values)) ** 0.5


if __name__ == "__main__":
    unittest.main()
