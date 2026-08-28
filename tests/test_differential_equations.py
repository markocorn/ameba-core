"""Differential equations solved as AMEBA graphs, checked against calculus.

Two kinds of check appear here.

Where a discretization reproduces the sampled analytic solution *exactly* --
the digital resonator does, for any linear second-order system -- the test
demands an exact match, which is the strongest evidence available.

Where the discretization is approximate -- forward Euler -- demanding a fixed
tolerance would only be asserting a coincidence. Those tests instead verify the
*order of accuracy*: halving the step must halve the error. A wiring mistake
does not produce clean first-order convergence, so this is a sharper test than
any single tolerance.

Nonlinear systems mostly lack closed forms, so those are checked against known
qualitative properties (limit cycles, period doubling, amplitude-dependent
period) rather than against a second implementation of the same recursion.
"""

import math
import unittest

from ameba_graph import Edge, Graph, Node
from ameba_signal import SignalSimulator


def run(graph: Graph, rows: list[tuple[float, ...]]) -> list[float]:
    return [row[0] for row in SignalSimulator().run_series(graph, rows)]


def second_order_graph(feedback_1: float, feedback_2: float, y1: float, y2: float) -> Graph:
    """y(k) = a1*y(k-1) + a2*y(k-2), started from y(-1)=y1 and y(-2)=y2."""
    return Graph(
        nodes=[
            Node("u", "input", {"index": 0}),
            Node("y", "add"),
            Node("d1", "delay", {"steps": 1, "initial": y1}),
            Node("d2", "delay", {"steps": 1, "initial": y2}),
            Node("out", "output", {"index": 0}),
        ],
        edges=[
            Edge("e_y_d1", "y", "d1"),
            Edge("e_d1_d2", "d1", "d2"),
            Edge("e_d1_y", "d1", "y", {"weight": feedback_1}),
            Edge("e_d2_y", "d2", "y", {"weight": feedback_2}),
            Edge("e_y_out", "y", "out"),
        ],
    )


def euler_decay_graph(rate: float, dt: float, initial: float) -> Graph:
    """dy/dt = -rate*y, by forward Euler through an integral and a delay."""
    return Graph(
        nodes=[
            Node("u", "input", {"index": 0}),
            Node("y", "integral", {"gain": dt, "initial": initial}),
            Node("d", "delay", {"steps": 1, "initial": initial}),
            Node("out", "output", {"index": 0}),
        ],
        edges=[
            Edge("e_y_d", "y", "d"),
            Edge("e_d_y", "d", "y", {"weight": -rate}),
            Edge("e_y_out", "y", "out"),
        ],
    )


class LinearOdeTests(unittest.TestCase):
    """d^2y/dt^2 + 2*zeta*wn*dy/dt + wn^2*y = 0 and its first-order relatives."""

    def test_harmonic_oscillator_reproduces_a_sampled_cosine_exactly(self) -> None:
        """y'' = -w^2 y with y(0)=1, y'(0)=0 has the solution y(t) = cos(wt).

        The undamped digital resonator is exact for sampled sinusoids, so no
        discretization error is admissible here at all.
        """
        for omega, dt in ((1.0, 0.05), (2.5, 0.01), (0.4, 0.2)):
            theta = omega * dt
            graph = second_order_graph(
                2.0 * math.cos(theta), -1.0, math.cos(theta), math.cos(2.0 * theta)
            )
            produced = run(graph, [(0.0,)] * 400)
            for k, value in enumerate(produced):
                self.assertAlmostEqual(
                    math.cos(omega * k * dt), value, places=9, msg=f"w={omega} k={k}"
                )

    def test_the_undamped_oscillator_conserves_its_energy(self) -> None:
        """A marginally stable system must neither grow nor decay over time."""
        omega, dt = 1.0, 0.02
        theta = omega * dt
        graph = second_order_graph(
            2.0 * math.cos(theta), -1.0, math.cos(theta), math.cos(2.0 * theta)
        )
        produced = run(graph, [(0.0,)] * 20000)
        early = max(abs(value) for value in produced[:2000])
        late = max(abs(value) for value in produced[-2000:])
        self.assertAlmostEqual(early, late, places=6)

    def test_damped_oscillator_matches_the_analytic_solution(self) -> None:
        """A mass-spring-damper, sampled.

        y(t) = e^(-zeta*wn*t) * [cos(wd*t) + zeta*wn/wd * sin(wd*t)]
        """
        for natural, damping, dt in ((1.0, 0.1, 0.05), (3.0, 0.3, 0.01), (2.0, 0.05, 0.02)):
            damped = natural * math.sqrt(1.0 - damping**2)
            decay = damping * natural

            def analytic(t: float) -> float:
                return math.exp(-decay * t) * (
                    math.cos(damped * t) + decay / damped * math.sin(damped * t)
                )

            radius = math.exp(-decay * dt)
            theta = damped * dt
            graph = second_order_graph(
                2.0 * radius * math.cos(theta),
                -(radius**2),
                analytic(-dt),
                analytic(-2.0 * dt),
            )
            produced = run(graph, [(0.0,)] * 500)
            for k, value in enumerate(produced):
                self.assertAlmostEqual(analytic(k * dt), value, places=9, msg=f"k={k}")

    def test_critically_stable_and_unstable_poles_behave_as_expected(self) -> None:
        for radius, growing in ((0.98, False), (1.02, True)):
            theta = 0.3
            graph = second_order_graph(
                2.0 * radius * math.cos(theta), -(radius**2), 1.0, 1.0
            )
            produced = run(graph, [(0.0,)] * 300)
            early = max(abs(value) for value in produced[:50])
            late = max(abs(value) for value in produced[-50:])
            self.assertEqual(growing, late > early, msg=f"r={radius}")

    def test_exponential_decay_converges_to_the_analytic_solution(self) -> None:
        """dy/dt = -k*y has y(t) = y0*e^(-kt); Euler is first-order accurate."""
        rate, initial, target = 2.0, 1.0, 1.0
        exact = initial * math.exp(-rate * target)

        errors = []
        for dt in (0.02, 0.01, 0.005):
            count = int(round(target / dt))
            produced = run(euler_decay_graph(rate, dt, initial), [(0.0,)] * count)
            errors.append(abs(exact - produced[-1]))

        for coarse, fine in zip(errors, errors[1:]):
            self.assertLess(fine, coarse)
            self.assertAlmostEqual(2.0, coarse / fine, delta=0.3)

    def test_exponential_decay_matches_its_exact_discrete_recursion(self) -> None:
        """Forward Euler on this system is exactly y(k) = (1 - k*dt)^(k+1)."""
        rate, dt, initial = 1.5, 0.01, 2.0
        produced = run(euler_decay_graph(rate, dt, initial), [(0.0,)] * 200)
        for k, value in enumerate(produced):
            self.assertAlmostEqual(
                initial * (1.0 - rate * dt) ** (k + 1), value, places=10, msg=f"k={k}"
            )

    def test_first_order_lag_converges_to_its_step_response(self) -> None:
        """tau*dy/dt + y = u has step response y(t) = 1 - e^(-t/tau).

        A low-pass with alpha = dt/tau is exactly the Euler discretization.
        """
        tau, target = 0.5, 1.0
        exact = 1.0 - math.exp(-target / tau)

        errors = []
        for dt in (0.02, 0.01, 0.005):
            count = int(round(target / dt))
            graph = Graph(
                nodes=[
                    Node("u", "input", {"index": 0}),
                    Node("lag", "filter_lp", {"alpha": dt / tau, "initial": 0.0}),
                    Node("out", "output", {"index": 0}),
                ],
                edges=[Edge("e1", "u", "lag"), Edge("e2", "lag", "out")],
            )
            errors.append(abs(exact - run(graph, [(1.0,)] * count)[-1]))

        for coarse, fine in zip(errors, errors[1:]):
            self.assertLess(fine, coarse)
            self.assertAlmostEqual(2.0, coarse / fine, delta=0.3)


class NonlinearSystemTests(unittest.TestCase):
    def _pendulum(self, dt: float, gravity_over_length: float, start: float) -> Graph:
        """theta'' = -(g/L) sin(theta), by semi-implicit Euler.

        omega(k) = omega(k-1) - dt*(g/L)*sin(theta(k-1))
        theta(k) = theta(k-1) + dt*omega(k)
        """
        return Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("previous", "delay", {"steps": 1, "initial": start}),
                Node("swing", "sin"),
                Node("omega", "integral", {"gain": dt, "initial": 0.0}),
                Node("theta", "integral", {"gain": dt, "initial": start}),
                Node("out", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "previous", "swing"),
                Edge("e2", "swing", "omega", {"weight": -gravity_over_length}),
                Edge("e3", "omega", "theta"),
                Edge("e4", "theta", "previous"),
                Edge("e5", "theta", "out"),
            ],
        )

    def test_small_angle_pendulum_approximates_simple_harmonic_motion(self) -> None:
        """For small theta, sin(theta) ~ theta and the period is 2*pi*sqrt(L/g)."""
        dt, ratio, start = 0.001, 4.0, 0.01
        period = 2.0 * math.pi / math.sqrt(ratio)
        produced = run(self._pendulum(dt, ratio, start), [(0.0,)] * int(3.0 * period / dt))

        for k in range(0, len(produced), 200):
            expected = start * math.cos(math.sqrt(ratio) * (k + 1) * dt)
            self.assertAlmostEqual(expected, produced[k], delta=0.05 * start, msg=f"k={k}")

    def test_pendulum_period_grows_with_amplitude(self) -> None:
        """The signature nonlinearity: a wide swing is slower than a narrow one.

        This cannot be reproduced by any linear model, so it is direct evidence
        that the sin node participates correctly in the feedback loop.
        """
        dt, ratio = 0.0005, 4.0
        periods = [self._measure_period(dt, ratio, start) for start in (0.05, 2.0)]
        self.assertLess(periods[0], periods[1])
        # A 2 rad swing is roughly 25-45% slower than the small-angle limit.
        self.assertGreater(periods[1] / periods[0], 1.15)

    def _measure_period(self, dt: float, ratio: float, start: float) -> float:
        produced = run(self._pendulum(dt, ratio, start), [(0.0,)] * 40000)
        crossings = [
            k
            for k in range(1, len(produced))
            if produced[k - 1] > 0.0 >= produced[k]
        ]
        self.assertGreaterEqual(len(crossings), 2, "no full oscillation observed")
        return (crossings[-1] - crossings[0]) / (len(crossings) - 1) * dt

    def test_pendulum_conserves_energy_over_many_swings(self) -> None:
        dt, ratio, start = 0.0005, 4.0, 1.0
        produced = run(self._pendulum(dt, ratio, start), [(0.0,)] * 60000)
        early = max(abs(value) for value in produced[:8000])
        late = max(abs(value) for value in produced[-8000:])
        self.assertAlmostEqual(early, late, delta=0.02 * start)

    def _logistic(self, rate: float, start: float) -> Graph:
        return Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("state", "delay", {"steps": 1, "initial": start}),
                Node("one", "constant", {"value": 1.0}),
                Node("complement", "add"),
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

    def test_logistic_map_settles_on_its_analytic_fixed_point(self) -> None:
        """For 1 < r < 3 the orbit converges to 1 - 1/r."""
        for rate in (1.5, 2.0, 2.8):
            produced = run(self._logistic(rate, 0.3), [(0.0,)] * 2000)
            self.assertAlmostEqual(1.0 - 1.0 / rate, produced[-1], places=6, msg=f"r={rate}")

    def test_logistic_map_period_doubles_past_the_first_bifurcation(self) -> None:
        """Past r = 3 the fixed point splits into a two-cycle."""
        produced = run(self._logistic(3.2, 0.3), [(0.0,)] * 4000)
        tail = produced[-200:]
        self.assertAlmostEqual(tail[-1], tail[-3], places=8)
        self.assertAlmostEqual(tail[-2], tail[-4], places=8)
        self.assertGreater(abs(tail[-1] - tail[-2]), 0.1)

    def test_logistic_map_is_chaotic_and_bounded_at_r_four(self) -> None:
        produced = run(self._logistic(4.0, 0.3), [(0.0,)] * 3000)
        self.assertTrue(all(0.0 <= value <= 1.0 for value in produced))
        # Sensitive dependence: a tiny change in the seed diverges completely.
        nudged = run(self._logistic(4.0, 0.3 + 1e-9), [(0.0,)] * 3000)
        self.assertGreater(abs(produced[-1] - nudged[-1]), 0.05)

    def test_a_quadratic_feedback_system_grows_faster_than_exponentially(self) -> None:
        """y(k) = y(k-1)^2 doubles the exponent each step: y(k) = y0^(2^k)."""
        start = 1.5
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("state", "delay", {"steps": 1, "initial": start}),
                Node("relay", "add"),
                Node("square", "multiply"),
                Node("out", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "state", "relay"),
                Edge("e2", "state", "square"),
                Edge("e3", "relay", "square"),
                Edge("e4", "square", "state"),
                Edge("e5", "square", "out"),
            ],
        )
        produced = run(graph, [(0.0,)] * 6)
        for k, value in enumerate(produced):
            self.assertAlmostEqual(start ** (2 ** (k + 1)), value, places=6, msg=f"k={k}")


if __name__ == "__main__":
    unittest.main()
