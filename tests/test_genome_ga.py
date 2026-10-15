"""The GA's parts, on a toy problem with no graphs in it.

Genomes are eight digits; the score is the squared distance to all sevens, so
zero is the optimum and every part can be checked for what it promises.
"""

import unittest
from random import Random

from ameba_genome.ga import (
    CommaSelection,
    Crowding,
    GAConfig,
    Generational,
    GeneticAlgorithm,
    Islands,
    LinearRanking,
    PlusSelection,
    Population,
    Restart,
    Roulette,
    SteadyState,
    Tournament,
    Truncation,
)

TARGET = 7


def score_many(genomes):
    return [float(sum((gene - TARGET) ** 2 for gene in genome)) for genome in genomes]


def breed(first, second, rng):
    child = list(first)
    if second is not None:
        child = [rng.choice(pair) for pair in zip(first, second)]
    position = rng.randrange(len(child))
    child[position] = rng.randrange(10)
    return tuple(child)


def initial(rng):
    return tuple(rng.randrange(10) for _ in range(8))


def population(seed=0, size=20):
    rng = Random(seed)
    return [initial(rng) for _ in range(size)]


def engine(size=20, seed=1, **parts):
    config = GAConfig(population_size=size, shorter_wins_ties=False)
    return GeneticAlgorithm(breed, score_many, config, seed, initial=initial, **parts)


class SelectionTests(unittest.TestCase):
    def test_every_selection_prefers_better_genomes(self) -> None:
        ga = engine()
        genomes = population()
        pop = Population(genomes, score_many(genomes), ga)
        mean_rank = sum(range(len(genomes))) / len(genomes)
        for selection in (Tournament(3), Truncation(0.2), LinearRanking(1.8), Roulette()):
            rng = Random(3)
            ranks = [pop.order.index(selection.pick(pop, rng)) for _ in range(2000)]
            self.assertLess(sum(ranks) / len(ranks), mean_rank, type(selection).__name__)

    def test_truncation_never_picks_outside_its_fraction(self) -> None:
        ga = engine()
        genomes = population()
        pop = Population(genomes, score_many(genomes), ga)
        rng = Random(1)
        allowed = set(pop.order[:4])
        self.assertTrue(all(Truncation(0.2).pick(pop, rng) in allowed for _ in range(500)))


class SchemeTests(unittest.TestCase):
    SCHEMES = (Generational(2), SteadyState(1), SteadyState(5), PlusSelection(), CommaSelection(2, 1), Crowding())

    def test_every_scheme_keeps_size_and_improves(self) -> None:
        start = population()
        for scheme in self.SCHEMES:
            result = engine(scheme=scheme).run(start, 40)
            self.assertEqual(len(start), len(result.population), type(scheme).__name__)
            self.assertLess(result.history[-1], result.history[0], type(scheme).__name__)

    def test_elitist_schemes_never_lose_the_best(self) -> None:
        for scheme in (Generational(1), SteadyState(1), PlusSelection(), Crowding(), CommaSelection(2, 1)):
            history = engine(scheme=scheme).run(population(), 40).history
            self.assertEqual(sorted(history, reverse=True), history, type(scheme).__name__)

    def test_same_seed_same_run_for_every_scheme(self) -> None:
        for scheme in self.SCHEMES:
            first = engine(scheme=scheme).run(population(), 15)
            second = engine(scheme=scheme).run(population(), 15)
            self.assertEqual(first.history, second.history)
            self.assertEqual(first.population, second.population)


class DiversityTests(unittest.TestCase):
    def test_clearing_ranks_duplicates_behind_distinct_genomes(self) -> None:
        ga = engine(clearing=lambda genome: genome)
        good, worse = (7,) * 8, (0,) * 8
        genomes = [good, good, good, worse]
        pop = Population(genomes, score_many(genomes), ga)
        self.assertEqual(3, pop.order[1])  # the distinct worse genome before the clones

    def test_islands_split_evenly_and_migrate_the_best(self) -> None:
        ga = engine(islands=Islands(count=4, interval=1, migrants=1))
        islands = [(population(seed=k, size=5), score_many(population(seed=k, size=5))) for k in range(4)]
        moved = ga._migrate(islands)
        for k in range(4):
            donor_best = min(islands[(k - 1) % 4][1])
            self.assertIn(donor_best, moved[k][1])

    def test_islands_must_divide_the_population(self) -> None:
        with self.assertRaises(ValueError):
            engine(size=20, islands=Islands(count=3))

    def test_immigrants_replace_only_the_worst(self) -> None:
        ga = engine(immigrants=0.25)
        genomes = population()
        scores = score_many(genomes)
        best_fifteen = sorted(scores)[:15]
        _, new_scores = ga._immigrate(genomes, scores)
        for score in best_fifteen:
            self.assertIn(score, new_scores)

    def test_restart_fires_after_patience_and_keeps_the_best(self) -> None:
        stuck = [(7,) * 8] + [(0,) * 8] * 19  # the optimum: nothing can improve
        result = engine(restart=Restart(patience=5, keep=1)).run(stuck, 12)
        self.assertGreaterEqual(result.restarts[0], 2)
        self.assertEqual(0.0, result.score)

    def test_immigrants_and_restart_need_an_initial_function(self) -> None:
        config = GAConfig(population_size=10)
        with self.assertRaises(ValueError):
            GeneticAlgorithm(breed, score_many, config, 1, immigrants=0.1)


if __name__ == "__main__":
    unittest.main()
