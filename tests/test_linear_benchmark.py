import math
import unittest
from random import Random

from ameba.benchmarks import (
    benchmark_engine,
    benchmark_policy,
    evaluation_controls,
    identification_dataset,
    linear,
    seed_graph,
    static_gain_floor,
    step_controls,
    training_controls,
)
from ameba_signal import SignalEvaluator, SignalSimulator
from ameba_signal.stateful import STATEFUL_KINDS


def reference_plant(controls: list[float]) -> list[float]:
    """An independent transcription of the difference equation."""
    history = [0.0, 0.0, 0.0]  # y(k-2), y(k-1), y(k)
    responses = []
    for u_k in controls:
        y_next = (
            1.5 * history[2] - 0.74 * history[1] + 0.12 * history[0] + 0.12 * u_k
        )
        responses.append(y_next)
        history = [history[1], history[2], y_next]
    return responses


def stateful_nodes(graph) -> int:
    return sum(1 for node in graph.nodes.values() if node.kind in STATEFUL_KINDS)


class LinearPlantTests(unittest.TestCase):
    def setUp(self) -> None:
        self.controls = list(training_controls(150, Random(7)))
        self.trajectory = linear.linear_trajectory(self.controls)

    def test_plant_matches_independent_recursion(self) -> None:
        expected = reference_plant(self.controls)
        for step, (want, got) in enumerate(zip(expected, self.trajectory.responses)):
            self.assertAlmostEqual(want, got, places=12, msg=f"step {step}")

    def test_first_step_is_the_input_gain(self) -> None:
        # From rest all history terms are zero, so y(1) = GAIN * u(0).
        self.assertAlmostEqual(
            linear.GAIN * self.controls[0], self.trajectory.responses[0], places=12
        )

    def test_plant_is_stable(self) -> None:
        """Poles at 0.4, 0.5, 0.6, so a sustained input must settle, not diverge."""
        trajectory = linear.linear_trajectory([1.0] * 400)
        self.assertTrue(all(math.isfinite(value) for value in trajectory.responses))
        # Unit DC gain: GAIN was chosen as 1 - sum(FEEDBACK).
        self.assertAlmostEqual(1.0, trajectory.responses[-1], places=6)

    def test_plant_has_memory(self) -> None:
        """The same input value maps to different outputs, so state is required."""
        trajectory = linear.linear_trajectory(step_controls(60, hold=10))
        first = trajectory.responses[10]
        later = trajectory.responses[30]
        self.assertEqual(trajectory.controls[10], trajectory.controls[30])
        self.assertGreater(abs(first - later), 1e-6)


class LinearReferenceGraphTests(unittest.TestCase):
    def setUp(self) -> None:
        self.policy = benchmark_policy()
        self.trajectory = linear.linear_trajectory(training_controls(200, Random(7)))

    def test_reference_graph_satisfies_the_signal_policy(self) -> None:
        self.policy.validate(linear.reference_graph())

    def test_reference_graph_is_minimal(self) -> None:
        graph = linear.reference_graph()
        self.assertEqual(3, stateful_nodes(graph))
        self.assertEqual(6, len(graph.nodes))

    def test_reference_graph_reproduces_the_plant(self) -> None:
        dataset = identification_dataset(self.trajectory)
        self.assertLess(SignalEvaluator(dataset).evaluate(linear.reference_graph()), 1e-20)

    def test_reference_graph_tracks_the_plant_step_by_step(self) -> None:
        rows = [(control,) for control in self.trajectory.controls]
        produced = SignalSimulator().run_series(linear.reference_graph(), rows)
        for step, (want, got) in enumerate(zip(self.trajectory.responses, produced)):
            self.assertAlmostEqual(want, got[0], places=10, msg=f"step {step}")

    def test_reference_graph_generalizes_to_other_inputs(self) -> None:
        for controls in (evaluation_controls(250), step_controls(200, hold=25)):
            dataset = identification_dataset(linear.linear_trajectory(controls))
            self.assertLess(
                SignalEvaluator(dataset).evaluate(linear.reference_graph()), 1e-20
            )

    def test_session_restarts_from_the_initial_conditions(self) -> None:
        rows = [(control,) for control in self.trajectory.controls[:40]]
        session = SignalSimulator().start(linear.reference_graph())
        first = [session.step(row) for row in rows]
        session.reset()
        self.assertEqual(first, [session.step(row) for row in rows])


class LinearIdentificationTests(unittest.TestCase):
    """The entry rung: evolution is expected to pass this one."""

    def setUp(self) -> None:
        self.trajectory = linear.linear_trajectory(training_controls(150, Random(7)))
        self.dataset = identification_dataset(self.trajectory)
        self.seed = seed_graph()
        self.floor = static_gain_floor(self.trajectory)

    def test_the_memoryless_floor_is_a_meaningful_target(self) -> None:
        """A stateless model cannot beat the floor, and the seed does not."""
        baseline = SignalEvaluator(self.dataset).evaluate(self.seed)
        self.assertGreater(baseline, self.floor)
        self.assertEqual(0, stateful_nodes(self.seed))

    def test_evolution_identifies_the_plant_dynamics(self) -> None:
        result = benchmark_engine(self.dataset, seed=0, population_size=16).run(
            [self.seed.copy() for _ in range(16)], 100
        )
        # Scoring below the memoryless floor is not reachable without state.
        self.assertLess(result.best.score, self.floor)
        self.assertGreaterEqual(stateful_nodes(result.best.graph), 1)
        benchmark_policy().validate(result.best.graph)

    def test_evolution_makes_substantial_progress_on_its_seed(self) -> None:
        """A quality check, deliberately loose.

        How far a run gets in a fixed budget depends on the operator set --
        restricting to the basic arithmetic operators made models larger and
        slower to find. The meaningful criterion is beating the memoryless
        floor, which is asserted separately; this only guards against the
        search stalling outright.
        """
        baseline = SignalEvaluator(self.dataset).evaluate(self.seed)
        result = benchmark_engine(self.dataset, seed=0, population_size=16).run(
            [self.seed.copy() for _ in range(16)], 100
        )
        self.assertLess(result.best.score, baseline / 4.0)

    def test_identification_runs_are_reproducible(self) -> None:
        scores = [
            benchmark_engine(self.dataset, seed=3, population_size=12)
            .run([self.seed.copy() for _ in range(12)], 30)
            .best.score
            for _ in range(2)
        ]
        self.assertEqual(scores[0], scores[1])

    def test_evolution_preserves_the_locked_interface(self) -> None:
        result = benchmark_engine(self.dataset, seed=0, population_size=12).run(
            [self.seed.copy() for _ in range(12)], 30
        )
        for individual in result.population:
            kinds = [node.kind for node in individual.graph.nodes.values()]
            self.assertEqual(1, kinds.count("input"))
            self.assertEqual(1, kinds.count("output"))


if __name__ == "__main__":
    unittest.main()
