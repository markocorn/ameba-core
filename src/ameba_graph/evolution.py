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
from .refinement import ParameterRefiner, Refinement
from .evaluation import OscillatingParsimony

_WORKER_EVALUATORS: tuple[Evaluator, ...] = ()
_WORKER_REFINER: ParameterRefiner | None = None
_WORKER_POLICIES: tuple[GraphPolicy, ...] = ()


def _initialize_evaluation_worker(
    evaluators: tuple[Evaluator, ...],
    refiner: ParameterRefiner | None,
    policies: tuple[GraphPolicy, ...],
) -> None:
    global _WORKER_EVALUATORS, _WORKER_REFINER, _WORKER_POLICIES
    _WORKER_EVALUATORS = evaluators
    _WORKER_REFINER = refiner
    _WORKER_POLICIES = policies


def _evaluate_in_worker(job: tuple[Graph, int]) -> float:
    if not _WORKER_EVALUATORS:
        raise RuntimeError("Parallel evaluation worker was not initialized")
    graph, evaluator_index = job
    return float(_WORKER_EVALUATORS[evaluator_index].evaluate(graph))


def _refine_in_worker(job: tuple[Graph, int, int]) -> Refinement | None:
    if not _WORKER_EVALUATORS or _WORKER_REFINER is None:
        raise RuntimeError("Parallel refinement worker was not initialized")
    graph, policy_index, seed = job
    try:
        return _WORKER_REFINER.refine(
            graph,
            _WORKER_EVALUATORS[policy_index],
            _WORKER_POLICIES[policy_index],
            Random(seed),
        )
    except (GraphError, ValueError):
        return None


@dataclass(slots=True)
class _Candidate:
    graph: Graph
    topology_age: int
    policy_index: int
    raw_score: float | None = None
    refinement_seed: int | None = None
    parent_signature: tuple[object, ...] | None = None
    parent_age: int = 0
    fallback: Individual | None = None
    discard_on_refinement_error: bool = False
    discarded: bool = False


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
    island_exchange: str = "migration"

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
        if self.island_exchange not in {"migration", "crossover"}:
            raise ValueError("island_exchange must be 'migration' or 'crossover'")


@dataclass(frozen=True, slots=True)
class Individual:
    graph: Graph
    score: float
    topology_age: int = 0
    raw_score: float | None = None

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
        island_evaluators: Sequence[Evaluator] | None = None,
        fitness_shaper: OscillatingParsimony | None = None,
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
        self.fitness_shaper = fitness_shaper
        self._evaluation_generation = 0
        if island_policies is None:
            self.island_policies = (policy,) * self.config.island_count
        else:
            self.island_policies = tuple(island_policies)
            if len(self.island_policies) != self.config.island_count:
                raise ValueError("island_policies must contain one policy per island")
        if island_evaluators is None:
            self.island_evaluators = (evaluator,) * self.config.island_count
        else:
            self.island_evaluators = tuple(island_evaluators)
            if len(self.island_evaluators) != self.config.island_count:
                raise ValueError("island_evaluators must contain one evaluator per island")
        self._evaluation_executor: ProcessPoolExecutor | None = None
        self.selection = TournamentSelection(self.config.tournament_size)
        self.last_exchange_stats: tuple[tuple[int, int], ...] = ()
        if (
            self.config.island_exchange == "crossover"
            and self.config.migration_interval
            and self.crossover is None
        ):
            raise ValueError("crossover island exchange requires a crossover operator")

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
        island_graphs: list[list[Graph]] = []
        for index, start in enumerate(range(0, len(graphs), island_size)):
            group = graphs[start : start + island_size]
            for graph in group:
                self.island_policies[index].validate(graph)
            island_graphs.append(group)
        raw_scores = self._evaluate_graphs(
            graphs,
            [
                island
                for island in range(self.config.island_count)
                for _ in range(island_size)
            ],
        )
        islands = []
        offset = 0
        for island_index, group in enumerate(island_graphs):
            entries = zip(group, raw_scores[offset : offset + len(group)])
            islands.append(sorted(
                (
                    self._individual(graph, 0, raw_score, island_index)
                    for graph, raw_score in entries
                ),
                key=lambda individual: individual.score,
            ))
            offset += len(group)
        islands = self._advance(islands, 0, generations)
        return self._result(islands, generations)

    def step(self, population: Sequence[Individual]) -> list[Individual]:
        """Advance a single-island population by one generation.

        Multi-island runs are advanced through :meth:`run` and :meth:`resume`,
        which retain the island boundaries needed for migration.
        """
        if self.config.island_count != 1:
            raise ValueError("step() is only available for single-island configurations")
        self._evaluation_generation += 1
        population = self._reshape(population, self._evaluation_generation, 0)
        candidates = self._prepare_island(population, 0)
        return self._finish_islands([candidates])[0]

    def advance(self, result: EvolutionResult, generations: int = 1) -> EvolutionResult:
        """Advance a result while retaining its island layout and migration phase."""
        if generations < 0:
            raise ValueError("generations cannot be negative")
        islands = result.islands or (result.population,)
        expected_sizes = (self._island_size,) * self.config.island_count
        if tuple(len(island) for island in islands) != expected_sizes:
            raise ValueError("Result island layout does not match evolution configuration")
        mutable = [list(island) for island in islands]
        advanced = self._advance(mutable, result.generations, generations)
        return self._result(advanced, result.generations + generations)

    def _prepare_island(
        self, population: Sequence[Individual], policy_index: int
    ) -> list[_Candidate]:
        if len(population) != self._island_size:
            raise ValueError("Population size does not match evolution configuration")
        policy = self.island_policies[policy_index]
        ranked = sorted(population, key=lambda individual: individual.score)
        next_entries = [
            _Candidate(
                individual.graph.copy(), individual.topology_age + 1, policy_index,
                individual.raw_score,
            )
            for individual in ranked[: self.config.elite_size]
        ]
        elite_signatures = {
            _topology_signature(individual.graph)
            for individual in ranked[: self.config.elite_size]
        }
        protected = self._protected(ranked, elite_signatures)
        next_entries.extend(
            _Candidate(
                individual.graph.copy(), individual.topology_age + 1, policy_index,
                individual.raw_score,
            )
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
                    next_entries.append(_Candidate(
                        child,
                        0,
                        policy_index,
                        refinement_seed=self.rng.getrandbits(64),
                        parent_signature=parent_signature,
                        parent_age=parent.topology_age,
                        fallback=parent,
                    ))
                    continue
            except (MutationError, CrossoverError, GraphError, ValueError):
                child = parent.graph.copy()
                next_entries.append(_Candidate(
                    child,
                    parent.topology_age + 1,
                    policy_index,
                    parent.raw_score,
                ))
                continue
            child_age = (
                parent.topology_age + 1
                if _topology_signature(child) == parent_signature
                else 0
            )
            next_entries.append(_Candidate(child, child_age, policy_index))
        return next_entries

    def checkpoint(self, result: EvolutionResult) -> EvolutionCheckpoint:
        islands = result.islands or (result.population,)
        return EvolutionCheckpoint(
            tuple(
                Individual(
                    item.graph.copy(), item.score, item.topology_age, item.raw_score
                )
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
            Individual(
                item.graph.copy(), float(item.score), item.topology_age, item.raw_score
            )
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
            self._evaluation_generation = current
            islands = [
                self._reshape(island, current, index)
                for index, island in enumerate(islands)
            ]
            prepared = [
                self._prepare_island(island, index)
                for index, island in enumerate(islands)
            ]
            islands = self._finish_islands(prepared)
            if (
                self.config.migration_interval
                and current % self.config.migration_interval == 0
            ):
                islands = self._exchange_islands(islands)
        return islands

    def _exchange_islands(
        self, islands: Sequence[Sequence[Individual]]
    ) -> list[list[Individual]]:
        if self.config.island_exchange == "crossover":
            return self._cross_islands(islands)
        return self._migrate(islands)

    def _cross_islands(
        self, islands: Sequence[Sequence[Individual]]
    ) -> list[list[Individual]]:
        """Cross local elites with predecessor elites without moving either parent."""
        assert self.crossover is not None
        count = self.config.migration_size
        prepared: list[list[_Candidate]] = []
        stats: list[tuple[int, int]] = []
        for index, island in enumerate(islands):
            donor_island = islands[(index - 1) % len(islands)]
            entries: list[_Candidate] = []
            for local, donor in zip(island[:count], donor_island[:count]):
                local_signature = _topology_signature(local.graph)
                try:
                    child = self.crossover.cross(
                        local.graph, donor.graph, self.island_policies[index], self.rng
                    )
                    self.island_policies[index].validate(child)
                except (CrossoverError, GraphError, ValueError):
                    continue
                age = (
                    local.topology_age + 1
                    if _topology_signature(child) == local_signature
                    else 0
                )
                entries.append(_Candidate(
                    child,
                    age,
                    index,
                    refinement_seed=(
                        self.rng.getrandbits(64) if self.refiner is not None else None
                    ),
                    parent_signature=local_signature,
                    parent_age=local.topology_age,
                    discard_on_refinement_error=True,
                ))
            prepared.append(entries)

        children_by_island = self._finish_islands(prepared)
        crossed: list[list[Individual]] = []
        for index, (island, children) in enumerate(zip(islands, children_by_island)):
            donor_island = islands[(index - 1) % len(islands)]
            stats.append((min(count, len(island), len(donor_island)), len(children)))
            residents = [
                self._copy_individual(item)
                for item in island[: len(island) - len(children)]
            ]
            crossed.append(sorted(residents + children, key=lambda item: item.score))
        self.last_exchange_stats = tuple(stats)
        return crossed

    def _migrate(self, islands: Sequence[Sequence[Individual]]) -> list[list[Individual]]:
        """Copy elites around a ring using a simultaneous migration snapshot."""
        count = self.config.migration_size
        migrants = [
            [self._copy_individual(item) for item in island[:count]]
            for island in islands
        ]
        prepared: list[list[_Candidate]] = []
        stats: list[tuple[int, int]] = []
        for index, island in enumerate(islands):
            incoming = migrants[(index - 1) % len(islands)]
            accepted = []
            for migrant in incoming:
                try:
                    self.island_policies[index].validate(migrant.graph)
                except (GraphError, ValueError):
                    continue
                accepted.append(migrant)
            stats.append((len(incoming), len(accepted)))
            residents = [
                self._copy_individual(item) for item in island[: len(island) - len(accepted)]
            ]
            prepared.append([
                *(
                    _Candidate(
                        resident.graph,
                        resident.topology_age,
                        index,
                        resident.raw_score,
                    )
                    for resident in residents
                ),
                *(
                    _Candidate(migrant.graph, migrant.topology_age, index)
                    for migrant in accepted
                ),
            ])
        self.last_exchange_stats = tuple(stats)
        return self._finish_islands(prepared)

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
            individual.graph.copy(), individual.score, individual.topology_age,
            individual.raw_score,
        )

    def _reshape(
        self, population: Sequence[Individual], generation: int, evaluator_index: int
    ) -> list[Individual]:
        if self.fitness_shaper is None:
            return list(population)
        reshaped = []
        for item in population:
            raw = item.raw_score
            if raw is None:
                raw = float(self.island_evaluators[evaluator_index].evaluate(item.graph))
            score = self.fitness_shaper.shape(item.graph, raw, generation)
            reshaped.append(Individual(item.graph, score, item.topology_age, raw))
        return sorted(reshaped, key=lambda item: item.score)

    def _executor(self) -> ProcessPoolExecutor:
        if self._evaluation_executor is None:
            self._evaluation_executor = ProcessPoolExecutor(
                max_workers=self.simulation_workers,
                initializer=_initialize_evaluation_worker,
                initargs=(self.island_evaluators, self.refiner, self.island_policies),
            )
        return self._evaluation_executor

    def _evaluate_graphs(
        self,
        graphs: Sequence[Graph],
        evaluator_indices: Sequence[int],
    ) -> list[float]:
        if len(graphs) != len(evaluator_indices):
            raise ValueError("Each graph must have an evaluator index")
        if self.simulation_workers == 1:
            return [
                float(self.island_evaluators[index].evaluate(graph))
                for graph, index in zip(graphs, evaluator_indices)
            ]
        return list(self._executor().map(
            _evaluate_in_worker, zip(graphs, evaluator_indices)
        ))

    def _refine_candidates(self, candidates: Sequence[_Candidate]) -> None:
        pending = [item for item in candidates if item.refinement_seed is not None]
        if not pending:
            return
        jobs = [
            (item.graph, item.policy_index, item.refinement_seed)
            for item in pending
        ]
        if self.simulation_workers == 1:
            assert self.refiner is not None
            results: Sequence[Refinement | None] = []
            serial_results = []
            for graph, policy_index, seed in jobs:
                try:
                    serial_results.append(self.refiner.refine(
                        graph,
                        self.island_evaluators[policy_index],
                        self.island_policies[policy_index],
                        Random(seed),
                    ))
                except (GraphError, ValueError):
                    serial_results.append(None)
            results = serial_results
        else:
            results = list(self._executor().map(_refine_in_worker, jobs))

        for item, result in zip(pending, results):
            if result is None:
                if item.discard_on_refinement_error:
                    item.discarded = True
                    continue
                assert item.fallback is not None
                item.graph = item.fallback.graph.copy()
                item.topology_age = item.fallback.topology_age + 1
                item.raw_score = item.fallback.raw_score
                continue
            item.graph = result.graph
            item.raw_score = result.score
            item.topology_age = (
                item.parent_age + 1
                if _topology_signature(item.graph) == item.parent_signature
                else 0
            )

    def _finish_islands(
        self, prepared: Sequence[Sequence[_Candidate]]
    ) -> list[list[Individual]]:
        candidates = [item for island in prepared for item in island]
        self._refine_candidates(candidates)
        unevaluated = [
            item for item in candidates
            if not item.discarded and item.raw_score is None
        ]
        if unevaluated:
            for item, raw_score in zip(
                unevaluated,
                self._evaluate_graphs(
                    [item.graph for item in unevaluated],
                    [item.policy_index for item in unevaluated],
                ),
            ):
                item.raw_score = raw_score

        islands: list[list[Individual]] = []
        for group in prepared:
            population = [
                self._individual(
                    item.graph, item.topology_age, item.raw_score, item.policy_index
                )
                for item in group
                if not item.discarded
            ]
            islands.append(sorted(population, key=lambda individual: individual.score))
        return islands

    def _individual(
        self,
        graph: Graph,
        topology_age: int,
        raw_score: float | None,
        evaluator_index: int,
    ) -> Individual:
        if raw_score is None:
            raw_score = float(self.island_evaluators[evaluator_index].evaluate(graph))
        if math.isnan(raw_score):
            raw_score = math.inf
        score = (
            self.fitness_shaper.shape(graph, raw_score, self._evaluation_generation)
            if self.fitness_shaper is not None
            else raw_score
        )
        return Individual(graph, score, topology_age, raw_score)

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
