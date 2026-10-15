"""Run an identification benchmark and report what evolution found.

    python scripts/benchmark_report.py                    # linear plant
    python scripts/benchmark_report.py --benchmark narendra
    python scripts/benchmark_report.py --benchmark tank-nonminphase --islands 4
    python scripts/benchmark_report.py --generations 300 --population 24

Prints a scored summary and the evolved model's difference equations, then
opens a figure with the signals, the model graph, and the equations. A
multi-channel benchmark reports one panel and one fit per output channel.
"""

from __future__ import annotations

import argparse
import sys
from collections import Counter
from math import isfinite, pi, sin
from pathlib import Path
from time import perf_counter

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from ameba.benchmarks import (  # noqa: E402
    BENCHMARKS,
    DEFAULT_NODE_PENALTY,
    FULL_OPERATORS,
    benchmark_engine,
    benchmark_policy,
    channel_evaluation_controls,
    channel_step_controls,
    channel_training_controls,
    crossover_names,
    dynamic_population,
    evaluation_controls,
    mutation_names,
    seed_graph,
    static_seed_graph,
    stateless_floor,
    step_controls,
    training_controls,
    trajectory_dataset,
    trajectory_floor,
    trajectory_interface,
    trajectory_rows,
    tuner_names,
)
from ameba.equations import format_equations, graph_to_equations  # noqa: E402
from ameba.plotting import build_layout, draw_graph  # noqa: E402
from ameba_graph import Graph, OscillatingParsimony, RefinementConfig  # noqa: E402
from ameba_signal import (  # noqa: E402
    CRITERIA,
    PARAMETER_ALPHABETS,
    SignalEvaluator,
    SignalGraphPolicy,
    SignalSimulationError,
    SignalSimulator,
)
from ameba_signal.stateful import CYCLE_BREAKER_KINDS, STATEFUL_KINDS  # noqa: E402
from ameba_signal.operators import OPERATOR_GROUPS  # noqa: E402

from random import Random  # noqa: E402

KIND_COLORS = {
    "input": "#059669",
    "output": "#dc2626",
    "constant": "#6b7280",
    "delay": "#d97706",
}
STATEFUL_COLOR = "#ea580c"
DEFAULT_COLOR = "#2563eb"

#: Per-channel line colours. Channel zero repeats the colours the report has
#: always used, so a single-channel figure comes out exactly as it did before.
INPUT_COLORS = ("#6b7280", "#0f766e", "#a16207", "#4338ca")
PLANT_COLORS = ("#111827", "#1d4ed8", "#047857", "#7c3aed")
MODEL_COLORS = ("#dc2626", "#ea580c", "#db2777", "#ca8a04")
ERROR_COLORS = ("#b91c1c", "#c2410c", "#be185d", "#b45309")


def main(argv: list[str] | None = None) -> int:
    parser = _parser()
    args = parser.parse_args(argv)
    if args.restarts < 1:
        parser.error("--restarts must be positive")
    if args.islands < 1:
        parser.error("--islands must be positive")
    if args.population % args.islands:
        parser.error("--population must be divisible by --islands")
    island_size = args.population // args.islands
    if island_size < 3:
        parser.error("each island needs at least three population members")
    if args.migration_interval < 1:
        parser.error("--migration-interval must be positive")
    if not 1 <= args.migration_size <= island_size:
        parser.error("--migration-size must fit within one island")
    if args.protect_topologies < 0:
        parser.error("--protect-topologies cannot be negative")
    if args.protect_topologies and not 1 <= args.protected_slots <= island_size - 2:
        parser.error("--protected-slots must fit beside the two benchmark elites")
    if not 0.0 <= args.protected_parent_rate <= 1.0:
        parser.error("--protected-parent-rate must be between zero and one")
    if args.simulation_workers < 1:
        parser.error("--simulation-workers must be positive")
    if args.expansion_generations < 1 or args.compression_generations < 1:
        parser.error("complexity phase lengths must be positive")
    if args.node_penalty < 0 or args.expansion_node_penalty < 0:
        parser.error("node penalties cannot be negative")
    if args.expansion_node_penalty > args.node_penalty:
        parser.error("--expansion-node-penalty cannot exceed --node-penalty")
    if args.same_initial_population and args.island_policies != "uniform":
        parser.error(
            "--same-initial-population requires --island-policies uniform: with "
            "niches the islands' policies differ, so one population cannot be "
            "valid for all of them"
        )
    benchmark = BENCHMARKS[args.benchmark]
    title = benchmark.title
    if args.time_step is None:
        args.time_step = benchmark.time_step
    if args.graph == "reference" and benchmark.reference_of is None:
        parser.error(f"the {args.benchmark} benchmark has no reference graph to draw")

    island_runs = _island_runs(args, benchmark)
    trajectory = island_runs[0][1]
    if not all(_bounded(item) for _, item, _ in island_runs):
        hint = (
            "\n  (the Narendra plant goes unstable somewhere above 1.6)"
            if args.benchmark == "narendra"
            else ""
        )
        print(
            f"\nThe {args.benchmark} plant diverges at amplitude {args.amplitude:g}.\n"
            "  It is only stable for bounded inputs; try a smaller --amplitude."
            f"{hint}",
            file=sys.stderr,
        )
        return 1
    dataset = trajectory_dataset(trajectory)
    inputs, outputs = trajectory_interface(trajectory)
    island_datasets = tuple(item for _, _, item in island_runs)
    diverse_signals = args.island_signals == "diverse" and args.islands > 1
    island_score_scales = (
        tuple(
            trajectory_floor(item, args.criterion, args.time_step)
            for _, item, _ in island_runs
        )
        if diverse_signals
        else None
    )
    evaluator = SignalEvaluator(
        dataset, criterion=args.criterion, time_step=args.time_step
    )
    reference = benchmark.reference_of() if benchmark.reference_of is not None else None
    seed = (
        seed_graph()
        if (inputs, outputs) == (1, 1)
        else static_seed_graph(inputs, outputs)
    )

    linear_floor = trajectory_floor(trajectory, args.criterion, args.time_step)
    # Where the closed-form floor is known to be beaten by a memoryless model,
    # the bar has to be found by searching for one instead. That search costs
    # about three runs at this budget, so it waits until the evolution is over:
    # a run measured in hours should be watchable from its first generation
    # rather than after a long silent preamble.
    deferred = (
        not benchmark.linear_floor_is_strict and not args.no_stateless_baseline
    )
    floor = linear_floor
    bar_label = "memoryless floor" if not deferred else "pass criterion"

    print(
        f"\n{title}  --  {args.steps} steps, {args.generations} generations, "
        f"{('diverse island inputs' if args.island_signals == 'diverse' and args.islands > 1 else args.input + ' input')} "
        f"at amplitude {args.amplitude:g}, {args.criterion.upper()} fitness"
    )
    if deferred:
        print(
            f"  search floor: {linear_floor:.6g}  (closed form, provisional)\n"
            f"    the pass criterion is established by a memoryless search once "
            f"this run ends, because the\n"
            f"    closed form is not a criterion on this plant; pass "
            f"--no-stateless-baseline to skip that search"
        )
    refinement = _refinement(args)
    excluded_mutations, excluded_crossovers = _exclusions(args)
    schedule = _complexity_schedule(args)
    print(f"  operators: {len(_operators(args))} ({args.operators})")
    print(
        "  complexity pressure: "
        + (
            f"oscillating; expand {args.expansion_generations} gen @ "
            f"{args.expansion_node_penalty:g}, compress "
            f"{args.compression_generations} gen @ {args.node_penalty:g}"
            if schedule is not None
            else f"fixed node penalty {args.node_penalty:g}"
        )
    )
    print(
        "  refinement: " + _refinement_summary(refinement, excluded_mutations)
    )
    if excluded_mutations or excluded_crossovers:
        print(
            "  reproduction excluded: "
            + ", ".join((*excluded_mutations, *excluded_crossovers))
        )
    if args.parameter_alphabet != "off":
        print(
            f"  parameter alphabet: {args.parameter_alphabet}; "
            "values are symbols and change only when a carrier is made or replaced"
        )
    protection_size = args.protected_slots if args.protect_topologies else 0
    protection_rate = args.protected_parent_rate if args.protect_topologies else 0.0
    print(
        "  topology protection: "
        + (
            f"{args.protect_topologies} generations, {protection_size} slots, "
            f"{protection_rate:g} parent rate"
            if args.protect_topologies
            else "off"
        )
    )
    print(f"  simulation workers: {args.simulation_workers}")
    if diverse_signals:
        print("  island fitness: normalized; 1.0 = each signal's memoryless floor")
    print(
        f"  islands: {args.islands} x {island_size}; {args.island_exchange}: "
        + (
            f"{args.migration_size} every {args.migration_interval} generations"
            if args.islands > 1
            else "off"
        )
    )
    offspring_slots = island_size - 2 - protection_size
    if args.islands > 1 and offspring_slots < 4:
        print(
            f"  WARNING: only {offspring_slots} new offspring slot(s) per island; "
            f"use --population {args.islands * (2 + protection_size + 4)} or larger"
        )
    if args.islands > 1:
        profiles = _island_policies(args)
        if args.island_policies == "uniform":
            print(
                f"    all {args.islands} islands identical: "
                f"{', '.join(profiles[0][1].evolvable_kinds)}"
            )
            print(
                f"    signals: {args.island_signals};  starts: "
                + ("identical" if args.same_initial_population else "per island")
                + f";  exchange: up to {args.migration_size} hybrid(s) per island "
                f"every {args.migration_interval} generations "
                f"({args.migration_size / island_size:.0%} of an island)"
            )
        else:
            for index, ((name, policy), (signal, _, _)) in enumerate(
                zip(profiles, island_runs), 1
            ):
                print(
                    f"    {index}. {name} | {signal}: "
                    f"{', '.join(policy.evolvable_kinds)}"
                )
    ranges = _response_ranges(trajectory)
    prefix = (
        f"  {args.steps} steps x dt {args.time_step:g} = "
        f"{args.steps * args.time_step:g} simulated time;  response "
    )
    if len(ranges) == 1:
        low, high = ranges[0]
        print(f"{prefix}range [{low:+.3f}, {high:+.3f}]")
    else:
        print(f"{prefix}ranges")
        for channel, (low, high) in enumerate(ranges):
            print(f"    y{channel}: [{low:+.3f}, {high:+.3f}]")
    print("=" * 78)
    initialization = args.initialization
    if initialization == "auto":
        initialization = benchmark.initialization
    print(f"  initialization: {initialization}; restarts: {args.restarts}")

    result = None
    best_item = None
    history: list[float] = []
    for restart in range(args.restarts):
        run_seed = args.seed + restart
        policy = benchmark_policy(_operators(args), alleles=_alleles(args))
        profiles = _island_policies(args)
        # Nothing island-specific to pass when every island reproduces and is
        # scored identically: the engine's own defaults then give all of them
        # one shared policy and one shared evaluator, which is the point of
        # uniform mode.
        undifferentiated = (
            args.island_policies == "uniform" and args.island_signals == "shared"
        )
        engine = benchmark_engine(
            dataset,
            policy=policy,
            seed=run_seed,
            population_size=args.population,
            criterion=args.criterion,
            time_step=args.time_step,
            node_penalty=args.node_penalty,
            refine=_refinement(args),
            excluded_mutations=excluded_mutations,
            excluded_crossovers=excluded_crossovers,
            topology_protection_generations=args.protect_topologies,
            topology_protection_size=protection_size,
            topology_parent_rate=protection_rate,
            simulation_workers=args.simulation_workers,
            island_count=args.islands,
            migration_interval=args.migration_interval if args.islands > 1 else 0,
            migration_size=args.migration_size if args.islands > 1 else 0,
            island_policies=(
                None if undifferentiated else tuple(p for _, p in profiles)
            ),
            island_datasets=None if undifferentiated else island_datasets,
            island_score_scales=island_score_scales,
            complexity_schedule=schedule,
            island_exchange=args.island_exchange,
        )
        if initialization == "dynamic":
            population = []
            for island, (_, island_policy) in enumerate(profiles):
                island_dataset = (
                    dataset if island_datasets is None else island_datasets[island]
                )
                if set(island_policy.evolvable_kinds) & STATEFUL_KINDS:
                    population.extend(dynamic_population(
                        island_dataset,
                        island_policy,
                        island_size,
                        run_seed + 1_000 + (
                            0 if args.same_initial_population else island
                        ),
                        args.initial_nodes_min,
                        args.initial_nodes_max,
                        args.initial_stateful,
                    ))
                else:
                    population.extend(seed.copy() for _ in range(island_size))
        else:
            population = [seed.copy() for _ in range(args.population)]
        if args.restarts > 1:
            print(f"\nRESTART {restart + 1}/{args.restarts} (seed {run_seed})")
        try:
            candidate, candidate_history = _evolve(
                engine, population, args, 1.0 if diverse_signals else floor, bar_label
            )
        finally:
            engine.close()
        candidate_best = min(
            candidate.population,
            key=lambda item: _aggregate_score(item.graph, island_runs, args),
        )
        if result is None or _aggregate_score(
            candidate_best.graph, island_runs, args
        ) < _aggregate_score(best_item.graph, island_runs, args):  # type: ignore[union-attr]
            result, history = candidate, candidate_history
            best_item = candidate_best
    assert result is not None
    assert best_item is not None
    best = best_item.graph

    # The bar has to be settled before the verdict is read. Where the closed
    # form is not a criterion, the stricter of it and the memoryless search is
    # used, because a closed-form optimum is itself reachable without state and
    # so remains a legitimate memoryless score.
    baseline = _stateless_bar(dataset, args, linear_floor) if deferred else None
    if baseline is not None:
        ratio = baseline / linear_floor
        if baseline < linear_floor:
            floor = baseline
            print(
                f"  criterion: the memoryless search beats the closed form "
                f"({ratio:.2f}x), so the search is the bar"
            )
        else:
            print(
                f"  criterion: the memoryless search did not beat the closed form "
                f"({ratio:.2f}x), so the form stays the bar; a larger budget would "
                f"make the search stricter"
            )

    # The search optimises accuracy plus a size penalty, but the floor is a
    # pure accuracy figure, so the verdict must use the unpenalised error.
    true_error = evaluator.evaluate(best)
    scores = {
        f"seed ({'y = u' if (inputs, outputs) == (1, 1) else 'static map'})": (
            evaluator.evaluate(seed)
        ),
    }
    if baseline is None:
        scores["memoryless floor"] = linear_floor
    else:
        scores["closed-form floor"] = linear_floor
        scores["memoryless search"] = baseline
        if floor != linear_floor:
            scores["pass criterion (stricter)"] = floor
    scores["evolved (accuracy)"] = true_error
    scores[
        "evolved (local shaped ratio)" if diverse_signals else "evolved (with penalty)"
    ] = best_item.score
    scores["all-signal floor ratio"] = _aggregate_score(best, island_runs, args)
    if reference is not None:
        scores["exact reference"] = evaluator.evaluate(reference)
    signals = _signals(trajectory, best)
    _print_scores(scores, floor, best, signals, args.criterion, bar_label)
    _print_graph(best)

    print("\nEVOLVED MODEL EQUATIONS")
    print("-" * 78)
    print(format_equations(best))
    if reference is not None:
        print("\nTRUE PLANT EQUATIONS (for comparison)")
        print("-" * 78)
        print(format_equations(reference))

    drawn = reference if args.graph == "reference" else best
    _figure(args, title, signals, drawn, scores, floor, history, bar_label)
    return 0


def _operators(args) -> tuple[str, ...]:
    if args.operators.strip() == "all":
        return FULL_OPERATORS
    return tuple(name.strip() for name in args.operators.split(",") if name.strip())


def _refinement_summary(refinement, excluded_mutations) -> str:
    """Describe refinement as it will run, not as it was asked for.

    Excluding the tuner's own operators switches refinement off, and a header
    that still advertised a tuning budget would misdescribe the run -- which is
    the one thing a header exists to prevent.
    """
    if refinement is None:
        return "off"
    dropped = set(excluded_mutations)
    if all(name in dropped for name in tuner_names()):
        return "off (its operators are excluded)"
    return (
        f"min {refinement.min_steps}, patience {refinement.patience}, "
        f"max {refinement.max_steps}"
    )


def _exclusions(args) -> tuple[tuple[str, ...], tuple[str, ...]]:
    """Split the reproduction operators to leave out into mutations and crossovers.

    Class names are unique across both sets, so one flag can name either kind
    without the caller having to know which list an operator came from. A name
    that matches neither is a typo, and a typo that excluded nothing would
    quietly change what the run means while still producing a number.
    """
    names = [
        name.strip() for name in args.exclude_reproduction.split(",") if name.strip()
    ]
    mutations, crossovers = mutation_names(), crossover_names()
    unknown = sorted(
        {name for name in names if name not in mutations and name not in crossovers}
    )
    if unknown:
        raise SystemExit(
            f"Unknown reproduction operator(s): {', '.join(unknown)}\n"
            f"  mutations:  {', '.join(sorted(mutations))}\n"
            f"  crossovers: {', '.join(sorted(crossovers))}"
        )
    return (
        tuple(name for name in names if name in mutations),
        tuple(name for name in names if name in crossovers),
    )


def _stateless_bar(dataset, args, linear_floor: float) -> float:
    """Find the memoryless pass criterion, announcing the wait before it starts.

    This is three searches at the run's own budget, so on a long run it is a
    long wait in its own right. The caller is told it is starting and told
    again when it finishes, or a run left unattended looks hung.
    """
    print(
        f"\n  finding the pass criterion: best memoryless model over "
        f"{args.population} candidates x {args.generations} generations "
        f"x 3 seeds",
        flush=True,
    )
    started = perf_counter()
    baseline = stateless_floor(
        dataset,
        args.criterion,
        args.time_step,
        population_size=args.population,
        generations=args.generations,
    )
    print(
        f"  memoryless search finished in {_duration(perf_counter() - started)}; "
        f"best memoryless score {baseline:.6g} against a closed-form floor of "
        f"{linear_floor:.6g}",
        flush=True,
    )
    return baseline


def _alleles(args):
    """The parameter alphabet the run will use, or None for continuous values.

    ``coarse`` makes every parameter a symbol from a small set, which is what
    lets a replacement be a sensible event: a redraw from six values can be
    searched, a redraw from a continuum cannot.
    """
    if args.parameter_alphabet == "off":
        return None
    return dict(PARAMETER_ALPHABETS[args.parameter_alphabet])


def _island_policies(args) -> list[tuple[str, SignalGraphPolicy]]:
    """The policy each island reproduces with.

    ``uniform`` gives every island the same policy, which is what turns the
    run into a pure test of isolation: the only difference between islands is
    then their own population and whatever crosses between them. The default
    builds deterministic signal-search niches instead, cycling the templates for
    extra islands.
    """
    allowed = tuple(_operators(args))
    alleles = _alleles(args)
    if args.islands == 1:
        return [("unrestricted", benchmark_policy(allowed, alleles=alleles))]
    if args.island_policies == "uniform":
        return [("uniform", benchmark_policy(allowed, alleles=alleles))] * args.islands
    templates = (
        ("simple/static", ("add", "negate", "constant")),
        (
            "delay-heavy",
            ("delay", "integral", "derivative", "filter_lp", "filter_hp", "add", "multiply", "constant"),
        ),
        (
            "nonlinear",
            ("sin", "tanh", "square", "sqrt", "abs", "exp", "log", "delay", "add", "multiply", "constant"),
        ),
        ("unrestricted", allowed),
    )
    profiles = []
    nonlinear = frozenset(OPERATOR_GROUPS["nonlinear"])
    stateful = frozenset(STATEFUL_KINDS)
    for index in range(args.islands):
        name, preferred = templates[index % len(templates)]
        kinds = tuple(kind for kind in preferred if kind in allowed) or allowed
        base = benchmark_policy(kinds)
        niche = index % len(templates)
        required = (
            ()
            if niche in (0, 3)
            else ((stateful,) if niche == 1 else (stateful, nonlinear))
        )
        forbidden = stateful | nonlinear if niche == 0 else frozenset()
        profiles.append((
            name,
            SignalGraphPolicy(
                base.evolvable_kinds,
                alleles=alleles,
                required_kind_groups=required,
                forbidden_kinds=forbidden,
            ),
        ))
    return profiles


def _refinement(args) -> RefinementConfig | None:
    """Parameter search applied to each new topology, or None to skip it."""
    if not args.refine:
        return None
    return RefinementConfig(
        min_steps=args.refine_min,
        patience=args.refine_patience,
        max_steps=args.refine_max,
    )


def _complexity_schedule(args) -> OscillatingParsimony | None:
    if not args.oscillating_penalty:
        return None
    return OscillatingParsimony(
        expansion_generations=args.expansion_generations,
        compression_generations=args.compression_generations,
        expansion_node_weight=args.expansion_node_penalty,
        compression_node_weight=args.node_penalty,
    )


def _bounded(trajectory) -> bool:
    """A nonlinear plant can be driven past its stable operating region."""
    _, responses = trajectory_rows(trajectory)
    return all(
        isfinite(value) and abs(value) < 1e6 for row in responses for value in row
    )


def _response_ranges(trajectory) -> tuple[tuple[float, float], ...]:
    """Each output channel's span, so a wide plant reports one line per channel."""
    _, responses = trajectory_rows(trajectory)
    return tuple(
        (
            min(row[channel] for row in responses),
            max(row[channel] for row in responses),
        )
        for channel in range(len(responses[0]))
    )


def _controls(args) -> tuple[float, ...]:
    if args.input == "sine":
        return evaluation_controls(args.steps, args.amplitude, args.period)
    if args.input == "step":
        return step_controls(args.steps, args.hold, args.amplitude)
    return training_controls(args.steps, Random(args.data_seed), args.amplitude)


def _channel_controls(args, channels: int) -> tuple[tuple[float, ...], ...]:
    """The primary excitation when the plant has more than one input.

    ``--hold`` applies to the random shape here, not only to the step shape. A
    plant with several inputs is one whose slowest mode is usually far slower
    than the sample time, and such a plant filters sample-to-sample noise out
    almost entirely, so an unheld input would carry no dynamics to identify.
    """
    if args.input == "sine":
        return channel_evaluation_controls(
            args.steps, channels, args.amplitude, args.period
        )
    if args.input == "step":
        return channel_step_controls(args.steps, channels, args.hold, args.amplitude)
    return channel_training_controls(
        args.steps, channels, Random(args.data_seed), args.amplitude, args.hold
    )


def _island_runs(args, benchmark) -> list[tuple[str, object, object]]:
    """Build one deterministic training trajectory per island."""
    if benchmark.channels == 1:
        return _scalar_island_runs(args, benchmark.build)
    return _wide_island_runs(args, benchmark.build, benchmark.channels)


def _scalar_island_runs(args, trajectory_of) -> list[tuple[str, object, object]]:
    """The single-channel island rotation, left exactly as it was measured."""
    primary = _controls(args)
    controls: list[tuple[str, tuple[float, ...]]] = [(args.input, primary)]
    for index in range(1, args.islands):
        if args.island_signals == "shared":
            controls.append((args.input, primary))
        elif index % 3 == 1:
            seed = args.data_seed + 104_729 * index
            controls.append((
                f"random seed {seed}",
                training_controls(args.steps, Random(seed), args.amplitude),
            ))
        elif index % 3 == 2:
            period = max(4, args.period // (index // 3 + 1))
            phase = period // 4
            controls.append((
                f"sine period {period}, phase 1/4",
                tuple(
                    args.amplitude * sin(2.0 * pi * (step + phase) / period)
                    for step in range(args.steps)
                ),
            ))
        else:
            hold = max(1, args.hold + index)
            base = step_controls(args.steps + hold, hold, args.amplitude)
            controls.append((
                f"step hold {hold}, shifted",
                tuple(base[hold // 2 : hold // 2 + args.steps]),
            ))
    return [
        (label, trajectory := trajectory_of(values), trajectory_dataset(trajectory))
        for label, values in controls
    ]


def _wide_island_runs(
    args, trajectory_of, channels: int
) -> list[tuple[str, object, object]]:
    """The multi-channel island rotation.

    A wider interface offers a second way to keep islands apart: the excitation
    shapes differ per channel as well as between islands, so no island can
    score by fitting one channel's response to another's.
    """
    primary = _channel_controls(args, channels)
    controls: list[tuple[str, object]] = [(args.input, primary)]
    for index in range(1, args.islands):
        if args.island_signals == "shared":
            controls.append((args.input, primary))
        elif index % 3 == 1:
            seed = args.data_seed + 104_729 * index
            controls.append((
                f"random seed {seed}",
                channel_training_controls(
                    args.steps, channels, Random(seed), args.amplitude, args.hold
                ),
            ))
        elif index % 3 == 2:
            period = max(4, args.period // (index // 3 + 1))
            controls.append((
                f"sine period {period}, per channel",
                channel_evaluation_controls(
                    args.steps, channels, args.amplitude, period
                ),
            ))
        else:
            hold = max(1, args.hold + index)
            controls.append((
                f"step hold {hold}, per channel",
                channel_step_controls(args.steps, channels, hold, args.amplitude),
            ))
    return [
        (label, trajectory := trajectory_of(values), trajectory_dataset(trajectory))
        for label, values in controls
    ]


def _aggregate_score(graph: Graph, island_runs, args) -> float:
    """Mean error relative to each signal's memoryless floor."""
    ratios = []
    for _, trajectory, dataset in island_runs:
        error = SignalEvaluator(
            dataset, criterion=args.criterion, time_step=args.time_step
        ).evaluate(graph)
        floor = trajectory_floor(trajectory, args.criterion, args.time_step)
        ratios.append(error / max(abs(floor), 1e-12))
    return sum(ratios) / len(ratios)


def _evolve(
    engine, population: list[Graph], args, floor: float, bar_label: str
):
    """Run generation by generation so the fitness can be reported as it moves.

    ``run(population, 0)`` only evaluates, and every later generation is one
    result-level advance, so this consumes the random generator exactly as a single
    ``run(population, generations)`` call would and stays reproducible.
    """
    result = engine.run(population, 0)
    started = perf_counter()
    history = [result.best.score]
    names = [name for name, _ in _island_policies(args)]

    interval = args.progress or max(1, args.generations // 20)
    print(f"{'gen':>6}  {'event':<9} {'time':>8} {'rate':>7} {'ETA':>8}")
    schedule = _complexity_schedule(args)
    previous = _report(
        0, args.generations, result, interval, args, names, None, schedule,
        engine, started,
    )
    for generation in range(1, args.generations + 1):
        result = engine.advance(result)
        history.append(result.best.score)
        previous = _report(
            generation, args.generations, result, interval, args, names, previous,
            schedule,
            engine, started,
        )

    winner = _winning_island(result)
    print(
        f"  final fitness {result.best.score:.10g} from I{winner + 1} {names[winner]}"
        f"   ({bar_label} {floor:.6g})"
    )
    return result, history


def _report(
    generation, total, result, interval, args, names, previous, schedule,
    engine, started,
):
    detailed = generation % interval == 0 or generation == total
    exchange = (
        args.islands > 1
        and generation > 0
        and generation % args.migration_interval == 0
    )
    island_bests = tuple(island[0].score for island in result.islands)
    winner = _winning_island(result)
    elapsed = perf_counter() - started
    rate = generation / elapsed if generation and elapsed > 0 else 0.0
    eta = (total - generation) / rate if rate > 0 else 0.0
    note = ""
    if schedule is not None:
        note += f"   {_pressure(schedule, generation)}"
    if exchange:
        note += f"   {args.migration_size} each; {_exchange_summary(engine)}"
    if not detailed:
        if exchange:
            print(
                f"{generation:>6}  {_exchange_event(args):<9} {_duration(elapsed):>8} "
                f"{rate:>7.2f} {_duration(eta):>8}{note}",
                flush=True,
            )
        return previous

    print(
        f"{generation:>6}  {'REPORT+X' if exchange else 'REPORT':<9} "
        f"{_duration(elapsed):>8} {rate:>7.2f} {_duration(eta):>8}{note}",
        flush=True,
    )
    if previous is None:
        print(
            f"        {'island':<18} {'fit':>10} {'change':>9} {'nodes':>6} "
            f"{'memory':>7} {'nonlin':>7} {'topo':>6}",
            flush=True,
        )
        if len(result.islands) > 1:
            print(
                "        change is since the previous report; negative is improvement",
                flush=True,
            )
    for index, island in enumerate(result.islands):
        finite_scores = [item.score for item in island if item.score < float("inf")]
        best = island[0]
        stateful = sum(
            node.kind in STATEFUL_KINDS for node in best.graph.nodes.values()
        )
        nonlinear = sum(
            node.kind in OPERATOR_GROUPS["nonlinear"]
            for node in best.graph.nodes.values()
        )
        topologies = len({_topology_profile(item.graph) for item in island})
        change = "first" if previous is None else f"{island_bests[index] - previous[index]:+.3g}"
        mark = "*" if index == winner else " "
        lossy = (
            ""
            if len(finite_scores) == len(island)
            else f"   !{len(finite_scores)}/{len(island)} finite"
        )
        label = f"I{index + 1} {names[index]}"
        print(
            f"     {mark}  {label:<18} {best.score:>10.6g} "
            f"{change:>9} {len(best.graph.nodes):>6} {stateful:>7} {nonlinear:>7} "
            f"{topologies:>4}/{len(island)}{lossy}",
            flush=True,
        )
    return island_bests


def _winning_island(result) -> int:
    return min(range(len(result.islands)), key=lambda index: result.islands[index][0].score)


def _pressure(schedule, generation: int) -> str:
    if schedule is None:
        return "fixed"
    return f"{schedule.phase_at(generation)} @ {schedule.node_weight_at(generation):g}"


def _exchange_event(args) -> str:
    return "CROSS" if args.island_exchange == "crossover" else "MIGRATE"


def _exchange_summary(engine) -> str:
    """How many hybrids each island accepted, for the exchange row."""
    if not engine.last_exchange_stats:
        return "no exchange"
    return " ".join(
        f"I{index + 1}={accepted}/{attempted}"
        for index, (attempted, accepted) in enumerate(engine.last_exchange_stats)
    )


def _topology_profile(graph: Graph) -> tuple[object, ...]:
    kinds = Counter(node.kind for node in graph.nodes.values())
    connections = Counter(
        (graph.nodes[edge.source].kind, graph.nodes[edge.target].kind)
        for edge in graph.edges.values()
    )
    return tuple(sorted(kinds.items())), tuple(sorted(connections.items()))


def _duration(seconds: float) -> str:
    seconds = max(0, int(seconds))
    hours, remainder = divmod(seconds, 3600)
    minutes, seconds = divmod(remainder, 60)
    return f"{hours:d}:{minutes:02d}:{seconds:02d}"


def _print_scores(
    scores: dict[str, float],
    floor: float,
    best: Graph,
    signals: dict,
    criterion: str,
    bar_label: str = "memoryless floor",
) -> None:
    print(f"\nFITNESS ({criterion.upper()}, lower is better)")
    print("-" * 78)
    width = max(len(name) for name in scores)
    for name, value in scores.items():
        print(f"  {name.ljust(width)}   {value:>14.6g}")

    if "model" in signals:
        channels = len(signals["plant"])
        residual = [
            a - b
            for channel in range(channels)
            for a, b in zip(signals["plant"][channel], signals["model"][channel])
        ]
        rmse = (sum(value * value for value in residual) / len(residual)) ** 0.5
        peak = max(abs(value) for value in residual)
        if channels == 1:
            fit = fit_percentage(signals["plant"][0], signals["model"][0])
            print(f"\n  open-loop fit    {fit:>13.2f} %   (100 = exact, 0 = no better than the mean)")
            print(f"  residual RMSE    {rmse:>13.6g}")
            print(f"  residual peak    {peak:>13.6g}")
        else:
            print("\n  open-loop fit by channel")
            for channel in range(channels):
                fit = fit_percentage(
                    signals["plant"][channel], signals["model"][channel]
                )
                print(f"    y{channel}            {fit:>13.2f} %")
            print(f"  residual RMSE    {rmse:>13.6g}   (channels pooled)")
            print(f"  residual peak    {peak:>13.6g}")

    stateful = [node for node in best.nodes.values() if node.kind in STATEFUL_KINDS]
    beat = scores["evolved (accuracy)"] < floor
    print("\nVERDICT")
    print("-" * 78)
    print(f"  below {bar_label} : {'YES' if beat else 'NO'}")
    print(f"  stateful nodes found   : {len(stateful)}"
          + (f"  ({', '.join(sorted(node.kind for node in stateful))})" if stateful else ""))
    flattened = (
        "static gain on u(k)"
        if len(signals["plant"]) == 1
        else "static map from u(k)"
    )
    if beat and stateful:
        print("  -> the model represents dynamics; it is not just fitting a gain.")
    else:
        print(f"  -> no dynamics identified; this is the best {flattened}.")


def _print_graph(graph: Graph) -> None:
    print("\nEVOLVED GRAPH")
    print("-" * 78)
    layout = build_layout(graph)
    print(f"  {len(graph.nodes)} nodes, {len(graph.edges)} edges,"
          f" {layout.width} layers deep")
    for node_id in sorted(graph.nodes, key=lambda item: (layout.layers[item], item)):
        node = graph.nodes[node_id]
        attributes = ", ".join(
            f"{key}={_round(value)}" for key, value in sorted(node.attributes.items())
        )
        print(f"    {node_id:<10} {node.kind:<12} {attributes}")
    print()
    for edge in sorted(graph.edges.values(), key=lambda item: item.id):
        weight = float(edge.attributes.get("weight", 1.0))
        print(f"    {edge.source:>10}  ->  {edge.target:<10}  w={weight:+.4f}")


def _signals(trajectory, best: Graph) -> dict[str, tuple[tuple[float, ...], ...]]:
    """Open-loop simulation: the model sees only u(k), never the true output.

    Every entry holds one flat series per channel, so a single-channel
    benchmark yields one-element tuples and everything below stays
    channel-agnostic without a second code path.
    """
    controls, responses = trajectory_rows(trajectory)
    signals: dict[str, tuple[tuple[float, ...], ...]] = {
        "input": tuple(
            tuple(row[channel] for row in controls)
            for channel in range(len(controls[0]))
        ),
        "plant": tuple(
            tuple(row[channel] for row in responses)
            for channel in range(len(responses[0]))
        ),
    }
    try:
        modelled = SignalSimulator().run_series(best, controls)
    except SignalSimulationError as exc:
        print(f"  (the evolved model could not be simulated: {exc})")
        return signals
    signals["model"] = tuple(
        tuple(row[channel] for row in modelled) for channel in range(len(modelled[0]))
    )
    return signals


def fit_percentage(measured: list[float], modelled: list[float]) -> float:
    """The standard identification fit: 100% is exact, 0% is no better than the mean.

    This is the normalised-RMSE figure reported by system-identification tools,
    and unlike a raw squared error it does not change when the input amplitude
    or the run length changes, so runs stay comparable.
    """
    mean = sum(measured) / len(measured)
    residual = sum((a - b) ** 2 for a, b in zip(measured, modelled))
    spread = sum((value - mean) ** 2 for value in measured)
    if spread == 0.0:
        return 0.0
    return 100.0 * (1.0 - (residual / spread) ** 0.5)


def _pooled_fit(signals: dict) -> float:
    """One fit figure across every channel, for the places with room for one.

    Pooling rather than averaging is deliberate: it weights each channel by its
    own spread, so a channel the model tracks almost perfectly cannot be hidden
    by averaging against one it tracks badly.
    """
    measured = [value for series in signals["plant"] for value in series]
    modelled = [value for series in signals["model"] for value in series]
    return fit_percentage(measured, modelled)





def _figure(
    args,
    title: str,
    signals: dict,
    graph: Graph,
    scores: dict,
    floor: float,
    history: list,
    bar_label: str,
) -> None:
    try:
        import matplotlib
    except ModuleNotFoundError:
        print("\n(matplotlib is not installed; install it to see the figure)")
        return
    if args.no_show:
        matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    # Size each panel to its content so a small graph is not stranded in a
    # tall empty band.
    layout = build_layout(graph)
    rows = layout.height
    equation_lines = len(graph_to_equations(graph))
    heights = [
        1.1,
        3.4,
        1.7,
        2.6,
        max(2.4, 1.15 * rows + 1.4),
        max(1.0, 0.26 * equation_lines + 0.6),
    ]

    fit = _pooled_fit(signals) if "model" in signals else 0.0
    figure = plt.figure(figsize=(14, sum(heights) + 1.0))
    grid = figure.add_gridspec(len(heights), 1, height_ratios=heights, hspace=0.55)
    figure.suptitle(
        f"{title}  --  open-loop identification   fit {fit:.1f}%   "
        f"{args.criterion.upper()} {scores['evolved (accuracy)']:.4g}"
        f"  ({bar_label} {floor:.4g})",
        fontsize=13,
    )

    time = [step * args.time_step for step in range(len(signals["plant"][0]))]
    label = "time" if args.time_step != 1.0 else "step k"
    _plot_input(figure.add_subplot(grid[0]), signals, time)
    _plot_comparison(figure.add_subplot(grid[1]), signals, time)
    _plot_error(figure.add_subplot(grid[2]), signals, time, label)
    _plot_fitness(figure.add_subplot(grid[3]), history, floor, args.criterion, bar_label)
    graph_axes = figure.add_subplot(grid[4])
    graph_axes.set_title(
        "Model graph  (orange = feedback edge into a delay)", loc="left", fontsize=11
    )
    draw_graph(graph_axes, graph, layout)
    _plot_equations(figure.add_subplot(grid[5]), graph)

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    figure.savefig(output, dpi=130, bbox_inches="tight")
    print(f"\nFigure written to {output}")
    if not args.no_show:
        plt.show()


def _plot_input(axes, signals: dict, time: list[float]) -> None:
    inputs = signals["input"]
    single = len(inputs) == 1
    for channel, series in enumerate(inputs):
        axes.plot(
            time,
            series,
            color=INPUT_COLORS[channel % len(INPUT_COLORS)],
            linewidth=0.9,
            label="u(k)" if single else f"u{channel}(k)",
        )
    axes.set_title(
        "Excitation u(k)" if single else "Excitation u(k), per input channel",
        loc="left",
        fontsize=10,
    )
    axes.set_ylabel("u")
    axes.grid(alpha=0.25)
    if not single:
        axes.legend(loc="upper right", fontsize=8, ncols=min(4, len(inputs)))
    axes.tick_params(labelbottom=False)


def _plot_comparison(axes, signals: dict, time: list[float]) -> None:
    plant = signals["plant"]
    single = len(plant) == 1
    for channel, series in enumerate(plant):
        axes.plot(
            time,
            series,
            color=PLANT_COLORS[channel % len(PLANT_COLORS)],
            linewidth=2.0,
            label="plant (measured)" if single else f"plant y{channel} (measured)",
        )
    if "model" in signals:
        for channel, series in enumerate(signals["model"]):
            fit = fit_percentage(plant[channel], series)
            axes.plot(
                time,
                series,
                color=MODEL_COLORS[channel % len(MODEL_COLORS)],
                linewidth=1.3,
                label=(
                    f"AMEBA model (fit {fit:.1f}%)"
                    if single
                    else f"model y{channel} (fit {fit:.1f}%)"
                ),
            )
    axes.set_title(
        "Open-loop response: measured vs identified model", loc="left", fontsize=11
    )
    axes.set_ylabel("y(k+1)")
    axes.grid(alpha=0.25)
    if single:
        axes.legend(loc="upper right", fontsize=9)
    else:
        axes.legend(loc="upper right", fontsize=8, ncols=2)
    axes.tick_params(labelbottom=False)


def _plot_error(axes, signals: dict, time: list[float], label: str) -> None:
    if "model" not in signals:
        axes.axis("off")
        return
    plant = signals["plant"]
    single = len(plant) == 1
    residuals = [
        [a - b for a, b in zip(plant[channel], signals["model"][channel])]
        for channel in range(len(plant))
    ]
    pooled = [value for residual in residuals for value in residual]
    peak = max(abs(value) for value in pooled) or 1.0
    axes.axhline(0.0, color="#9ca3af", linewidth=0.8)
    for channel, residual in enumerate(residuals):
        axes.plot(
            time,
            residual,
            color=ERROR_COLORS[channel % len(ERROR_COLORS)],
            linewidth=0.9,
            label=None if single else f"y{channel}",
        )
        axes.fill_between(
            time,
            residual,
            color=MODEL_COLORS[channel % len(MODEL_COLORS)],
            alpha=0.25,
        )
    rmse = (sum(value * value for value in pooled) / len(pooled)) ** 0.5
    axes.set_title(
        f"Residual  (RMSE {rmse:.4g}, peak {peak:.4g})", loc="left", fontsize=10
    )
    axes.set_xlabel(label)
    axes.set_ylabel("error")
    axes.grid(alpha=0.25)
    if not single:
        axes.legend(loc="upper right", fontsize=8, ncols=min(4, len(residuals)))


def _plot_fitness(
    axes, history: list[float], floor: float, criterion: str, bar_label: str
) -> None:
    finite = [value for value in history if value < float("inf")]
    axes.plot(history, color="#dc2626", linewidth=1.6, label="best fitness")
    axes.axhline(
        floor,
        color="#2563eb",
        linestyle="--",
        linewidth=1.2,
        label=f"{bar_label} ({floor:.3g})",
    )
    axes.set_title(
        "Fitness by generation  (below the floor = dynamics identified)",
        loc="left",
        fontsize=11,
    )
    axes.set_xlabel("generation", labelpad=1)
    axes.set_ylabel(criterion.upper())
    # Fitness spans orders of magnitude, and a log axis is the only way the
    # late-run improvements stay visible next to the starting error.
    if finite and min(finite) > 0:
        axes.set_yscale("log")
    axes.grid(alpha=0.25, which="both")
    axes.legend(loc="upper right", fontsize=9)



def _plot_equations(axes, graph) -> None:
    lines = [f"{equation.text}     [{equation.note}]" for equation in graph_to_equations(graph)]
    axes.text(
        0.01,
        0.95,
        "\n".join(lines) or "(empty graph)",
        family="monospace",
        fontsize=8.5,
        va="top",
        ha="left",
        transform=axes.transAxes,
    )
    axes.set_title("Recovered difference equations", loc="left", fontsize=11)
    axes.axis("off")


def _round(value: object) -> object:
    return round(value, 4) if isinstance(value, float) else value


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--benchmark", choices=sorted(BENCHMARKS), default="linear")
    parser.add_argument("--generations", type=int, default=150)
    parser.add_argument(
        "--population", type=int, default=16,
        help="total candidates, divided evenly across islands",
    )
    parser.add_argument("--seed", type=int, default=1, help="evolution random seed")
    parser.add_argument(
        "--restarts",
        type=int,
        default=1,
        help="independent runs; the report keeps the best result",
    )
    parser.add_argument(
        "--islands",
        type=int,
        default=1,
        help="specialized populations connected by ring migration",
    )
    parser.add_argument(
        "--island-policies",
        choices=("niches", "uniform"),
        default="niches",
        help=(
            "uniform gives every island the same reproduction policy, so the "
            "run isolates island structure alone; niches gives each its own "
            "operator profile"
        ),
    )
    parser.add_argument(
        "--same-initial-population",
        action="store_true",
        help=(
            "start every island from the same generated population, so an "
            "island difference cannot come from a luckier random start "
            "(requires --island-policies uniform)"
        ),
    )
    parser.add_argument(
        "--island-signals",
        choices=("diverse", "shared"),
        default="diverse",
        help="train islands on distinct deterministic inputs or one shared input",
    )
    parser.add_argument(
        "--island-exchange",
        choices=("crossover", "migration"),
        default="crossover",
        help="cross parents between islands or copy migrants",
    )
    parser.add_argument(
        "--migration-interval",
        "--exchange-interval",
        type=int,
        default=10,
        help="generations between island migrations",
    )
    parser.add_argument(
        "--migration-size",
        "--exchange-size",
        type=int,
        default=1,
        help="best candidates sent by each island per migration",
    )
    parser.add_argument("--data-seed", type=int, default=7, help="control-sequence seed")
    parser.add_argument(
        "--initialization",
        choices=("auto", "seed", "dynamic"),
        default="auto",
        help="initial population; auto uses each benchmark's own default",
    )
    parser.add_argument("--initial-nodes-min", type=int, default=10)
    parser.add_argument("--initial-nodes-max", type=int, default=14)
    parser.add_argument(
        "--initial-stateful",
        type=int,
        default=0,
        help="minimum live memory nodes in each dynamic initial graph",
    )
    parser.add_argument(
        "--steps", type=int, default=150, help="simulated time steps (run length)"
    )
    parser.add_argument(
        "--amplitude", type=float, default=1.0, help="input signal amplitude"
    )
    parser.add_argument(
        "--time-step",
        type=float,
        default=None,
        help=(
            "sample time dt; total simulated time is steps * dt "
            "(default: the benchmark's own sample time)"
        ),
    )
    parser.add_argument(
        "--input",
        choices=("random", "sine", "step"),
        default="random",
        help="control signal shape",
    )
    parser.add_argument(
        "--criterion",
        choices=sorted(CRITERIA),
        default="ise",
        help="integral error criterion used as fitness (default: ISE)",
    )
    parser.add_argument(
        "--operators",
        default="all",
        help=(
            "comma-separated operators or groups evolution may create: "
            "all, arithmetic, nonlinear, memory, or individual names"
        ),
    )
    parser.add_argument(
        "--exclude-reproduction",
        default="",
        metavar="NAMES",
        help=(
            "comma-separated reproduction operators to leave out, by class name; "
            "exclusions apply to refinement too, so "
            "MutateNodeAttributes,MutateEdgeAttributes leaves only structure "
            "able to write parameters"
        ),
    )
    parser.add_argument(
        "--parameter-alphabet",
        choices=("off", *sorted(PARAMETER_ALPHABETS)),
        default="off",
        help=(
            "restrict every parameter to a small set of values so a replacement "
            "draws a symbol instead of sampling a continuum (default: off)"
        ),
    )
    parser.add_argument(
        "--node-penalty",
        type=float,
        default=DEFAULT_NODE_PENALTY,
        help="fitness added per node, to hold back structural bloat",
    )
    parser.add_argument(
        "--oscillating-penalty",
        action="store_true",
        help="alternate low-pressure growth and high-pressure compression phases",
    )
    parser.add_argument(
        "--expansion-generations", type=int, default=25,
        help="generations in each low-complexity-pressure phase",
    )
    parser.add_argument(
        "--compression-generations", type=int, default=25,
        help="generations in each high-complexity-pressure phase",
    )
    parser.add_argument(
        "--expansion-node-penalty", type=float, default=0.0,
        help="relative node penalty during expansion phases",
    )
    parser.add_argument(
        "--refine",
        action="store_true",
        help="after each structural change, tune parameters while they keep improving",
    )
    parser.add_argument(
        "--refine-min", type=int, default=8, help="tuning steps every new topology gets"
    )
    parser.add_argument(
        "--refine-patience",
        type=int,
        default=6,
        help="stop tuning after this many attempts without improvement",
    )
    parser.add_argument(
        "--refine-max", type=int, default=40, help="hard ceiling on tuning steps"
    )
    parser.add_argument(
        "--protect-topologies",
        type=int,
        default=0,
        metavar="GENERATIONS",
        help="grace generations for new topologies (default: off)",
    )
    parser.add_argument(
        "--protected-slots",
        type=int,
        default=4,
        help="population slots reserved for distinct young topologies",
    )
    parser.add_argument(
        "--protected-parent-rate",
        type=float,
        default=0.25,
        help="chance to choose a young topology as a parent",
    )
    parser.add_argument(
        "--simulation-workers",
        type=int,
        default=1,
        help="processes used to simulate population members in each generation",
    )
    parser.add_argument("--period", type=int, default=250, help="sine input period")
    parser.add_argument(
        "--hold",
        type=int,
        default=10,
        help="samples each level is held: the step shape, and multi-channel random",
    )
    parser.add_argument(
        "--progress",
        type=int,
        default=0,
        help="report fitness every N generations (default: 20 reports total)",
    )
    parser.add_argument(
        "--graph",
        choices=("evolved", "reference"),
        default="evolved",
        help="which model to draw in the figure",
    )
    parser.add_argument(
        "--no-stateless-baseline",
        action="store_true",
        help=(
            "on a rung whose closed-form floor is not a valid criterion, skip "
            "the memoryless search and judge against the closed form anyway"
        ),
    )
    parser.add_argument("--output", default="benchmark_report.png")
    parser.add_argument("--no-show", action="store_true", help="save the figure without opening it")
    return parser


if __name__ == "__main__":
    raise SystemExit(main())
