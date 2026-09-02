"""Shared machinery for single-input/single-output identification benchmarks.

Every benchmark here uses the same setup: a plant is driven by a scalar control
sequence u(k) and the model sees only u(k). The model must carry its own state
through delay nodes, so its prediction error feeds back into the next step.

This is deliberately the only setup offered. The alternative — supplying
measured plant history as extra model inputs — turns identification into
fitting a static map, and a model scored that way never has to represent the
system's dynamics at all.
"""

from __future__ import annotations

from dataclasses import dataclass
from math import isfinite, pi, sin
from random import Random
from typing import Sequence

from ameba_graph import (
    Edge,
    EvolutionConfig,
    EvolutionEngine,
    GenerationConfig,
    Graph,
    GraphGenerator,
    GraphMutation,
    Node,
    ParameterRefiner,
    OscillatingParsimony,
    RefinementConfig,
    live_nodes,
)
from ameba_graph.crossover import (
    AlignedAttributeCrossover,
    CrossoverPortfolio,
    InducedSubgraphInsertionCrossover,
    TypedSubgraphReplacementCrossover,
    UniformGraphCrossover,
)
from ameba_graph.mutation import (
    AddEdge,
    AddNode,
    MoveEdgeSource,
    MoveEdgeTarget,
    MutateEdgeAttributes,
    MutateNodeAttributes,
    MutationPortfolio,
    RemoveEdge,
    RemoveNode,
    RemoveNodeBypass,
    ReplaceNode,
    SplitEdge,
)
from ameba_signal import (
    CRITERIA,
    Dataset,
    SignalEvaluator,
    SignalGraphPolicy,
    interface_scaffold,
)
from ameba_signal.operators import EVOLVABLE_OPERATORS, operators_in
from ameba_signal.stateful import STATEFUL_KINDS

CONTROL_INPUT = "u_k"
RESPONSE_OUTPUT = "y_next"

#: The minimal arithmetic set: sum, product, division, and one step of memory.
#: Division is the unary ``reciprocal``, so ``a/b`` is ``multiply(a, 1/b)``;
#: keeping every multi-input operator commutative matters because operator
#: arguments are ordered by edge identifier, which mutation is free to change.
BASIC_OPERATORS = ("add", "multiply", "reciprocal", "delay")

#: Linear dynamics only: no nonlinearity is reachable from this set.
LINEAR_OPERATORS = operators_in("arithmetic", "memory") + ("constant",)

#: Everything evolution may create. The default, so a benchmark is not quietly
#: handicapped; restrict deliberately when an experiment calls for it.
FULL_OPERATORS = EVOLVABLE_OPERATORS

#: Fraction of its score a node has to earn to justify its place. Relative, so
#: it stays below the local gradient at every scale instead of vetoing real
#: improvements once the score falls -- an absolute weight of 0.05 was doing
#: exactly that on the Narendra plant, where the available gain is under 0.1.
DEFAULT_NODE_PENALTY = 1e-3


@dataclass(frozen=True, slots=True)
class PlantTrajectory:
    """One simulated plant run.

    ``responses[k]`` is y(k+1), the value a model driven by ``controls[k]`` is
    asked to produce at step k.
    """

    controls: tuple[float, ...]
    responses: tuple[float, ...]


def training_controls(count: int, rng: Random, amplitude: float = 1.0) -> tuple[float, ...]:
    """Uniform excitation in [-amplitude, amplitude], the usual identification input.

    Amplitude is worth varying deliberately. A nonlinear plant driven weakly
    stays close to its linearization, so a small input can hide exactly the
    behaviour a benchmark is meant to expose.
    """
    return tuple(rng.uniform(-amplitude, amplitude) for _ in range(count))


def evaluation_controls(count: int, amplitude: float = 1.0, period: int = 250) -> tuple[float, ...]:
    """A slow sinusoid, used to check generalization away from the training input."""
    return tuple(amplitude * sin(2.0 * pi * step / period) for step in range(count))


def step_controls(count: int, hold: int = 10, amplitude: float = 1.0) -> tuple[float, ...]:
    """A piecewise-constant input that exposes settling behaviour."""
    return tuple(
        amplitude if (step // hold) % 2 == 0 else -amplitude for step in range(count)
    )


def identification_dataset(trajectory: PlantTrajectory) -> Dataset:
    """Score a model driven only by u(k), with its own state fed back."""
    return Dataset(
        tuple((control,) for control in trajectory.controls),
        tuple((response,) for response in trajectory.responses),
    )


def static_gain_floor(
    trajectory: PlantTrajectory, criterion: str = "ise", time_step: float = 1.0
) -> float:
    """The best score attainable by the memoryless model y(k+1) = c u(k).

    This is the benchmark's real pass criterion. The plants here are driven by
    an independent zero-mean control sequence, so no function of u(k) alone
    predicts y(k+1) better than the fitted gain. A model that scores below this
    floor is using state, which is the whole point of the exercise; a model
    that merely improves on its seed may only have found a better gain.

    The gain is always fitted by least squares, which is exactly optimal for
    ISE. Under the other criteria the floor is therefore a close reference
    rather than a strict bound.
    """
    controls, responses = trajectory.controls, trajectory.responses
    energy = sum(control * control for control in controls)
    if energy == 0.0:
        raise ValueError("Cannot fit a static gain to a zero control sequence")
    gain = sum(c * r for c, r in zip(controls, responses)) / energy

    contribution = CRITERIA[criterion]
    total = sum(
        contribution(response - gain * control, step * time_step)
        for step, (control, response) in enumerate(zip(controls, responses))
    )
    return total * time_step


def seed_graph() -> Graph:
    """The uninformed starting point: y(k+1) predicted as u(k).

    The control input and the response output are structure-locked because they
    are the model's external interface. An unconnected interface node is
    otherwise a free removal target, and no mutation can create an input or
    output node, so the search could permanently discard the terminals it is
    being scored on.
    """
    graph = Graph()
    graph.add_node(Node(CONTROL_INPUT, "input", {"index": 0}, locked=True))
    graph.add_node(Node("one", "constant", {"value": 1.0}))
    return add_output(graph, CONTROL_INPUT)


def dynamic_population(
    dataset: Dataset,
    policy: SignalGraphPolicy,
    population_size: int,
    seed: int,
    min_nodes: int = 10,
    max_nodes: int = 14,
    min_stateful: int = 0,
) -> list[Graph]:
    """Generate a deterministic, executable, state-bearing initial population.

    Identical copies of ``seed_graph()`` give crossover no material to combine
    and make every useful dynamic lineage climb away from a very competitive
    static model. This initializer starts search at several architectures while
    remaining equation-agnostic: it knows only the dataset interface. Callers
    may require live memory nodes, but leaving that at zero gives selection a
    spectrum from simple to strongly dynamic architectures; forcing complexity
    was empirically worse on the Narendra benchmark.
    """
    if population_size < 1:
        raise ValueError("population_size must be positive")
    if min_stateful < 0:
        raise ValueError("min_stateful cannot be negative")
    if not dataset.inputs or not dataset.outputs:
        raise ValueError("Dynamic population generation requires a non-empty dataset")
    if min_stateful and not set(policy.evolvable_kinds) & STATEFUL_KINDS:
        raise ValueError("Dynamic population generation requires a stateful operator")

    input_count = len(dataset.inputs[0])
    output_count = len(dataset.outputs[0])
    scaffold = interface_scaffold(input_count, output_count)
    generator = GraphGenerator(
        policy,
        GenerationConfig(
            min_nodes=min_nodes,
            max_nodes=max_nodes,
            edge_probability=0.18,
            attempts=500,
        ),
    )
    probe = Dataset(dataset.inputs[:30], dataset.outputs[:30])
    evaluator = SignalEvaluator(probe)
    rng = Random(seed)

    def acceptable(graph: Graph) -> bool:
        terminals = {node.id for node in graph.nodes.values() if node.kind == "output"}
        live = live_nodes(graph, terminals)
        stateful = sum(
            node.kind in STATEFUL_KINDS and node.id in live
            for node in graph.nodes.values()
        )
        return stateful >= min_stateful and isfinite(evaluator.evaluate(graph))

    return [
        generator.generate(rng, scaffold, accept=acceptable)
        for _ in range(population_size)
    ]


def add_output(graph: Graph, source: str) -> Graph:
    """Attach the locked response output to ``source``."""
    graph.add_node(Node(RESPONSE_OUTPUT, "output", {"index": 0}, locked=True))
    graph.add_edge(Edge(f"e_{source}_output", source, RESPONSE_OUTPUT))
    return graph


def add_delay_chain(graph: Graph, source: str, names: Sequence[str]) -> list[str]:
    """Chain unit delays from ``source``, returning one node per past step.

    ``names[0]`` holds the value ``source`` carried one step ago, ``names[1]``
    two steps ago, and so on. Every loop closed this way passes through a
    delay, so the graph stays free of algebraic cycles.
    """
    previous = source
    for name in names:
        graph.add_node(Node(name, "delay", {"steps": 1, "initial": 0.0}))
        graph.add_edge(Edge(f"e_{previous}_{name}", previous, name))
        previous = name
    return list(names)


def benchmark_policy(
    kinds: Sequence[str] = FULL_OPERATORS, **kwargs: object
) -> SignalGraphPolicy:
    """The signal policy used for benchmark runs.

    Defaults to every evolvable operator. ``kinds`` accepts group names as well
    as operator names, so a restricted experiment reads as
    ``benchmark_policy(["arithmetic", "memory"])``.
    """
    return SignalGraphPolicy(evolvable_kinds=operators_in(*kinds), **kwargs)  # type: ignore[arg-type]


def parameter_mutations() -> tuple[GraphMutation, ...]:
    """Operations that retune a graph without changing its shape.

    Kept separate from the structural set because an archive treats them as
    local search rather than exploration: a retuned copy stays in its parent's
    neighbourhood and competes only against it.
    """
    return (MutateNodeAttributes(), MutateEdgeAttributes())


def structural_mutations() -> tuple[GraphMutation, ...]:
    """Operations that change what a graph is, not merely how it is tuned."""
    return (
        SplitEdge(),
        AddNode(),
        AddEdge(),
        RemoveEdge(),
        MoveEdgeSource(),
        MoveEdgeTarget(),
        ReplaceNode(),
        RemoveNodeBypass(),
        RemoveNode(),
    )


def benchmark_crossover() -> CrossoverPortfolio:
    """The crossover portfolio used by every benchmark search."""
    return CrossoverPortfolio(
        [
            AlignedAttributeCrossover(),
            InducedSubgraphInsertionCrossover(),
            TypedSubgraphReplacementCrossover(),
            UniformGraphCrossover(),
        ]
    )


def benchmark_engine(
    dataset: Dataset,
    seed: int,
    population_size: int = 12,
    policy: SignalGraphPolicy | None = None,
    criterion: str = "ise",
    time_step: float = 1.0,
    node_penalty: float = DEFAULT_NODE_PENALTY,
    refine: RefinementConfig | None = None,
    topology_protection_generations: int = 0,
    topology_protection_size: int = 0,
    topology_parent_rate: float = 0.0,
    simulation_workers: int = 1,
    island_count: int = 1,
    migration_interval: int = 0,
    migration_size: int = 0,
    island_policies: Sequence[SignalGraphPolicy] | None = None,
    island_datasets: Sequence[Dataset] | None = None,
    island_score_scales: Sequence[float] | None = None,
    complexity_schedule: OscillatingParsimony | None = None,
    island_exchange: str = "migration",
) -> EvolutionEngine:
    """The full default operator portfolio, wired for benchmark runs."""
    if island_score_scales is not None and (
        island_datasets is None or len(island_score_scales) != len(island_datasets)
    ):
        raise ValueError("island_score_scales must match island_datasets")
    evaluator = SignalEvaluator(dataset, criterion=criterion, time_step=time_step)
    fitness_shaper = complexity_schedule or OscillatingParsimony(
        expansion_generations=1,
        compression_generations=1,
        expansion_node_weight=node_penalty,
        compression_node_weight=node_penalty,
    )
    return EvolutionEngine(
        evaluator=evaluator,
        policy=policy or benchmark_policy(),
        mutations=[MutationPortfolio(list(parameter_mutations() + structural_mutations()))],
        crossover=benchmark_crossover(),
        config=EvolutionConfig(
            population_size=population_size,
            elite_size=2,
            tournament_size=3,
            mutation_rate=1.0,
            crossover_rate=0.4,
            topology_protection_generations=topology_protection_generations,
            topology_protection_size=topology_protection_size,
            topology_parent_rate=topology_parent_rate,
            island_count=island_count,
            migration_interval=migration_interval,
            migration_size=migration_size,
            island_exchange=island_exchange,
        ),
        seed=seed,
        refiner=(
            ParameterRefiner(
                [MutateEdgeAttributes(), MutateNodeAttributes()], refine
            )
            if refine is not None
            else None
        ),
        simulation_workers=simulation_workers,
        island_policies=island_policies,
        island_evaluators=(
            tuple(
                SignalEvaluator(
                    item,
                    criterion=criterion,
                    time_step=time_step,
                    normalization=(
                        island_score_scales[index]
                        if island_score_scales is not None
                        else 1.0
                    ),
                )
                for index, item in enumerate(island_datasets)
            )
            if island_datasets is not None
            else None
        ),
        fitness_shaper=fitness_shaper,
    )
