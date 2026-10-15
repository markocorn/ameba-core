"""The multi-channel benchmark plumbing, end to end.

The simulation and evolution layers were already channel-agnostic. What these
tests pin down is the layer added on top of them: the trajectory that carries a
wide plant, the dispatch that must leave the single-channel floor untouched,
and the evaluator behaviour the reports now rely on.
"""

import unittest
from math import isfinite
from random import Random

from ameba.benchmarks import (
    BENCHMARKS,
    FULL_OPERATORS,
    MultiChannelTrajectory,
    benchmark_engine,
    benchmark_policy,
    channel_training_controls,
    identification_dataset,
    linear,
    memoryless_floor,
    quadruple_tank,
    static_gain_floor,
    static_seed_graph,
    stateless_floor,
    stateless_operators,
    training_controls,
    trajectory_dataset,
    trajectory_floor,
    trajectory_interface,
    trajectory_rows,
)
from ameba.benchmarks.common import _solve
from ameba_graph import Edge, Graph, Node
from ameba_signal import Dataset, SignalEvaluator
from ameba_signal.stateful import STATEFUL_KINDS

#: Short runs keep the suite fast; the plant does not care how long the data is.
STEPS = 120
HOLD = 4
TIME_STEP = quadruple_tank.TIME_STEP


def tank_trajectory(name: str, seed: int = 7, steps: int = STEPS):
    """A held input through one of the registered tank variants."""
    controls = channel_training_controls(steps, 2, Random(seed), 0.5, HOLD)
    return quadruple_tank.TRAJECTORIES[name](controls)


def baseline_dataset(name: str) -> Dataset:
    """The dataset the documented baseline figures are quoted from."""
    controls = channel_training_controls(
        BASELINE_STEPS, 2, Random(7), 0.5, quadruple_tank.HOLD
    )
    return trajectory_dataset(quadruple_tank.TRAJECTORIES[name](controls))


def closed_form_floor(dataset: Dataset, time_step: float) -> float:
    """The two-channel least-squares optimum, solved by the 2x2 inverse.

    Deliberately independent of :func:`memoryless_floor`'s Gaussian
    elimination. At two inputs the Gram matrix can be inverted by hand, so
    agreement between the two is evidence rather than a restatement.
    """
    width = len(dataset.inputs[0])
    if width != 2:
        raise ValueError("This closed form is written for two input channels")
    rows, outputs = dataset.inputs, dataset.outputs
    gram = [
        [sum(row[i] * row[j] for row in rows) for j in range(width)]
        for i in range(width)
    ]
    determinant = gram[0][0] * gram[1][1] - gram[0][1] * gram[1][0]
    total = 0.0
    for channel in range(len(outputs[0])):
        moment = [
            sum(row[i] * out[channel] for row, out in zip(rows, outputs))
            for i in range(width)
        ]
        gains = [
            (gram[1][1] * moment[0] - gram[0][1] * moment[1]) / determinant,
            (-gram[1][0] * moment[0] + gram[0][0] * moment[1]) / determinant,
        ]
        total += sum(
            (out[channel] - sum(g * value for g, value in zip(gains, row))) ** 2
            for row, out in zip(rows, outputs)
        )
    return total * time_step


def fitted_static_seed(dataset: Dataset) -> Graph:
    """``static_seed_graph`` with its input weights set to the fitted gains."""
    inputs, outputs = len(dataset.inputs[0]), len(dataset.outputs[0])
    graph = static_seed_graph(inputs, outputs)
    gram = [
        [sum(row[i] * row[j] for row in dataset.inputs) for j in range(inputs)]
        for i in range(inputs)
    ]
    for channel in range(outputs):
        moment = [
            sum(row[i] * out[channel] for row, out in zip(dataset.inputs, dataset.outputs))
            for i in range(inputs)
        ]
        for index, gain in enumerate(_solve(gram, moment)):
            edge = graph.edges[f"e_u{index}_s{channel}"]
            graph.edges[edge.id] = Edge(
                edge.id, edge.source, edge.target, {"weight": gain}
            )
    return graph


#: The measurement budget behind MemorylessStrictnessTests, kept small enough
#: that the assertion stays a regression guard rather than an experiment.
STRICTNESS_SEEDS = (1, 2, 3)
STRICTNESS_STEPS = 150
STRICTNESS_POPULATION = 12
STRICTNESS_GENERATIONS = 20

#: Everything evolution may create, minus every operator that carries state.
#: Delegated rather than restated, so the tests cannot drift from what the
#: library actually searches.
STATELESS_OPERATORS = stateless_operators()

#: The dataset the baseline figures in the docs are quoted from: the plant's own
#: held excitation, which is what the reports use by default.
BASELINE_STEPS = 150
BASELINE_KWARGS = {"population_size": 12, "generations": 20}


def stateless_floor_ratio(name: str, seed: int) -> float:
    """The best score a search with no state at all reaches, over the floor.

    Run at the report's own budget so the number means the same thing as the
    one the docs quote, and seeded so it can be reproduced rather than merely
    believed.
    """
    controls = channel_training_controls(
        STRICTNESS_STEPS, 2, Random(7), 0.5, quadruple_tank.HOLD
    )
    dataset = trajectory_dataset(quadruple_tank.TRAJECTORIES[name](controls))
    floor = memoryless_floor(dataset, "ise", TIME_STEP)
    engine = benchmark_engine(
        dataset,
        seed=seed,
        population_size=STRICTNESS_POPULATION,
        policy=benchmark_policy(STATELESS_OPERATORS),
        criterion="ise",
        time_step=TIME_STEP,
        node_penalty=0.0,
    )
    try:
        population = [static_seed_graph(2, 2) for _ in range(STRICTNESS_POPULATION)]
        result = engine.run(population, STRICTNESS_GENERATIONS)
    finally:
        engine.close()
    return min(item.score for item in result.population) / floor


def algebraic_cycle() -> Graph:
    """A two-in, two-out graph whose only fault is a loop with no delay."""
    return Graph(
        nodes=[
            Node("u0", "input", {"index": 0}),
            Node("u1", "input", {"index": 1}),
            Node("a", "add"),
            Node("b", "add"),
            Node("y0", "output", {"index": 0}),
            Node("y1", "output", {"index": 1}),
        ],
        edges=[
            Edge("e1", "u0", "a"),
            Edge("e2", "u1", "b"),
            Edge("e3", "a", "b"),
            Edge("e4", "b", "a"),
            Edge("e5", "b", "y0"),
            Edge("e6", "a", "y1"),
        ],
    )


class MultiChannelTrajectoryTests(unittest.TestCase):
    def test_a_tank_trajectory_reports_its_interface(self) -> None:
        trajectory = tank_trajectory("tank-minphase")
        self.assertEqual(trajectory.inputs, 2)
        self.assertEqual(trajectory.outputs, 2)
        self.assertEqual(trajectory_interface(trajectory), (2, 2))
        self.assertEqual(len(trajectory.controls), STEPS)

    def test_matched_rectangular_rows_are_accepted(self) -> None:
        trajectory = MultiChannelTrajectory(
            ((1.0, 2.0), (3.0, 4.0)), ((5.0, 6.0), (7.0, 8.0))
        )
        self.assertEqual((trajectory.inputs, trajectory.outputs), (2, 2))

    def test_empty_mismatched_and_ragged_rows_are_rejected(self) -> None:
        with self.assertRaises(ValueError):
            MultiChannelTrajectory((), ())
        with self.assertRaises(ValueError):
            MultiChannelTrajectory(((1.0,),), ((1.0,), (2.0,)))
        with self.assertRaises(ValueError):
            MultiChannelTrajectory(((1.0, 2.0), (3.0,)), ((1.0, 2.0), (3.0, 4.0)))
        with self.assertRaises(ValueError):
            MultiChannelTrajectory(((1.0,),), ((1.0, 2.0), (1.0, 2.0)))

    def test_non_finite_values_are_rejected(self) -> None:
        with self.assertRaises(ValueError):
            MultiChannelTrajectory(((1.0,),), ((float("inf"),),))
        with self.assertRaises(ValueError):
            MultiChannelTrajectory(((float("nan"),),), ((1.0,),))


class SingleChannelDispatchTests(unittest.TestCase):
    """A scalar trajectory has to reach exactly the code it always did."""

    def setUp(self) -> None:
        self.trajectory = linear.linear_trajectory(training_controls(80, Random(3)))

    def test_the_floor_is_the_original_single_gain_floor(self) -> None:
        self.assertEqual(
            trajectory_floor(self.trajectory, "ise", 1.0),
            static_gain_floor(self.trajectory, "ise", 1.0),
        )

    def test_the_dataset_is_the_original_one_channel_form(self) -> None:
        self.assertEqual(
            trajectory_dataset(self.trajectory),
            identification_dataset(self.trajectory),
        )

    def test_rows_read_as_a_single_channel(self) -> None:
        inputs, outputs = trajectory_rows(self.trajectory)
        self.assertEqual(inputs[0], (self.trajectory.controls[0],))
        self.assertEqual(outputs[0], (self.trajectory.responses[0],))
        self.assertEqual(trajectory_interface(self.trajectory), (1, 1))


class MultiChannelFloorTests(unittest.TestCase):
    """The pass criterion on the tank, cross-checked against a closed form."""

    def test_it_matches_an_independent_least_squares_fit(self) -> None:
        for name in sorted(quadruple_tank.TRAJECTORIES):
            with self.subTest(benchmark=name):
                dataset = trajectory_dataset(tank_trajectory(name))
                self.assertAlmostEqual(
                    memoryless_floor(dataset, "ise", TIME_STEP),
                    closed_form_floor(dataset, TIME_STEP),
                    places=6,
                )

    def test_fitted_static_weights_land_exactly_on_the_floor(self) -> None:
        """The seed's structure is the floor's own model, at any width."""
        for name in sorted(quadruple_tank.TRAJECTORIES):
            with self.subTest(benchmark=name):
                dataset = trajectory_dataset(tank_trajectory(name))
                self.assertAlmostEqual(
                    SignalEvaluator(dataset, time_step=TIME_STEP).evaluate(
                        fitted_static_seed(dataset)
                    ),
                    memoryless_floor(dataset, "ise", TIME_STEP),
                    places=6,
                )

    def test_the_floor_leaves_room_to_identify(self) -> None:
        for name in sorted(quadruple_tank.TRAJECTORIES):
            with self.subTest(benchmark=name):
                dataset = trajectory_dataset(tank_trajectory(name))
                energy = sum(value * value for row in dataset.outputs for value in row)
                self.assertGreater(
                    memoryless_floor(dataset, "ise", TIME_STEP), 0.2 * energy
                )


class MultiChannelEvaluatorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.dataset = trajectory_dataset(tank_trajectory("tank-nonminphase"))
        self.evaluator = SignalEvaluator(self.dataset, time_step=TIME_STEP)

    def test_the_score_is_finite_for_an_executable_graph(self) -> None:
        self.assertTrue(isfinite(self.evaluator.evaluate(static_seed_graph(2, 2))))

    def test_the_score_adds_the_channels_up(self) -> None:
        """A decoupled model must score exactly its two channels added."""
        decoupled = Graph(
            nodes=[
                Node("u0", "input", {"index": 0}),
                Node("u1", "input", {"index": 1}),
                Node("y0", "output", {"index": 0}),
                Node("y1", "output", {"index": 1}),
            ],
            edges=[
                Edge("e0", "u0", "y0", {"weight": 0.5}),
                Edge("e1", "u1", "y1", {"weight": -0.25}),
            ],
        )
        total = 0.0
        for channel, weight in ((0, 0.5), (1, -0.25)):
            single = Dataset(
                tuple((row[channel],) for row in self.dataset.inputs),
                tuple((row[channel],) for row in self.dataset.outputs),
            )
            one_channel = Graph(
                nodes=[
                    Node("u0", "input", {"index": 0}),
                    Node("y0", "output", {"index": 0}),
                ],
                edges=[Edge("e", "u0", "y0", {"weight": weight})],
            )
            total += SignalEvaluator(single, time_step=TIME_STEP).evaluate(one_channel)
        self.assertAlmostEqual(self.evaluator.evaluate(decoupled), total, places=10)

    def test_a_mismatched_output_count_is_rejected(self) -> None:
        score, _ = self.evaluator.evaluate_detailed(static_seed_graph(2, 1))
        self.assertEqual(score, self.evaluator.invalid_score)

    def test_a_loop_without_a_delay_is_rejected(self) -> None:
        score, _ = self.evaluator.evaluate_detailed(algebraic_cycle())
        self.assertEqual(score, self.evaluator.invalid_score)

    def test_the_descriptor_ranks_every_step_and_channel(self) -> None:
        size = len(self.dataset.inputs) * len(self.dataset.outputs[0])
        _, descriptor = self.evaluator.describe(static_seed_graph(2, 2))
        assert descriptor is not None
        self.assertEqual(len(descriptor), size)
        self.assertGreaterEqual(min(descriptor), 1.0)
        self.assertLessEqual(max(descriptor), float(size))

    def test_a_graph_that_cannot_run_has_no_descriptor(self) -> None:
        _, descriptor = self.evaluator.describe(algebraic_cycle())
        self.assertIsNone(descriptor)

    def test_describing_a_graph_twice_agrees(self) -> None:
        graph = static_seed_graph(2, 2)
        self.assertEqual(
            self.evaluator.describe(graph), self.evaluator.describe(graph)
        )


class RegisteredBenchmarkTests(unittest.TestCase):
    """The catalogue has to describe each rung accurately."""

    def test_both_tank_variants_are_registered(self) -> None:
        for name in ("tank-minphase", "tank-nonminphase"):
            with self.subTest(benchmark=name):
                spec = BENCHMARKS[name]
                self.assertEqual(spec.channels, 2)
                self.assertEqual(spec.time_step, quadruple_tank.TIME_STEP)
                self.assertIsNone(spec.reference_of)
                self.assertFalse(spec.linear_floor_is_strict)

    def test_every_registered_benchmark_builds_a_matching_trajectory(self) -> None:
        for name, spec in sorted(BENCHMARKS.items()):
            with self.subTest(benchmark=name):
                if spec.channels == 1:
                    controls = training_controls(20, Random(1))
                else:
                    controls = channel_training_controls(
                        20, spec.channels, Random(1), 0.5, HOLD
                    )
                trajectory = spec.build(controls)
                inputs, _ = trajectory_interface(trajectory)
                self.assertEqual(inputs, spec.channels)
                self.assertEqual(len(trajectory_dataset(trajectory).inputs), 20)

    def test_the_single_channel_rungs_are_unaffected(self) -> None:
        for name in ("linear", "narendra"):
            with self.subTest(benchmark=name):
                spec = BENCHMARKS[name]
                self.assertEqual(spec.channels, 1)
                self.assertEqual(spec.time_step, 1.0)
                self.assertTrue(spec.linear_floor_is_strict)
                self.assertIsNotNone(spec.reference_of)


class MemorylessStrictnessTests(unittest.TestCase):
    """How much of the floor a model with no state at all can already reach.

    The floor is the best *linear* static map. That is only provably the best
    memoryless predictor when the controls are independent, and the tank breaks
    both preconditions: its input is held for ten samples, so the controls are
    autocorrelated, and it is genuinely nonlinear -- level varies as the square
    of flow -- so its static input-output relation is not a line.

    A search restricted to stateless operators measures the gap rather than
    assuming it away. Measured over seeds 1-3 at 150 steps, 12 candidates and
    20 generations, the best ratio reached with no state at all was:

    =================  =========================
    variant            stateless best / floor
    =================  =========================
    tank-minphase      0.6184
    tank-nonminphase   0.3140
    =================  =========================

    So the floor is **not** a strict bound on either tank rung: a memoryless
    model reaches a third to two thirds of it. Falling below the floor therefore
    demonstrates nothing about state on these rungs.

    That measurement is what motivates ``linear_floor_is_strict=False`` on both
    tank rungs and the stateless baseline that replaces the closed form where
    the flag is set — see :class:`StatelessBaselineTests`. This class keeps the
    measurement itself, so the reason for the flag stays visible and the numbers
    cannot drift away from the docstrings that quote them.
    """

    def test_a_stateless_search_undercuts_the_floor_on_both_variants(self) -> None:
        for name in sorted(quadruple_tank.TRAJECTORIES):
            with self.subTest(benchmark=name):
                ratio = min(
                    stateless_floor_ratio(name, seed) for seed in STRICTNESS_SEEDS
                )
                self.assertLess(
                    ratio,
                    1.0,
                    "the floor is holding on this rung after all; if that is a "
                    "deliberate change, drop the caveat from docs/benchmarks.md",
                )


class StatelessBaselineTests(unittest.TestCase):
    """The criterion that replaces the closed form where that form fails.

    A closed-form optimum is itself reachable with no state, so it is a
    legitimate memoryless score; where a search finds better, the search is the
    bar. These tests pin both directions: the baseline is measurably stricter on
    the tank rungs, and it confirms rather than contradicts the closed form on
    the single-input ones.
    """

    def test_it_selects_exactly_the_kinds_that_carry_no_state(self) -> None:
        self.assertTrue(set(stateless_operators()).isdisjoint(STATEFUL_KINDS))
        self.assertEqual(
            set(stateless_operators()) | set(STATEFUL_KINDS), set(FULL_OPERATORS)
        )

    def test_it_refuses_a_budget_it_cannot_search(self) -> None:
        dataset = baseline_dataset("tank-minphase")
        for kwargs in ({"population_size": 2}, {"generations": 0}, {"rounds": 0}):
            with self.assertRaises(ValueError, msg=str(kwargs)):
                stateless_floor(dataset, "ise", TIME_STEP, **kwargs)

    def test_it_is_stricter_than_the_closed_form_on_the_tank(self) -> None:
        """The whole reason the flag exists, at the documented budget."""
        for name in sorted(quadruple_tank.TRAJECTORIES):
            with self.subTest(benchmark=name):
                dataset = baseline_dataset(name)
                closed = memoryless_floor(dataset, "ise", TIME_STEP)
                baseline = stateless_floor(
                    dataset, "ise", TIME_STEP, **BASELINE_KWARGS
                )
                self.assertLess(baseline, closed)
                self.assertGreater(baseline, 0.0)
                self.assertFalse(BENCHMARKS[name].linear_floor_is_strict)

    def test_it_confirms_the_closed_form_on_the_single_input_rungs(self) -> None:
        """There, the search finds nothing much better than least squares.

        A flexible search can always fit its sample a little better than a
        linear optimum, so the gap is not zero even where the closed form has
        the right shape. That is why this is a judgement with a tolerance rather
        than a comparison: a few percent is estimator noise, and the tank's
        factor of two is not.
        """
        for name, tolerance in (("linear", 0.85), ("narendra", 0.95)):
            with self.subTest(benchmark=name):
                spec = BENCHMARKS[name]
                dataset = trajectory_dataset(
                    spec.build(training_controls(150, Random(7)))
                )
                closed = memoryless_floor(dataset, "ise", spec.time_step)
                baseline = stateless_floor(
                    dataset, "ise", spec.time_step, **BASELINE_KWARGS
                )
                self.assertTrue(spec.linear_floor_is_strict)
                self.assertGreater(baseline / closed, tolerance)


class MultiChannelSearchTests(unittest.TestCase):
    """A short search over the tank, to prove the whole path runs."""

    @staticmethod
    def _run(seed: int):
        dataset = trajectory_dataset(tank_trajectory("tank-minphase", steps=40))
        engine = benchmark_engine(
            dataset,
            seed=seed,
            population_size=6,
            policy=benchmark_policy(FULL_OPERATORS),
            criterion="ise",
            time_step=TIME_STEP,
            node_penalty=0.0,
        )
        try:
            population = [static_seed_graph(2, 2) for _ in range(6)]
            return engine.run(population, 2)
        finally:
            engine.close()

    def test_it_runs_and_stays_deterministic(self) -> None:
        first = [item.score for item in self._run(1).population]
        second = [item.score for item in self._run(1).population]
        self.assertEqual(first, second)
        # Not every random child is executable, and one that cannot be
        # simulated scores infinity by design. Only the winner has to be real,
        # and the population is sorted best first.
        self.assertTrue(isfinite(first[0]))

    def test_every_child_is_accepted_by_the_policy(self) -> None:
        policy = benchmark_policy(FULL_OPERATORS)
        for item in self._run(1).population:
            policy.validate(item.graph)


if __name__ == "__main__":
    unittest.main()
