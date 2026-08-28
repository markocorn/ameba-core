"""Minimization-oriented evolutionary loop for arbitrary graph genomes."""

from __future__ import annotations

import math
from concurrent.futures import ProcessPoolExecutor
from dataclasses import dataclass
from random import Random
from typing import Iterable, Sequence

from .crossover import CrossoverError
from .model import Graph, GraphError
from .mutation import MutationError
from .protocols import Evaluator, GraphCrossover, GraphMutation, GraphPolicy
from .refinement import ParameterRefiner

_WORKER_EVALUATOR: Evaluator | None = None


def _initialize_evaluation_worker(evaluator: Evaluator) -> None:
    global _WORKER_EVALUATOR
    _WORKER_EVALUATOR = evaluator


def _evaluate_in_worker(graph: Graph) -> float:
    if _WORKER_EVALUATOR is None:
        raise RuntimeError("Parallel evaluation worker was not initialized")
    return float(_WORKER_EVALUATOR.evaluate(graph))


@dataclass(frozen=True, slots=True)
class EvolutionConfig:
    population_size: int = 20
    elite_size: int = 1
    tournament_size: int = 3
    mutation_rate: float = 1.0
    crossover_rate: float = 0.5
    topology_protection_generations: int = 0
    topology_protection_size: int = 0
    topology_parent_rate: float = 0.0
    island_count: int = 1
    migration_interval: int = 0
    migration_size: int = 0

    def __post_init__(self) -> None:
        if self.population_size < 1:
            raise ValueError("population_size must be positive")
        if self.island_count < 1:
            raise ValueError("island_count must be positive")
        if self.population_size % self.island_count:
            raise ValueError("population_size must be divisible by island_count")
        island_size = self.population_size // self.island_count
        if not 0 <= self.elite_size <= island_size:
            raise ValueError("elite_size must fit within the population")
        if not 1 <= self.tournament_size <= island_size:
            raise ValueError("tournament_size must fit within the population")
        if not 0.0 <= self.mutation_rate <= 1.0:
            raise ValueError("mutation_rate must be between zero and one")
        if not 0.0 <= self.crossover_rate <= 1.0:
            raise ValueError("crossover_rate must be between zero and one")
        if self.topology_protection_generations < 0:
            raise ValueError("topology_protection_generations cannot be negative")
        if not 0 <= self.topology_protection_size <= island_size - self.elite_size:
            raise ValueError("topology_protection_size must fit beside the fitness elites")
        if not 0.0 <= self.topology_parent_rate <= 1.0:
            raise ValueError("topology_parent_rate must be between zero and one")
        enabled = self.topology_protection_generations > 0
        if enabled != (self.topology_protection_size > 0):
            raise ValueError("topology protection generations and size must be enabled together")
        if not enabled and self.topology_parent_rate != 0.0:
            raise ValueError("topology_parent_rate requires topology protection")
        if self.migration_interval < 0:
            raise ValueError("migration_interval cannot be negative")
        if self.migration_size < 0:
            raise ValueError("migration_size cannot be negative")
        migration_enabled = self.migration_interval > 0
        if migration_enabled != (self.migration_size > 0):
            raise ValueError("migration interval and size must be enabled together")
        if migration_enabled and self.island_count < 2:
            raise ValueError("migration requires at least two islands")
        if self.migration_size > island_size:
            raise ValueError("migration_size must fit within an island")


@dataclass(frozen=True, slots=True)
class Individual:
    graph: Graph
    score: float
    topology_age: int = 0

    def __post_init__(self) -> None:
        if self.topology_age < 0:
            raise ValueError("topology_age cannot be negative")


@dataclass(frozen=True, slots=True)
class EvolutionResult:
    population: tuple[Individual, ...]
    generations: int
    islands: tuple[tuple[Individual, ...], ...] = ()

    @property
    def best(self) -> Individual:
        return self.population[0]


@dataclass(frozen=True, slots=True)
class EvolutionCheckpoint:
    population: tuple[Individual, ...]
    generation: int
    random_state: tuple[object, ...]
    island_sizes: tuple[int, ...] = ()

    @property
    def best(self) -> Individual:
        return min(self.population, key=lambda individual: individual.score)


class TournamentSelection:
    def __init__(self, size: int) -> None:
        self.size = size

    def select(self, population: Sequence[Individual], rng: Random) -> Individual:
        contenders = rng.sample(list(population), self.size)
        return min(contenders, key=lambda individual: individual.score)


class EvolutionEngine:
    def __init__(
        self,
        evaluator: Evaluator,
        policy: GraphPolicy,
        mutations: Sequence[GraphMutation],
        crossover: GraphCrossover | None = None,
        config: EvolutionConfig | None = None,
        seed: int | None = None,
        refiner: ParameterRefiner | None = None,
        simulation_workers: int = 1,
        island_policies: Sequence[GraphPolicy] | None = None,
    ) -> None:
        if simulation_workers < 1:
            raise ValueError("simulation_workers must be positive")
        self.evaluator = evaluator
        self.policy = policy
        self.mutations = tuple(mutations)
        self.crossover = crossover
        self.config = config or EvolutionConfig()
        self.rng = Random(seed)
        self.refiner = refiner
        self.simulation_workers = simulation_workers
        if island_policies is None:
            self.island_policies = (policy,) * self.config.island_count
        else:
            self.island_policies = tuple(island_policies)
            if len(self.island_policies) != self.config.island_count:
                raise ValueError("island_policies must contain one policy per island")
        self._evaluation_executor: ProcessPoolExecutor | None = None
        self.selection = TournamentSelection(self.config.tournament_size)

    def __enter__(self) -> EvolutionEngine:
        return self

    def __exit__(self, *exc_info: object) -> None:
        self.close()

    def close(self) -> None:
        """Release worker processes created for parallel simulation."""
        if self._evaluation_executor is not None:
            self._evaluation_executor.shutdown(wait=True, cancel_futures=False)
            self._evaluation_executor = None

    def run(self, initial_population: Iterable[Graph], generations: int) -> EvolutionResult:
        if generations < 0:
            raise ValueError("generations cannot be negative")
        graphs = [graph.copy() for graph in initial_population]
        if len(graphs) != self.config.population_size:
            raise ValueError(
                f"Expected {self.config.population_size} initial graphs, received {len(graphs)}"
            )
        island_size = self._island_size
        islands = []
        for index, start in enumerate(range(0, len(graphs), island_size)):
            island_graphs = graphs[start : start + island_size]
            for graph in island_graphs:
                self.island_policies[index].validate(graph)
            islands.append(self._evaluate(island_graphs))
        islands = self._advance(islands, 0, generations)
        return self._result(islands, generations)

    def step(self, population: Sequence[Individual]) -> list[Individual]:
        """Advance a single-island population by one generation.

        Multi-island runs are advanced through :meth:`run` and :meth:`resume`,
        which retain the island boundaries needed for migration.
        """
        if self.config.island_count != 1:
            raise ValueError("step() is only available for single-island configurations")
        return self._step_island(population, self.island_policies[0])

    def _step_island(
        self, population: Sequence[Individual], policy: GraphPolicy
    ) -> list[Individual]:
        if len(population) != self._island_size:
            raise ValueError("Population size does not match evolution configuration")
        ranked = sorted(population, key=lambda individual: individual.score)
        next_entries = [
            (individual.graph.copy(), individual.topology_age + 1)
            for individual in ranked[: self.config.elite_size]
        ]
        elite_signatures = {
            _topology_signature(individual.graph)
            for individual in ranked[: self.config.elite_size]
        }
        protected = self._protected(ranked, elite_signatures)
        next_entries.extend(
            (individual.graph.copy(), individual.topology_age + 1)
            for individual in protected
        )

        while len(next_entries) < self._island_size:
            parent = self._select_parent(ranked)
            child = parent.graph.copy()
            parent_signature = _topology_signature(parent.graph)
            try:
                if self.crossover is not None and self.rng.random() < self.config.crossover_rate:
                    other = self._select_parent(ranked)
                    child = self.crossover.cross(parent.graph, other.graph, policy, self.rng)
                if self.mutations and self.rng.random() < self.config.mutation_rate:
                    child = self.rng.choice(self.mutations).mutate(child, policy, self.rng)
                policy.validate(child)
                if self.refiner is not None:
                    # Judge the new shape on what it can do once tuned, not on
                    # the parameters it happened to be born with.
                    child = self.refiner.refine(
                        child, self.evaluator, policy, self.rng
                    ).graph
            except (MutationError, CrossoverError, GraphError, ValueError):
                child = parent.graph.copy()
            child_age = (
                parent.topology_age + 1
                if _topology_signature(child) == parent_signature
                else 0
            )
            next_entries.append((child, child_age))
        return self._evaluate_entries(next_entries)

    def checkpoint(self, result: EvolutionResult) -> EvolutionCheckpoint:
        islands = result.islands or (result.population,)
        return EvolutionCheckpoint(
            tuple(
                Individual(item.graph.copy(), item.score, item.topology_age)
                for island in islands
                for item in island
            ),
            result.generations,
            self.rng.getstate(),
            tuple(len(island) for island in islands),
        )

    def resume(self, checkpoint: EvolutionCheckpoint, generations: int) -> EvolutionResult:
        if generations < 0:
            raise ValueError("generations cannot be negative")
        if len(checkpoint.population) != self.config.population_size:
            raise ValueError("Checkpoint population size does not match evolution configuration")
        expected_sizes = (self._island_size,) * self.config.island_count
        island_sizes = checkpoint.island_sizes or (len(checkpoint.population),)
        if island_sizes != expected_sizes:
            raise ValueError("Checkpoint island layout does not match evolution configuration")
        self.rng.setstate(checkpoint.random_state)
        population = [
            Individual(item.graph.copy(), float(item.score), item.topology_age)
            for item in checkpoint.population
        ]
        islands: list[list[Individual]] = []
        offset = 0
        for index, size in enumerate(island_sizes):
            island = population[offset : offset + size]
            for individual in island:
                self.island_policies[index].validate(individual.graph)
            islands.append(island)
            offset += size
        islands = self._advance(islands, checkpoint.generation, generations)
        return self._result(islands, checkpoint.generation + generations)

    @property
    def _island_size(self) -> int:
        return self.config.population_size // self.config.island_count

    def _advance(
        self,
        islands: list[list[Individual]],
        generation: int,
        generations: int,
    ) -> list[list[Individual]]:
        for current in range(generation + 1, generation + generations + 1):
            islands = [
                self._step_island(island, self.island_policies[index])
                for index, island in enumerate(islands)
            ]
            if (
                self.config.migration_interval
                and current % self.config.migration_interval == 0
            ):
                islands = self._migrate(islands)
        return islands

    def _migrate(self, islands: Sequence[Sequence[Individual]]) -> list[list[Individual]]:
        """Copy elites around a ring using a simultaneous migration snapshot."""
        count = self.config.migration_size
        migrants = [
            [self._copy_individual(item) for item in island[:count]]
            for island in islands
        ]
        migrated: list[list[Individual]] = []
        for index, island in enumerate(islands):
            incoming = migrants[(index - 1) % len(islands)]
            accepted = []
            for migrant in incoming:
                try:
                    self.island_policies[index].validate(migrant.graph)
                except (GraphError, ValueError):
                    continue
                accepted.append(migrant)
            residents = [
                self._copy_individual(item) for item in island[: len(island) - len(accepted)]
            ]
            migrated.append(sorted(residents + accepted, key=lambda item: item.score))
        return migrated

    def _result(
        self, islands: Sequence[Sequence[Individual]], generations: int
    ) -> EvolutionResult:
        frozen = tuple(tuple(island) for island in islands)
        population = tuple(sorted(
            (item for island in frozen for item in island),
            key=lambda item: item.score,
        ))
        return EvolutionResult(population, generations, frozen)

    @staticmethod
    def _copy_individual(individual: Individual) -> Individual:
        return Individual(
            individual.graph.copy(), individual.score, individual.topology_age
        )

    def _evaluate(self, graphs: Sequence[Graph]) -> list[Individual]:
        return self._evaluate_entries([(graph, 0) for graph in graphs])

    def _evaluate_entries(self, entries: Sequence[tuple[Graph, int]]) -> list[Individual]:
        graphs = [graph for graph, _ in entries]
        if self.simulation_workers == 1:
            scores = [float(self.evaluator.evaluate(graph)) for graph in graphs]
        else:
            if self._evaluation_executor is None:
                self._evaluation_executor = ProcessPoolExecutor(
                    max_workers=self.simulation_workers,
                    initializer=_initialize_evaluation_worker,
                    initargs=(self.evaluator,),
                )
            scores = list(self._evaluation_executor.map(_evaluate_in_worker, graphs))

        population = []
        for (graph, topology_age), score in zip(entries, scores):
            if math.isnan(score):
                score = math.inf
            population.append(Individual(graph, score, topology_age))
        return sorted(population, key=lambda individual: individual.score)

    def _protected(
        self,
        ranked: Sequence[Individual],
        excluded: set[tuple[object, ...]],
    ) -> list[Individual]:
        if self.config.topology_protection_size == 0:
            return []
        by_topology: dict[tuple[object, ...], Individual] = {}
        for individual in ranked:
            if individual.topology_age >= self.config.topology_protection_generations:
                continue
            signature = _topology_signature(individual.graph)
            if signature in excluded or signature in by_topology:
                continue
            by_topology[signature] = individual
        candidates = sorted(
            by_topology.values(),
            # Finish an admitted topology's grace window before admitting a
            # newborn over it. Otherwise every age-zero mutation can evict the
            # very lineage this mechanism is meant to protect.
            key=lambda individual: (-individual.topology_age, individual.score),
        )
        return candidates[: self.config.topology_protection_size]

    def _select_parent(self, population: Sequence[Individual]) -> Individual:
        if self.config.topology_parent_rate == 0.0:
            return self.selection.select(population, self.rng)
        young = [
            individual
            for individual in population
            if individual.topology_age < self.config.topology_protection_generations
        ]
        if young and self.rng.random() < self.config.topology_parent_rate:
            return self.rng.choice(young)
        return self.selection.select(population, self.rng)


def _topology_signature(graph: Graph) -> tuple[object, ...]:
    """Parameter-independent identity used only to age structural lineages."""
    nodes = tuple(
        (node.id, node.kind, node.locked)
        for node in sorted(graph.nodes.values(), key=lambda item: item.id)
    )
    edges = tuple(
        (
            edge.id,
            edge.source,
            edge.target,
            edge.source_locked,
            edge.target_locked,
        )
        for edge in sorted(graph.edges.values(), key=lambda item: item.id)
    )
    return nodes, edges
