"""Leaving reproduction operators out by name.

A run should be able to pose a narrower question -- "can structure alone write
the parameters?" -- without a second definition of the portfolio drifting away
from the canonical one. The filter is by class name, which is also the name the
command line takes.

Two properties matter beyond the obvious one. An exclusion has to reach
refinement as well as the portfolio, or a "no parameter mutation" run still
tunes inside the tuner. And a name nothing answers to has to fail rather than
pass quietly: a typo that excludes nothing still produces a number, and the
number looks fine.
"""

import unittest
from random import Random

from ameba_graph import RefinementConfig
from ameba.benchmarks import (
    benchmark_engine,
    benchmark_policy,
    crossover_names,
    identification_dataset,
    linear,
    mutation_names,
    seed_graph,
    training_controls,
)

PARAMETER_MUTATIONS = ("MutateNodeAttributes", "MutateEdgeAttributes")


def portfolio_names(engine) -> set[str]:
    return {type(operator).__name__ for operator in engine.mutations[0].mutations}


class ReproductionExclusionTests(unittest.TestCase):
    def setUp(self) -> None:
        trajectory = linear.linear_trajectory(training_controls(30, Random(7)))
        self.dataset = identification_dataset(trajectory)
        self.budget = RefinementConfig(min_steps=2, patience=1, max_steps=4)

    def test_by_default_every_canonical_operator_is_present(self) -> None:
        engine = benchmark_engine(self.dataset, seed=0)
        self.assertEqual(set(mutation_names()), portfolio_names(engine))
        self.assertEqual(set(crossover_names()), {
            type(item).__name__ for item in engine.crossover.crossovers
        })

    def test_a_named_mutation_leaves_the_portfolio(self) -> None:
        engine = benchmark_engine(self.dataset, seed=0, excluded_mutations=("AddNode",))
        names = portfolio_names(engine)
        self.assertNotIn("AddNode", names)
        self.assertIn("SplitEdge", names)

    def test_a_named_crossover_leaves_the_portfolio(self) -> None:
        engine = benchmark_engine(
            self.dataset, seed=0, excluded_crossovers=("UniformGraphCrossover",)
        )
        names = {type(item).__name__ for item in engine.crossover.crossovers}
        self.assertNotIn("UniformGraphCrossover", names)
        self.assertIn("AlignedAttributeCrossover", names)

    def test_excluding_parameter_mutation_also_silences_refinement(self) -> None:
        """Otherwise the operator keeps running inside the tuner."""
        engine = benchmark_engine(
            self.dataset,
            seed=0,
            refine=self.budget,
            excluded_mutations=PARAMETER_MUTATIONS,
        )
        self.assertIsNone(engine.refiner)
        self.assertTrue(portfolio_names(engine).isdisjoint(PARAMETER_MUTATIONS))
        self.assertIn("ReplaceEdge", portfolio_names(engine))

    def test_excluding_only_one_tuner_operator_leaves_it_refining(self) -> None:
        engine = benchmark_engine(
            self.dataset,
            seed=0,
            refine=self.budget,
            excluded_mutations=("MutateNodeAttributes",),
        )
        self.assertIsNotNone(engine.refiner)
        self.assertEqual(
            {"MutateEdgeAttributes"},
            {type(operator).__name__ for operator in engine.refiner.mutations},
        )

    def test_excluding_every_crossover_leaves_a_mutation_only_search(self) -> None:
        engine = benchmark_engine(
            self.dataset, seed=0, excluded_crossovers=crossover_names()
        )
        self.assertIsNone(engine.crossover)

    def test_an_unknown_name_is_rejected_rather_than_ignored(self) -> None:
        with self.assertRaisesRegex(ValueError, "Unknown mutation"):
            benchmark_engine(self.dataset, seed=0, excluded_mutations=("SplitEdges",))
        with self.assertRaisesRegex(ValueError, "Unknown crossover"):
            benchmark_engine(self.dataset, seed=0, excluded_crossovers=("SplitEdge",))

    def test_excluding_every_mutation_is_refused(self) -> None:
        with self.assertRaisesRegex(ValueError, "At least one mutation"):
            benchmark_engine(self.dataset, seed=0, excluded_mutations=mutation_names())

    def test_a_structure_only_search_still_evolves(self) -> None:
        """The point of the flag: parameters move only when a carrier is replaced."""
        engine = benchmark_engine(
            self.dataset,
            seed=0,
            population_size=8,
            refine=self.budget,
            excluded_mutations=PARAMETER_MUTATIONS,
        )
        result = engine.run([seed_graph().copy() for _ in range(8)], 3)
        self.assertEqual(8, len(result.population))
        policy = benchmark_policy()
        for individual in result.population:
            policy.validate(individual.graph)


if __name__ == "__main__":
    unittest.main()
