"""Local parameter search applied after a structural change."""

import unittest
from random import Random

from ameba_graph import Edge, Graph, Node, ParameterRefiner, RefinementConfig
from ameba_graph.mutation import MutateEdgeAttributes, MutateNodeAttributes, MutationError
from ameba_graph.serialization import graph_dumps
from ameba_signal import SignalEvaluator
from ameba.benchmarks import (
    benchmark_engine,
    benchmark_policy,
    identification_dataset,
    linear,
    seed_graph,
    training_controls,
)


def tuneable_graph(weight: float) -> Graph:
    """u -> add -> output, with a single weight to tune."""
    return Graph(
        nodes=[
            Node("u_k", "input", {"index": 0}, locked=True),
            Node("gain", "add"),
            Node("y_next", "output", {"index": 0}, locked=True),
        ],
        edges=[
            Edge("e1", "u_k", "gain", {"weight": weight}),
            Edge("e2", "gain", "y_next"),
        ],
    )


class RefinementConfigTests(unittest.TestCase):
    def test_defaults_are_coherent(self) -> None:
        config = RefinementConfig()
        self.assertLessEqual(config.min_steps, config.max_steps)
        self.assertGreater(config.patience, 0)

    def test_invalid_budgets_are_rejected(self) -> None:
        for kwargs in (
            {"min_steps": -1},
            {"patience": 0},
            {"max_steps": 2, "min_steps": 5},
            {"min_improvement": -0.1},
            {"scales": ()},
            {"scales": (0.0,)},
            {"scales": (1.1,)},
        ):
            with self.assertRaises(ValueError, msg=str(kwargs)):
                RefinementConfig(**kwargs)

    def test_at_least_one_mutation_is_required(self) -> None:
        with self.assertRaises(ValueError):
            ParameterRefiner([])


class RefinementBehaviourTests(unittest.TestCase):
    def setUp(self) -> None:
        self.policy = benchmark_policy()
        trajectory = linear.linear_trajectory(training_controls(80, Random(7)))
        self.evaluator = SignalEvaluator(identification_dataset(trajectory))
        self.refiner = ParameterRefiner(
            [MutateEdgeAttributes(), MutateNodeAttributes()],
            RefinementConfig(min_steps=10, patience=8, max_steps=80),
        )

    def test_refinement_improves_a_badly_tuned_graph(self) -> None:
        graph = tuneable_graph(1.9)
        before = self.evaluator.evaluate(graph)
        result = self.refiner.refine(graph, self.evaluator, self.policy, Random(1))
        self.assertLess(result.score, before)
        self.assertGreater(result.improvements, 0)

    def test_refinement_never_returns_something_worse(self) -> None:
        """It keeps the best it has seen, so it cannot regress."""
        for weight in (0.05, 0.5, 1.5, -1.2):
            graph = tuneable_graph(weight)
            before = self.evaluator.evaluate(graph)
            result = self.refiner.refine(graph, self.evaluator, self.policy, Random(3))
            self.assertLessEqual(result.score, before, msg=f"weight={weight}")

    def test_the_reported_score_matches_the_returned_graph(self) -> None:
        result = self.refiner.refine(
            tuneable_graph(1.7), self.evaluator, self.policy, Random(5)
        )
        self.assertAlmostEqual(
            self.evaluator.evaluate(result.graph), result.score, places=12
        )

    def test_the_result_is_still_accepted_by_the_policy(self) -> None:
        result = self.refiner.refine(
            tuneable_graph(1.8), self.evaluator, self.policy, Random(9)
        )
        self.policy.validate(result.graph)

    def test_the_original_graph_is_left_alone(self) -> None:
        graph = tuneable_graph(1.9)
        before = dict(graph.edges["e1"].attributes)
        self.refiner.refine(graph, self.evaluator, self.policy, Random(2))
        self.assertEqual(before, graph.edges["e1"].attributes)

    def test_it_always_spends_the_minimum_effort(self) -> None:
        """A new topology gets its fair hearing before patience can end the run."""
        refiner = ParameterRefiner(
            [MutateEdgeAttributes()],
            RefinementConfig(min_steps=15, patience=1, max_steps=60),
        )
        result = refiner.refine(
            tuneable_graph(0.12), self.evaluator, self.policy, Random(4)
        )
        self.assertGreaterEqual(result.steps, 15)

    def test_it_stops_once_progress_stalls(self) -> None:
        """Patience ends a run that is no longer improving, short of the ceiling."""
        refiner = ParameterRefiner(
            [MutateEdgeAttributes()],
            RefinementConfig(min_steps=0, patience=3, max_steps=500),
        )
        result = refiner.refine(
            tuneable_graph(0.12), self.evaluator, self.policy, Random(6)
        )
        self.assertLess(result.steps, 500)

    def test_it_keeps_going_while_progress_continues(self) -> None:
        refiner = ParameterRefiner(
            [MutateEdgeAttributes()],
            RefinementConfig(min_steps=0, patience=10, max_steps=400),
        )
        result = refiner.refine(
            tuneable_graph(1.95), self.evaluator, self.policy, Random(8)
        )
        self.assertGreater(result.improvements, 0)
        self.assertLessEqual(result.steps, 400)

    def test_the_hard_ceiling_is_respected(self) -> None:
        """Without one, a candidate improving by a sliver each time never stops."""
        refiner = ParameterRefiner(
            [MutateEdgeAttributes()],
            RefinementConfig(min_steps=0, patience=1000, max_steps=12),
        )
        result = refiner.refine(
            tuneable_graph(1.9), self.evaluator, self.policy, Random(7)
        )
        self.assertLessEqual(result.steps, 12)

    def test_it_copes_when_every_mutation_declines(self) -> None:
        class AlwaysDeclines:
            def mutate(self, graph, policy, rng):
                raise MutationError("nothing to tune")

        refiner = ParameterRefiner(
            [AlwaysDeclines()], RefinementConfig(min_steps=3, patience=1, max_steps=5)
        )
        graph = tuneable_graph(0.5)
        result = refiner.refine(graph, self.evaluator, self.policy, Random(1))
        self.assertEqual(0, result.improvements)
        self.assertAlmostEqual(self.evaluator.evaluate(graph), result.score, places=12)

    def test_refinement_is_reproducible(self) -> None:
        first = self.refiner.refine(
            tuneable_graph(1.6), self.evaluator, self.policy, Random(11)
        )
        second = self.refiner.refine(
            tuneable_graph(1.6), self.evaluator, self.policy, Random(11)
        )
        self.assertEqual(first.score, second.score)
        self.assertEqual(first.steps, second.steps)


class EngineIntegrationTests(unittest.TestCase):
    def setUp(self) -> None:
        trajectory = linear.linear_trajectory(training_controls(60, Random(7)))
        self.dataset = identification_dataset(trajectory)
        self.budget = RefinementConfig(min_steps=3, patience=2, max_steps=8)

    def test_a_refined_engine_still_produces_valid_graphs(self) -> None:
        engine = benchmark_engine(
            self.dataset, seed=0, population_size=8, refine=self.budget
        )
        result = engine.run([seed_graph().copy() for _ in range(8)], 5)
        policy = benchmark_policy()
        for individual in result.population:
            policy.validate(individual.graph)

    def test_a_refined_run_is_reproducible(self) -> None:
        def run() -> float:
            engine = benchmark_engine(
                self.dataset, seed=4, population_size=8, refine=self.budget
            )
            return engine.run([seed_graph().copy() for _ in range(8)], 5).best.score

        self.assertEqual(run(), run())

    def test_parallel_refinement_matches_serial_refinement_exactly(self) -> None:
        def run(workers: int):
            with benchmark_engine(
                self.dataset,
                seed=4,
                population_size=8,
                refine=self.budget,
                simulation_workers=workers,
            ) as engine:
                return engine.run([seed_graph().copy() for _ in range(8)], 3)

        serial, parallel = run(1), run(2)
        self.assertEqual(
            [(item.score, graph_dumps(item.graph)) for item in serial.population],
            [(item.score, graph_dumps(item.graph)) for item in parallel.population],
        )

    def test_refinement_is_off_unless_asked_for(self) -> None:
        self.assertIsNone(benchmark_engine(self.dataset, seed=0).refiner)
        self.assertIsNotNone(
            benchmark_engine(self.dataset, seed=0, refine=self.budget).refiner
        )


if __name__ == "__main__":
    unittest.main()
