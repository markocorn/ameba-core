import math
import unittest
from random import Random

from ameba.benchmarks import (
    benchmark_engine,
    benchmark_policy,
    dynamic_population,
    evaluation_controls,
    identification_dataset,
    narendra,
    seed_graph,
    static_gain_floor,
    training_controls,
)
from ameba_graph import Edge, Graph, Node
from ameba_signal import SignalEvaluator, SignalSimulator


def reference_plant(controls: list[float]) -> list[float]:
    """An independent transcription of the published difference equation."""
    y = [0.0, 0.0, 0.0]  # y(k-2), y(k-1), y(k)
    u = [0.0]  # u(k-1)
    responses = []
    for u_k in controls:
        numerator = y[2] * y[1] * y[0] * u[0] * (y[0] - 1.0) + u_k
        denominator = 1.0 + y[1] ** 2 + y[0] ** 2
        y_next = numerator / denominator
        responses.append(y_next)
        y = [y[1], y[2], y_next]
        u = [u_k]
    return responses


class NarendraPlantTests(unittest.TestCase):
    def setUp(self) -> None:
        self.controls = list(training_controls(150, Random(7)))
        self.trajectory = narendra.narendra_trajectory(self.controls)

    def test_plant_matches_independent_recursion(self) -> None:
        expected = reference_plant(self.controls)
        self.assertEqual(len(expected), len(self.trajectory.responses))
        for step, (want, got) in enumerate(zip(expected, self.trajectory.responses)):
            self.assertAlmostEqual(want, got, places=12, msg=f"step {step}")

    def test_first_step_reduces_to_the_control_input(self) -> None:
        # From rest every history term is zero, so y(1) = u(0) / 1.
        self.assertAlmostEqual(self.controls[0], self.trajectory.responses[0], places=12)

    def test_plant_stays_bounded_under_the_standard_excitation(self) -> None:
        self.assertTrue(all(math.isfinite(value) for value in self.trajectory.responses))
        self.assertLess(max(abs(value) for value in self.trajectory.responses), 10.0)

    def test_denominator_never_vanishes(self) -> None:
        """1 + y(k-1)^2 + y(k-2)^2 >= 1, so the plant itself never divides by zero."""
        responses = (0.0, 0.0) + self.trajectory.responses
        for step in range(len(self.trajectory.responses)):
            denominator = 1.0 + responses[step + 1] ** 2 + responses[step] ** 2
            self.assertGreaterEqual(denominator, 1.0)


class NarendraReferenceGraphTests(unittest.TestCase):
    def setUp(self) -> None:
        self.policy = benchmark_policy()
        self.trajectory = narendra.narendra_trajectory(training_controls(200, Random(7)))

    def test_reference_graph_satisfies_the_signal_policy(self) -> None:
        self.policy.validate(narendra.reference_graph())

    def test_reference_graph_reproduces_the_plant(self) -> None:
        """Driven only by u(k), with three delay loops carrying the history."""
        dataset = identification_dataset(self.trajectory)
        self.assertLess(SignalEvaluator(dataset).evaluate(narendra.reference_graph()), 1e-20)

    def test_reference_graph_tracks_the_plant_step_by_step(self) -> None:
        rows = [(control,) for control in self.trajectory.controls]
        produced = SignalSimulator().run_series(narendra.reference_graph(), rows)
        for step, (want, got) in enumerate(zip(self.trajectory.responses, produced)):
            self.assertAlmostEqual(want, got[0], places=10, msg=f"step {step}")

    def test_reference_graph_generalizes_to_the_sinusoidal_input(self) -> None:
        trajectory = narendra.narendra_trajectory(evaluation_controls(250))
        dataset = identification_dataset(trajectory)
        self.assertLess(SignalEvaluator(dataset).evaluate(narendra.reference_graph()), 1e-20)

    def test_session_restarts_from_the_initial_conditions(self) -> None:
        rows = [(control,) for control in self.trajectory.controls[:40]]
        session = SignalSimulator().start(narendra.reference_graph())
        first = [session.step(row) for row in rows]
        session.reset()
        self.assertEqual(first, [session.step(row) for row in rows])

    def test_reciprocal_rejects_a_zero_denominator(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("inverse", "reciprocal"),
                Node("y", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "inverse"), Edge("e2", "inverse", "y")],
        )
        self.policy.validate(graph)
        dataset = identification_dataset(narendra.narendra_trajectory([0.0, 0.0]))
        self.assertEqual(math.inf, SignalEvaluator(dataset).evaluate(graph))


class NarendraIdentificationTests(unittest.TestCase):
    """The nonlinear rung. Evolution does not currently clear it."""

    def setUp(self) -> None:
        self.trajectory = narendra.narendra_trajectory(training_controls(150, Random(7)))
        self.dataset = identification_dataset(self.trajectory)
        self.seed = seed_graph()
        self.floor = static_gain_floor(self.trajectory)

    def test_evolution_improves_on_the_uninformed_seed(self) -> None:
        baseline = SignalEvaluator(self.dataset).evaluate(self.seed)
        result = benchmark_engine(self.dataset, seed=0, population_size=12).run(
            [self.seed.copy() for _ in range(12)], 60
        )
        self.assertLess(result.best.score, baseline)
        benchmark_policy().validate(result.best.graph)

    def test_the_benchmark_remains_open(self) -> None:
        """A tripwire, not a requirement.

        The exact model scores at floating-point noise while evolution stalls at
        the memoryless floor. When this test starts failing the benchmark has
        been solved and the documented standing result needs updating.
        """
        result = benchmark_engine(self.dataset, seed=0, population_size=12).run(
            [self.seed.copy() for _ in range(12)], 60
        )
        exact = SignalEvaluator(self.dataset).evaluate(narendra.reference_graph())
        self.assertLess(exact, result.best.score)

    def test_the_memoryless_floor_is_below_the_seed(self) -> None:
        baseline = SignalEvaluator(self.dataset).evaluate(self.seed)
        self.assertLess(self.floor, baseline)

    def test_identification_runs_are_reproducible(self) -> None:
        scores = [
            benchmark_engine(self.dataset, seed=3, population_size=12)
            .run([self.seed.copy() for _ in range(12)], 30)
            .best.score
            for _ in range(2)
        ]
        self.assertEqual(scores[0], scores[1])

    def test_dynamic_initialization_is_repeatable_executable_and_stateful(self) -> None:
        policy = benchmark_policy()
        first = dynamic_population(
            self.dataset, policy, 4, seed=91, min_nodes=6, max_nodes=8, min_stateful=2
        )
        second = dynamic_population(
            self.dataset, policy, 4, seed=91, min_nodes=6, max_nodes=8, min_stateful=2
        )
        evaluator = SignalEvaluator(self.dataset)
        for left, right in zip(first, second):
            self.assertEqual(left.nodes, right.nodes)
            self.assertEqual(left.edges, right.edges)
            self.assertTrue(math.isfinite(evaluator.evaluate(left)))
            self.assertGreaterEqual(
                sum(node.kind in {"delay", "integral", "derivative", "filter_lp", "filter_hp"} for node in left.nodes.values()),
                2,
            )


if __name__ == "__main__":
    unittest.main()
