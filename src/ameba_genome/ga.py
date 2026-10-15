"""A configurable genetic algorithm over integer genomes.

Nothing here knows what a gene means. The algorithm sees tuples of integers, a
``breed`` function that makes a child from one or two parents, and a scoring
function; lower scores are better. Everything domain-specific -- the
dictionary, decoding, the variation operators themselves -- arrives through
those callables, so the GA can be tuned on its own while the encoding stays
fixed.

The GA is assembled from parts, each replaceable:

``selection``   how a parent is chosen: :class:`Tournament` (default),
                :class:`Truncation`, :class:`LinearRanking`, :class:`Roulette`
``scheme``      how a generation replaces the last: :class:`Generational`
                (default), :class:`SteadyState`, :class:`PlusSelection`,
                :class:`CommaSelection`, :class:`Crowding`
``islands``     split the population into rings of sub-populations that
                exchange their best every few generations
``clearing``    rank exact phenotypic duplicates behind every distinct model,
                so a population cannot fill with clones of one
``immigrants``  replace a fraction of the worst with fresh genomes each
                generation
``restart``     on stagnation, keep the best few and refill the rest fresh

Every scheme spends the same budget per generation -- one population's worth
of offspring -- so generations compare across schemes.

With the defaults the GA is exactly the plain generational GA it replaced: the
same random draws in the same order, so seeds reproduce earlier runs.
"""

from __future__ import annotations

import math
from dataclasses import dataclass, field
from math import isfinite
from random import Random
from typing import Callable, Hashable, Sequence

Genome = tuple[int, ...]
Breed = Callable[[Genome, "Genome | None", Random], Genome]
ScoreMany = Callable[[Sequence[Genome]], list[float]]
Initial = Callable[[Random], Genome]
KeyOf = Callable[[Genome], Hashable]


@dataclass(frozen=True, slots=True)
class GAConfig:
    population_size: int = 60
    elite_size: int = 2
    tournament_size: int = 3
    crossover_rate: float = 0.5
    max_genes: int = 200
    #: Break score ties in favour of the shorter genome. Off, ties are left to
    #: chance, which lets a neutral variant -- an unexpressed record, say --
    #: replace its parent and drift: the arrangement Cartesian GP relies on.
    shorter_wins_ties: bool = True

    def __post_init__(self) -> None:
        if self.population_size < 2:
            raise ValueError("A population needs at least two genomes")
        if not 0 <= self.elite_size < self.population_size:
            raise ValueError("elite_size must leave room for offspring")
        if not 1 <= self.tournament_size <= self.population_size:
            raise ValueError("tournament_size must fit in the population")
        if not 0.0 <= self.crossover_rate <= 1.0:
            raise ValueError("crossover_rate must be a probability")
        if self.max_genes < 1:
            raise ValueError("max_genes must be positive")


@dataclass(slots=True)
class GAResult:
    best: Genome
    score: float
    generations: int
    #: Best score after each generation, the initial population first.
    history: list[float] = field(default_factory=list)
    population: list[Genome] = field(default_factory=list)
    scores: list[float] = field(default_factory=list)
    #: How many restarts fired, per island.
    restarts: list[int] = field(default_factory=list)


# --- Ranking -----------------------------------------------------------------


class Population:
    """One (sub-)population with its ranking resolved once.

    ``keys[i]`` orders genome ``i``: lower is better. With clearing, every
    phenotype after its best instance is ranked behind all distinct ones.
    """

    def __init__(self, genomes: list[Genome], scores: list[float], ga: "GeneticAlgorithm") -> None:
        self.genomes = genomes
        self.scores = scores
        self.keys = ga.rank_keys(genomes, scores)
        self.order = sorted(range(len(genomes)), key=lambda i: self.keys[i])

    def __len__(self) -> int:
        return len(self.genomes)


# --- Selection ---------------------------------------------------------------


@dataclass(frozen=True)
class Tournament:
    size: int = 3

    def pick(self, population: Population, rng: Random) -> int:
        entrants = rng.sample(range(len(population)), self.size)
        return min(entrants, key=lambda i: population.keys[i])


@dataclass(frozen=True)
class Truncation:
    """Uniformly among the best ``fraction`` of the population."""

    fraction: float = 0.2

    def pick(self, population: Population, rng: Random) -> int:
        count = max(1, math.ceil(self.fraction * len(population)))
        return population.order[rng.randrange(count)]

@dataclass(frozen=True)
class LinearRanking:
    """Probability falls linearly with rank; ``pressure`` 1 is uniform, 2 the steepest."""

    pressure: float = 1.7

    def pick(self, population: Population, rng: Random) -> int:
        n = len(population)
        s = self.pressure
        weights = [(2 - s + 2 * (s - 1) * (n - 1 - rank) / (n - 1)) for rank in range(n)]
        return population.order[rng.choices(range(n), weights)[0]]


@dataclass(frozen=True)
class Roulette:
    """Fitness-proportional on ``1 / (1 + score - best)``, since lower is better."""

    def pick(self, population: Population, rng: Random) -> int:
        finite = [s for s in population.scores if isfinite(s)]
        if not finite:
            return rng.randrange(len(population))
        best = min(finite)
        weights = [1.0 / (1.0 + s - best) if isfinite(s) else 0.0 for s in population.scores]
        return rng.choices(range(len(population)), weights)[0]


# --- Schemes -----------------------------------------------------------------


@dataclass(frozen=True)
class Generational:
    """The elite carry over unchanged; offspring fill every other slot."""

    elite: int = 2

    def step(self, ga: "GeneticAlgorithm", population: Population) -> tuple[list[Genome], list[float]]:
        n = len(population)
        keep = population.order[: self.elite]
        survivors = [population.genomes[i] for i in keep]
        scores = [population.scores[i] for i in keep]
        children = ga.offspring(population, n - len(survivors))
        return survivors + children, scores + ga.score(children)


@dataclass(frozen=True)
class SteadyState:
    """Offspring arrive ``batch`` at a time, each replacing the current worst.

    A child replaces the worst only if it ranks no worse, so a neutral variant
    can still drift in. One generation is one population's worth of children.
    """

    batch: int = 1

    def step(self, ga: "GeneticAlgorithm", population: Population) -> tuple[list[Genome], list[float]]:
        genomes, scores = list(population.genomes), list(population.scores)
        made = 0
        while made < len(genomes):
            current = Population(genomes, scores, ga)
            children = ga.offspring(current, min(self.batch, len(genomes) - made))
            made += len(children)
            for child, score in zip(children, ga.score(children)):
                current = Population(genomes, scores, ga)
                worst = current.order[-1]
                if ga.rank_keys([child], [score])[0] <= current.keys[worst]:
                    genomes[worst], scores[worst] = child, score
        return genomes, scores


@dataclass(frozen=True)
class PlusSelection:
    """(mu + lambda): the best of parents and offspring together survive.

    Offspring rank ahead of parents they tie with, so neutral drift continues.
    """

    def step(self, ga: "GeneticAlgorithm", population: Population) -> tuple[list[Genome], list[float]]:
        n = len(population)
        children = ga.offspring(population, n)
        child_scores = ga.score(children)
        genomes = children + population.genomes
        scores = child_scores + population.scores
        pool = Population(genomes, scores, ga)
        keep = pool.order[:n]
        return [genomes[i] for i in keep], [scores[i] for i in keep]


@dataclass(frozen=True)
class CommaSelection:
    """(mu, lambda): only offspring survive, plus an optional elite.

    ``ratio`` offspring per parent are made and the best fill the population.
    That spends ``ratio`` times the budget of the other schemes per generation,
    so compare it at ``generations / ratio``.
    """

    ratio: int = 2
    elite: int = 0

    def step(self, ga: "GeneticAlgorithm", population: Population) -> tuple[list[Genome], list[float]]:
        n = len(population)
        children = ga.offspring(population, n * self.ratio)
        child_scores = ga.score(children)
        keep_parents = population.order[: self.elite]
        pool = Population(children, child_scores, ga)
        keep = pool.order[: n - len(keep_parents)]
        return (
            [population.genomes[i] for i in keep_parents] + [children[i] for i in keep],
            [population.scores[i] for i in keep_parents] + [child_scores[i] for i in keep],
        )


@dataclass(frozen=True)
class Crowding:
    """Every member breeds once and its child competes only with it.

    The child takes its parent's place if it ranks no worse; a second parent,
    when crossover fires, is chosen by the selection operator. Competition is
    local, as in the behavioural archive, so a new structure is judged against
    its own parent rather than against the best of the population.
    """

    def step(self, ga: "GeneticAlgorithm", population: Population) -> tuple[list[Genome], list[float]]:
        genomes, scores = list(population.genomes), list(population.scores)
        children = [ga.child(population, first=i) for i in range(len(genomes))]
        for i, (child, score) in enumerate(zip(children, ga.score(children))):
            if ga.rank_keys([child], [score])[0] <= population.keys[i]:
                genomes[i], scores[i] = child, score
        return genomes, scores


# --- Diversity ---------------------------------------------------------------


@dataclass(frozen=True)
class Islands:
    """``count`` sub-populations on a ring; every ``interval`` generations each
    sends copies of its best ``migrants`` to the next, replacing its worst."""

    count: int = 4
    interval: int = 50
    migrants: int = 2


@dataclass(frozen=True)
class Restart:
    """After ``patience`` generations without improvement, keep the best
    ``keep`` and refill the rest of the (sub-)population with fresh genomes."""

    patience: int = 300
    keep: int = 2


# --- Engine ------------------------------------------------------------------


class GeneticAlgorithm:
    def __init__(
        self,
        breed: Breed,
        score_many: ScoreMany,
        config: GAConfig,
        seed: int,
        selection=None,
        scheme=None,
        islands: Islands | None = None,
        clearing: KeyOf | None = None,
        immigrants: float = 0.0,
        restart: Restart | None = None,
        initial: Initial | None = None,
    ) -> None:
        if (immigrants or restart) and initial is None:
            raise ValueError("immigrants and restart need an initial-genome function")
        if not 0.0 <= immigrants < 1.0:
            raise ValueError("immigrants must be a fraction below one")
        if islands is not None:
            if islands.count < 1 or config.population_size % islands.count:
                raise ValueError("population_size must divide evenly into the islands")
            if islands.migrants >= config.population_size // islands.count:
                raise ValueError("migrants must leave room on an island")
        self.breed = breed
        self.score_many = score_many
        self.config = config
        self.rng = Random(seed)
        self.selection = selection or Tournament(config.tournament_size)
        self.scheme = scheme or Generational(config.elite_size)
        self.islands = islands
        self.clearing = clearing
        self.immigrants = immigrants
        self.restart = restart
        self.initial = initial

    # --- parts the schemes call ---------------------------------------------

    def rank_keys(self, genomes: Sequence[Genome], scores: Sequence[float]) -> list[tuple]:
        base = [
            (s if isfinite(s) else float("inf"), len(g) if self.config.shorter_wins_ties else 0)
            for g, s in zip(genomes, scores)
        ]
        if self.clearing is None:
            return [(0,) + key for key in base]
        # The best instance of each phenotype keeps its place; the rest queue
        # behind every distinct model.
        best_of: dict[Hashable, int] = {}
        phenotypes = [self.clearing(g) for g in genomes]
        for i, phenotype in enumerate(phenotypes):
            if phenotype not in best_of or base[i] < base[best_of[phenotype]]:
                best_of[phenotype] = i
        return [(0 if best_of[p] == i else 1,) + base[i] for i, p in enumerate(phenotypes)]

    def child(self, population: Population, first: int | None = None) -> Genome:
        if first is None:
            first = self.selection.pick(population, self.rng)
        parent = population.genomes[first]
        second = (
            population.genomes[self.selection.pick(population, self.rng)]
            if self.rng.random() < self.config.crossover_rate
            else None
        )
        child = self.breed(parent, second, self.rng)
        if len(child) > self.config.max_genes:
            child = self.breed(parent, None, self.rng)
            if len(child) > self.config.max_genes:
                child = parent
        return child

    def offspring(self, population: Population, count: int) -> list[Genome]:
        return [self.child(population) for _ in range(count)]

    def score(self, genomes: Sequence[Genome]) -> list[float]:
        return list(self.score_many(genomes)) if genomes else []

    # --- the run --------------------------------------------------------------

    def run(
        self,
        population: Sequence[Genome],
        generations: int,
        report: Callable[[int, list[Genome], list[float]], None] | None = None,
        stop: Callable[[list[float]], bool] | None = None,
    ) -> GAResult:
        if len(population) != self.config.population_size:
            raise ValueError("Initial population does not match population_size")
        genomes = [tuple(genome) for genome in population]
        scores = list(self.score_many(genomes))
        count = self.islands.count if self.islands else 1
        size = len(genomes) // count
        islands = [
            (genomes[k * size:(k + 1) * size], scores[k * size:(k + 1) * size]) for k in range(count)
        ]
        best_seen = [min(s) for _, s in islands]
        stalled = [0] * count
        restarts = [0] * count
        history = [min(scores)]
        if report:
            report(0, genomes, scores)
        generation = 0
        for generation in range(1, generations + 1):
            for k in range(count):
                g, s = self.scheme.step(self, Population(*islands[k], self))
                g, s = self._immigrate(g, s)
                g, s, fired = self._restart(g, s, best_seen, stalled, k)
                restarts[k] += fired
                islands[k] = (g, s)
            if self.islands and count > 1 and generation % self.islands.interval == 0:
                islands = self._migrate(islands)
            genomes = [genome for g, _ in islands for genome in g]
            scores = [score for _, s in islands for score in s]
            history.append(min(scores))
            if report:
                report(generation, genomes, scores)
            if stop and stop(history):
                break
        final = Population(genomes, scores, self)
        best = min(range(len(genomes)), key=lambda i: (final.keys[i][1:], len(genomes[i])))
        return GAResult(genomes[best], scores[best], generation, history, genomes, scores, restarts)

    def _immigrate(self, genomes: list[Genome], scores: list[float]):
        count = round(self.immigrants * len(genomes))
        if not count:
            return genomes, scores
        order = Population(genomes, scores, self).order
        fresh = [self.initial(self.rng) for _ in range(count)]
        fresh_scores = self.score(fresh)
        genomes, scores = list(genomes), list(scores)
        for slot, genome, score in zip(order[-count:], fresh, fresh_scores):
            genomes[slot], scores[slot] = genome, score
        return genomes, scores

    def _restart(self, genomes, scores, best_seen, stalled, k):
        if self.restart is None:
            return genomes, scores, 0
        current = min(scores)
        if current < best_seen[k]:
            best_seen[k], stalled[k] = current, 0
            return genomes, scores, 0
        stalled[k] += 1
        if stalled[k] < self.restart.patience:
            return genomes, scores, 0
        stalled[k] = 0
        order = Population(genomes, scores, self).order
        keep = order[: self.restart.keep]
        fresh = [self.initial(self.rng) for _ in range(len(genomes) - len(keep))]
        return (
            [genomes[i] for i in keep] + fresh,
            [scores[i] for i in keep] + self.score(fresh),
            1,
        )

    def _migrate(self, islands):
        """Ring migration from a snapshot, so no genome crosses two boundaries."""
        snapshot = [Population(g, s, self) for g, s in islands]
        moved = []
        for k, (genomes, scores) in enumerate(islands):
            source = snapshot[(k - 1) % len(islands)]
            here = snapshot[k]
            genomes, scores = list(genomes), list(scores)
            for slot, donor in zip(here.order[-self.islands.migrants:][::-1],
                                   source.order[: self.islands.migrants]):
                genomes[slot], scores[slot] = source.genomes[donor], source.scores[donor]
            moved.append((genomes, scores))
        return moved
