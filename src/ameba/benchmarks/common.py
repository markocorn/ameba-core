"""Shared machinery for identification benchmarks.

Every benchmark here uses the same setup: a plant is driven by a control
sequence u(k) and the model sees only u(k). It may have any number of input and
output channels; the single-input benchmarks are simply the one-channel case.
The model must carry its own state through delay nodes, so its prediction error
feeds back into the next step.

This is deliberately the only setup offered. The alternative — supplying
measured plant history as extra model inputs — turns identification into
fitting a static map, and a model scored that way never has to represent the
system's dynamics at all.
"""

from __future__ import annotations

from dataclasses import dataclass
from math import cos, isfinite, pi, sin
from random import Random
from typing import Callable, Sequence

from ameba_graph import (
    Edge,
    EvolutionConfig,
    EvolutionEngine,
    GenerationConfig,
    Graph,
    GraphCrossover,
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
    ReplaceEdge,
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


@dataclass(frozen=True, slots=True)
class MultiChannelTrajectory:
    """One simulated plant run with several inputs and several outputs.

    ``controls[k]`` is u(k) and ``responses[k]`` is y(k+1), each a tuple holding
    one entry per channel. This is the multi-channel counterpart of
    :class:`PlantTrajectory`, which stays scalar so the single-input benchmarks
    and the floors they were measured against are left untouched.

    Rows are validated here rather than left to :class:`Dataset` because the
    divergence guard and the report's channel summaries both read a trajectory
    before any dataset exists, and a plant that produced a ragged or non-finite
    run should say so in its own terms.
    """

    controls: tuple[tuple[float, ...], ...]
    responses: tuple[tuple[float, ...], ...]

    def __post_init__(self) -> None:
        if not self.controls or not self.responses:
            raise ValueError("A trajectory needs at least one sample")
        if len(self.controls) != len(self.responses):
            raise ValueError(
                "controls and responses must have matched row counts, got "
                f"{len(self.controls)} and {len(self.responses)}"
            )
        if not self.controls[0] or not self.responses[0]:
            raise ValueError("Every row needs at least one channel")

        inputs = len(self.controls[0])
        outputs = len(self.responses[0])
        if any(len(row) != inputs for row in self.controls):
            raise ValueError("Every control row must have the same channel count")
        if any(len(row) != outputs for row in self.responses):
            raise ValueError("Every response row must have the same channel count")
        if any(not isfinite(value) for row in self.controls for value in row):
            raise ValueError("Control values must be finite")
        if any(not isfinite(value) for row in self.responses for value in row):
            raise ValueError("Response values must be finite")

    @property
    def inputs(self) -> int:
        """How many control channels the plant is driven by."""
        return len(self.controls[0])

    @property
    def outputs(self) -> int:
        """How many measured channels the plant reports."""
        return len(self.responses[0])


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


def multisine_controls(
    count: int, low: float, high: float, rng: Random, amplitude: float = 1.0
) -> tuple[float, ...]:
    """A random-phase multisine confined to one frequency band.

    Every DFT frequency of a ``count``-sample record between ``low`` and
    ``high`` (cycles per sample, Nyquist 0.5) gets a unit cosine with a random
    phase, and the sum is scaled so its peak is ``amplitude``. Confining the
    excitation to a band is what lets an error be attributed to a part of the
    spectrum; scaling by the peak rather than the RMS keeps every band inside
    the amplitude range a model was trained on, which matters for a nonlinear
    plant, where amplitude and frequency would otherwise be confounded.
    """
    if not 0.0 <= low < high <= 0.5:
        raise ValueError("band must satisfy 0 <= low < high <= 0.5")
    first = max(1, int(-(-low * count // 1)))
    last = min(int(high * count), (count - 1) // 2)
    bins = list(range(first, last + 1)) or [max(1, round((low + high) / 2 * count))]
    phases = [rng.uniform(0.0, 2.0 * pi) for _ in bins]
    values = [sum(cos(2.0 * pi * k * n / count + phase) for k, phase in zip(bins, phases))
              for n in range(count)]
    peak = max(abs(value) for value in values) or 1.0
    return tuple(amplitude * value / peak for value in values)


def step_controls(count: int, hold: int = 10, amplitude: float = 1.0) -> tuple[float, ...]:
    """A piecewise-constant input that exposes settling behaviour."""
    return tuple(
        amplitude if (step // hold) % 2 == 0 else -amplitude for step in range(count)
    )


def channel_training_controls(
    count: int,
    channels: int,
    rng: Random,
    amplitude: float = 1.0,
    hold: int = 1,
) -> tuple[tuple[float, ...], ...]:
    """Uniform excitation per channel, optionally held for several samples.

    A ``hold`` of one is the white noise the single-input benchmarks use. A
    plant whose slowest mode is far slower than the sample time needs its input
    to persist instead: sample-to-sample noise is filtered out almost entirely
    by such a plant, so the data would carry no information about the dynamics
    worth identifying.
    """
    if channels < 1:
        raise ValueError("channels must be at least one")
    if hold < 1:
        raise ValueError("hold must be at least one sample")
    rows: list[tuple[float, ...]] = []
    current = tuple(0.0 for _ in range(channels))
    for step in range(count):
        if step % hold == 0:
            current = tuple(
                rng.uniform(-amplitude, amplitude) for _ in range(channels)
            )
        rows.append(current)
    return tuple(rows)


def channel_evaluation_controls(
    count: int, channels: int, amplitude: float = 1.0, period: int = 250
) -> tuple[tuple[float, ...], ...]:
    """A slow sinusoid per channel, each at its own period.

    Distinct periods are the point rather than a detail. Channels driven by the
    same trajectory cannot separate a plant's cross-coupling from its direct
    paths, so a shared input would hide exactly the structure a MIMO benchmark
    exists to expose.
    """
    if channels < 1:
        raise ValueError("channels must be at least one")
    if period < 1:
        raise ValueError("period must be at least one")
    return tuple(
        tuple(
            amplitude * sin(2.0 * pi * step / (period * (channel + 1)))
            for channel in range(channels)
        )
        for step in range(count)
    )


def channel_step_controls(
    count: int, channels: int, hold: int = 10, amplitude: float = 1.0
) -> tuple[tuple[float, ...], ...]:
    """A piecewise-constant input per channel, each switching at its own rate.

    The channels switch on different schedules, measured in multiples of
    ``hold``, so a model cannot fit one channel's response to another's.
    """
    if channels < 1:
        raise ValueError("channels must be at least one")
    if hold < 1:
        raise ValueError("hold must be at least one sample")
    return tuple(
        tuple(
            amplitude
            if ((step // (hold * (channel + 1))) + channel) % 2 == 0
            else -amplitude
            for channel in range(channels)
        )
        for step in range(count)
    )


def identification_dataset(trajectory: PlantTrajectory) -> Dataset:
    """Score a model driven only by u(k), with its own state fed back."""
    return Dataset(
        tuple((control,) for control in trajectory.controls),
        tuple((response,) for response in trajectory.responses),
    )


def multi_channel_dataset(trajectory: MultiChannelTrajectory) -> Dataset:
    """Score a multi-channel model driven only by u(k), per channel.

    Rows pass through untouched: the simulator reads each input by its channel
    index and the evaluator sums the criterion over every output, so a wider
    interface needs no adaptation here.
    """
    return Dataset(trajectory.controls, trajectory.responses)


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


def memoryless_floor(
    dataset: Dataset, criterion: str = "ise", time_step: float = 1.0
) -> float:
    """The best score attainable without state, for any number of channels.

    The single-input form fits one gain to ``y(k+1) = c u(k)``. With several
    inputs and outputs the same idea is a static linear map: each output is
    fitted from every input at the current step by least squares, which is
    exactly optimal for ISE. Because the plants here are driven by independent
    zero-mean controls, the best predictor of y(k+1) from u(k) alone *is*
    linear, so this remains the boundary between fitting a static map and
    representing dynamics.

    Reduces to :func:`static_gain_floor` when there is one input and one
    output; a MIMO score is only evidence of dynamics when it falls below this.

    Both conditions that make the linear fit optimal are assumed rather than
    checked, so a caller must confirm they hold for its plant. See
    :func:`multi_channel_floor` for what happens when they do not.
    """
    rows = len(dataset.inputs)
    if rows == 0:
        raise ValueError("Cannot fit a static map to an empty dataset")
    width = len(dataset.inputs[0])
    contribution = CRITERIA[criterion]

    # Normal equations, one shared Gram matrix for every output channel.
    gram = [
        [
            sum(row[i] * row[j] for row in dataset.inputs)
            for j in range(width)
        ]
        for i in range(width)
    ]
    total = 0.0
    for channel in range(len(dataset.outputs[0])):
        moment = [
            sum(row[i] * out[channel] for row, out in zip(dataset.inputs, dataset.outputs))
            for i in range(width)
        ]
        gains = _solve(gram, moment)
        total += sum(
            contribution(
                out[channel] - sum(g * value for g, value in zip(gains, row)),
                step * time_step,
            )
            for step, (row, out) in enumerate(zip(dataset.inputs, dataset.outputs))
        )
    return total * time_step


def multi_channel_floor(
    trajectory: MultiChannelTrajectory, criterion: str = "ise", time_step: float = 1.0
) -> float:
    """The best score attainable without state, for a multi-channel plant.

    The static map of :func:`memoryless_floor` is the same idea as the scalar
    gain, fitted from every input channel to every output channel, and it is
    exactly optimal for ISE.

    It is a weaker bar than the scalar floor, and measurably so. Least squares
    is the best *linear* memoryless predictor, and it is the best memoryless
    predictor outright only when the controls are independent *and* the plant's
    static relation is linear. A multi-channel rung that needs a held input for
    identifiability breaks the first condition, and a genuinely nonlinear plant
    breaks the second. On the quadruple tank a search restricted to stateless
    operators reaches **0.62** of this floor on the minimum-phase setting and
    **0.31** on the non-minimum-phase one, over seeds 1--3; the measurement
    lives in ``tests/test_mimo_benchmark.py``.

    On such a rung this number is a reference rather than a pass criterion:
    scoring below it does not by itself demonstrate that the model carries
    state. :func:`stateless_floor` is what replaces it, and the report scripts
    take the stricter of the two wherever :attr:`Benchmark.linear_floor_is_strict`
    is False.
    """
    return memoryless_floor(multi_channel_dataset(trajectory), criterion, time_step)


#: Either trajectory flavour. A scalar trajectory is the one-channel case.
Trajectory = PlantTrajectory | MultiChannelTrajectory


@dataclass(frozen=True, slots=True)
class Benchmark:
    """One rung of the ladder, in the terms a report needs.

    ``build`` turns a control sequence into a plant trajectory, so a
    multi-channel rung is declared exactly like a single-channel one and a
    caller reads the interface off the trajectory rather than branching on a
    benchmark's name. ``channels`` is the one thing that cannot be read off,
    because the controls have to be generated before any trajectory exists.

    ``reference_of`` is absent for the quadruple tank: no graph reproduces that
    plant yet, so there are no reference equations to compare against and
    nothing for a reference-graph figure to draw.

    ``linear_floor_is_strict`` records whether the closed-form memoryless floor
    is a valid pass criterion on this rung. False means a memoryless model has
    been *measured* to beat that floor by far more than estimator noise, which
    makes falling below it no evidence that state was used. It is a judgement
    backed by :func:`stateless_floor` rather than a computed property, because
    the measurement carries sampling error of its own: on the linear rung a
    memoryless search also undercuts the closed form, by 0.93x, and that is the
    search fitting the sample rather than the closed form having the wrong
    shape.
    """

    title: str
    build: Callable[..., Trajectory]
    reference_of: Callable[[], Graph] | None = None
    initialization: str = "seed"
    channels: int = 1
    time_step: float = 1.0
    linear_floor_is_strict: bool = True


def trajectory_dataset(trajectory: Trajectory) -> Dataset:
    """Build the scoring dataset for either trajectory flavour.

    Kept as a dispatch rather than a widened constructor so the single-input
    path is byte-for-byte what it always was.
    """
    if isinstance(trajectory, MultiChannelTrajectory):
        return multi_channel_dataset(trajectory)
    return identification_dataset(trajectory)


def trajectory_floor(
    trajectory: Trajectory, criterion: str = "ise", time_step: float = 1.0
) -> float:
    """The memoryless floor for either trajectory flavour.

    One input and one output reduce to :func:`static_gain_floor` exactly, so
    the benchmarks measured against it keep the same number.
    """
    if isinstance(trajectory, MultiChannelTrajectory):
        return multi_channel_floor(trajectory, criterion, time_step)
    return static_gain_floor(trajectory, criterion, time_step)


def trajectory_interface(trajectory: Trajectory) -> tuple[int, int]:
    """How many control and measured channels the trajectory carries."""
    if isinstance(trajectory, MultiChannelTrajectory):
        return trajectory.inputs, trajectory.outputs
    return 1, 1


def trajectory_rows(
    trajectory: Trajectory,
) -> tuple[tuple[tuple[float, ...], ...], tuple[tuple[float, ...], ...]]:
    """Control and response rows as nested tuples, whatever the flavour.

    A scalar trajectory reads as a one-channel one, so a channel-aware caller
    — the report's signal extraction and figures — needs no branch of its own.
    """
    if isinstance(trajectory, MultiChannelTrajectory):
        return trajectory.controls, trajectory.responses
    return (
        tuple((control,) for control in trajectory.controls),
        tuple((response,) for response in trajectory.responses),
    )


def _solve(matrix: Sequence[Sequence[float]], vector: Sequence[float]) -> list[float]:
    """Gaussian elimination with partial pivoting; the systems here are tiny.

    A singular system means an input carries no independent information, so the
    corresponding gain is simply left at zero rather than treated as an error.
    """
    size = len(vector)
    rows = [list(row) + [value] for row, value in zip(matrix, vector)]
    for column in range(size):
        pivot = max(range(column, size), key=lambda r: abs(rows[r][column]))
        if abs(rows[pivot][column]) < 1e-12:
            continue
        rows[column], rows[pivot] = rows[pivot], rows[column]
        for target in range(size):
            if target == column:
                continue
            factor = rows[target][column] / rows[column][column]
            for position in range(column, size + 1):
                rows[target][position] -= factor * rows[column][position]
    return [
        rows[index][size] / rows[index][index] if abs(rows[index][index]) > 1e-12 else 0.0
        for index in range(size)
    ]


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


def static_seed_graph(inputs: int = 1, outputs: int = 1) -> Graph:
    """The uninformed starting point for any interface: a static map.

    Every output is predicted from the inputs at the current step and nothing
    else, which is precisely the model :func:`memoryless_floor` measures. The
    search therefore begins on the boundary it has to beat, holding structure
    that can reach the floor exactly once its weights are fitted, and needing
    to discover state to go any further.

    An output node accepts a single input, so with more than one input each
    output is fed through a summing relay. The relay is unlocked, so mutation
    is free to rebuild what feeds it; only the interface terminals are locked.
    """
    graph = interface_scaffold(inputs, outputs)
    graph.add_node(Node("one", "constant", {"value": 1.0}))
    for channel in range(outputs):
        if inputs == 1:
            graph.add_edge(Edge(f"e_u0_y{channel}", "u0", f"y{channel}"))
            continue
        relay = f"s{channel}"
        graph.add_node(Node(relay, "add"))
        for index in range(inputs):
            graph.add_edge(Edge(f"e_u{index}_{relay}", f"u{index}", relay))
        graph.add_edge(Edge(f"e_{relay}_y{channel}", relay, f"y{channel}"))
    return graph


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
    """Operations that change what a graph is, not merely how it is tuned.

    ``ReplaceEdge`` belongs here despite leaving the shape untouched. The
    parameter set is reserved for moves that keep a candidate inside its
    parent's behavioural neighbourhood, which is what makes them local search
    for an archive; a replacement redraws the whole weight range, so it
    explores even though the topology comes out identical. It is the one
    operator that fits neither group, which is the shape/parameter boundary
    showing through.
    """
    return (
        SplitEdge(),
        AddNode(),
        AddEdge(),
        RemoveEdge(),
        MoveEdgeSource(),
        MoveEdgeTarget(),
        ReplaceEdge(),
        ReplaceNode(),
        RemoveNodeBypass(),
        RemoveNode(),
    )


def tuner_mutations() -> tuple[GraphMutation, ...]:
    """The operations refinement tunes with.

    Refinement is parameter mutation run as local search, so this is the set an
    exclusion has to reach for "no parameter mutation" to mean what it says.
    """
    return (MutateEdgeAttributes(), MutateNodeAttributes())


def tuner_names() -> tuple[str, ...]:
    """The tuner's operators by class name, for reporting and exclusions."""
    return tuple(type(operator).__name__ for operator in tuner_mutations())


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


def mutation_names() -> tuple[str, ...]:
    """Every mutation a search may be told to leave out, by class name.

    Derived from the canonical sets rather than written out again, so the list
    cannot drift away from what the portfolios actually contain.
    """
    return tuple(
        type(operator).__name__
        for operator in parameter_mutations() + structural_mutations()
    )


def crossover_names() -> tuple[str, ...]:
    """Every crossover a search may be told to leave out, by class name."""
    return tuple(
        type(operator).__name__ for operator in benchmark_crossover().crossovers
    )


def leave_out(
    operators: Sequence[GraphMutation] | Sequence[GraphCrossover],
    excluded: Sequence[str],
) -> tuple:
    """Drop operators by class name.

    Naming is the whole interface. An operator is excluded by its class name,
    which keeps the filter meaningful when the portfolios are regrouped -- the
    question "is this a parameter move or a structural one" stops being
    something a run has to answer -- and it is the same name the command line
    takes.

    A name absent from one particular list is a no-op rather than an error, so
    a single exclusion list can govern the mutation portfolio and the tuner
    together: excluding ``MutateEdgeAttributes`` has to remove it from both, or
    the operator keeps running inside refinement and the exclusion is a lie.
    Whether a name is *known* at all is settled once, before any of this, by
    ``_require_known``.
    """
    dropped = set(excluded)
    return tuple(
        operator for operator in operators if type(operator).__name__ not in dropped
    )


def _require_known(names: Sequence[str], known: Sequence[str], what: str) -> None:
    """Reject an exclusion name nothing answers to.

    A typo that silently excludes nothing is worse than one that stops the run,
    because the experiment still produces a number and the number looks fine.
    """
    unknown = sorted({name for name in names if name not in set(known)})
    if unknown:
        raise ValueError(
            f"Unknown {what}: {', '.join(unknown)}; known {what}: "
            + ", ".join(sorted(known))
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
    excluded_mutations: Sequence[str] = (),
    excluded_crossovers: Sequence[str] = (),
) -> EvolutionEngine:
    """The full default operator portfolio, wired for benchmark runs.

    ``excluded_mutations`` and ``excluded_crossovers`` name reproduction
    operators to leave out, so a run can pose a narrower question without a
    second definition of the portfolio. Excluding ``MutateNodeAttributes`` and
    ``MutateEdgeAttributes`` is the way to ask whether structure alone can write
    the parameters: it removes the tuner's operators as well, which is why the
    filter is applied to both.
    """
    _require_known(excluded_mutations, mutation_names(), "mutation")
    _require_known(excluded_crossovers, crossover_names(), "crossover")
    mutations = leave_out(
        parameter_mutations() + structural_mutations(), excluded_mutations
    )
    if not mutations:
        raise ValueError("At least one mutation must remain to reproduce anything")
    crossovers = leave_out(benchmark_crossover().crossovers, excluded_crossovers)
    # Refinement is parameter mutation run as local search, so an exclusion that
    # removes its operators leaves nothing for it to do.
    tuners = leave_out(tuner_mutations(), excluded_mutations)
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
        mutations=[MutationPortfolio(list(mutations))],
        crossover=(
            CrossoverPortfolio(list(crossovers)) if crossovers else None
        ),
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
            ParameterRefiner(list(tuners), refine)
            if refine is not None and tuners
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


#: The budget :func:`stateless_floor` uses when a caller does not say otherwise:
#: small enough to sit beside a real run rather than dwarf it. A caller wanting
#: the baseline to match its own search should pass its own budget instead.
STATELESS_BASELINE_POPULATION = 12
STATELESS_BASELINE_GENERATIONS = 20

#: How many independent seeds the baseline searches before keeping the best.
STATELESS_BASELINE_ROUNDS = 3


def stateless_operators(kinds: Sequence[str] = FULL_OPERATORS) -> tuple[str, ...]:
    """The kinds in ``kinds`` that carry nothing between one sample and the next.

    Whatever score a model built from these alone reaches, it reached without
    remembering anything — which is exactly the bar a memoryless floor exists
    to draw.
    """
    return tuple(kind for kind in kinds if kind not in STATEFUL_KINDS)


def stateless_floor(
    dataset: Dataset,
    criterion: str = "ise",
    time_step: float = 1.0,
    population_size: int = STATELESS_BASELINE_POPULATION,
    generations: int = STATELESS_BASELINE_GENERATIONS,
    seed: int = 1,
    rounds: int = STATELESS_BASELINE_ROUNDS,
) -> float:
    """The best score a model with no state at all can reach, found by search.

    :func:`memoryless_floor` fits the best *linear* static map, which is the
    best memoryless predictor only when the controls are independent and the
    plant's static relation is linear. Where either condition fails, that bar
    is beaten easily by a memoryless model: on the quadruple tank, which breaks
    both, this search reaches 0.31 to 0.62 of the linear floor. A model scoring
    below the linear floor there has therefore demonstrated nothing about
    carrying state.

    This establishes the bar by search instead. Evolution is restricted to
    :func:`stateless_operators`, and the best score it reaches *is* the floor.
    Nothing is assumed about the plant, and the engine, dataset, criterion, and
    time step are the ones the judged run used, so the two are comparable.

    Three properties are worth stating plainly.

    * The result is a *lower bound* on what memoryless models can reach, so it
      is a conservative bar: beating it is evidence of state, and failing it
      proves nothing.
    * It moves with the budget and the seed. ``rounds`` independent seeds are
      searched and the best kept, which raises the bar and so errs toward *not*
      claiming dynamics. A caller comparing against this must say which budget
      it used.
    * The node penalty is zero throughout, because the baseline is an accuracy
      figure and a size penalty would stop it being comparable with the
      unpenalised error the verdict is taken from.

    Measured with the defaults below on the registered rungs:

    ===================  ===========  ==========  =====
    benchmark            closed form  baseline    ratio
    ===================  ===========  ==========  =====
    ``linear``                9.1994      8.5724   0.93
    ``narendra``              1.2603      1.2621   1.00
    ``tank-minphase``       132.4007     81.8809   0.62
    ``tank-nonminphase``    117.3226     36.8379   0.31
    ===================  ===========  ==========  =====

    Both numbers are in-sample on the same data, so this compares two
    estimators rather than measuring either against a held-out truth. That is
    what the small gaps mean: least squares is optimal among *linear* maps, but
    a flexible search can still fit the sample better, so a few percent below
    the closed form on an independent white-noise input is the search
    overfitting rather than the closed form having the wrong shape. The tank
    gaps are an order of magnitude larger, which is why
    :attr:`Benchmark.linear_floor_is_strict` is False there and True on the two
    single-input rungs.
    """
    if population_size < 3:
        raise ValueError("A stateless baseline needs at least three candidates")
    if generations < 1:
        raise ValueError("generations must be positive")
    if rounds < 1:
        raise ValueError("rounds must be positive")
    interface = (len(dataset.inputs[0]), len(dataset.outputs[0]))
    policy = benchmark_policy(stateless_operators())
    best = float("inf")
    for index in range(rounds):
        engine = benchmark_engine(
            dataset,
            seed=seed + index,
            population_size=population_size,
            policy=policy,
            criterion=criterion,
            time_step=time_step,
            node_penalty=0.0,
        )
        try:
            population = [
                static_seed_graph(*interface) for _ in range(population_size)
            ]
            result = engine.run(population, generations)
        finally:
            engine.close()
        best = min(best, min(item.score for item in result.population))
    return best
