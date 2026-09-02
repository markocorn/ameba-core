"""Run an identification benchmark and report what evolution found.

    python scripts/benchmark_report.py                    # linear plant
    python scripts/benchmark_report.py --benchmark narendra
    python scripts/benchmark_report.py --generations 300 --population 24

Prints a scored summary and the evolved model's difference equations, then
opens a figure with the signals, the model graph, and the equations.
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
    DEFAULT_NODE_PENALTY,
    FULL_OPERATORS,
    benchmark_engine,
    benchmark_policy,
    dynamic_population,
    evaluation_controls,
    identification_dataset,
    linear,
    narendra,
    seed_graph,
    static_gain_floor,
    step_controls,
    training_controls,
)
from ameba.equations import format_equations, graph_to_equations  # noqa: E402
from ameba.plotting import build_layout, draw_graph  # noqa: E402
from ameba_graph import Graph, OscillatingParsimony, RefinementConfig  # noqa: E402
from ameba_signal import (  # noqa: E402
    CRITERIA,
    SignalEvaluator,
    SignalGraphPolicy,
    SignalSimulationError,
    SignalSimulator,
)
from ameba_signal.stateful import CYCLE_BREAKER_KINDS, STATEFUL_KINDS  # noqa: E402
from ameba_signal.operators import OPERATOR_GROUPS  # noqa: E402

from random import Random  # noqa: E402

BENCHMARKS = {
    "linear": (linear.linear_trajectory, linear.reference_graph, "Third-order linear plant"),
    "narendra": (narendra.narendra_trajectory, narendra.reference_graph, "Narendra plant"),
}

KIND_COLORS = {
    "input": "#059669",
    "output": "#dc2626",
    "constant": "#6b7280",
    "delay": "#d97706",
}
STATEFUL_COLOR = "#ea580c"
DEFAULT_COLOR = "#2563eb"


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
    trajectory_of, reference_of, title = BENCHMARKS[args.benchmark]

    island_runs = _island_runs(args, trajectory_of)
    trajectory = island_runs[0][1]
    if not all(_bounded(item) for _, item, _ in island_runs):
        print(
            f"\nThe {args.benchmark} plant diverges at amplitude {args.amplitude:g}.\n"
            "  It is only stable for bounded inputs; try a smaller --amplitude.\n"
            "  (the Narendra plant goes unstable somewhere above 1.6)",
            file=sys.stderr,
        )
        return 1
    dataset = identification_dataset(trajectory)
    island_datasets = tuple(item for _, _, item in island_runs)
    diverse_signals = args.island_signals == "diverse" and args.islands > 1
    island_score_scales = (
        tuple(
            static_gain_floor(item, args.criterion, args.time_step)
            for _, item, _ in island_runs
        )
        if diverse_signals
        else None
    )
    evaluator = SignalEvaluator(
        dataset, criterion=args.criterion, time_step=args.time_step
    )
    reference = reference_of()
    seed = seed_graph()

    floor = static_gain_floor(trajectory, args.criterion, args.time_step)

    print(
        f"\n{title}  --  {args.steps} steps, {args.generations} generations, "
        f"{('diverse island inputs' if args.island_signals == 'diverse' and args.islands > 1 else args.input + ' input')} "
        f"at amplitude {args.amplitude:g}, {args.criterion.upper()} fitness"
    )
    refinement = _refinement(args)
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
        "  refinement: "
        + (
            f"min {refinement.min_steps}, patience {refinement.patience}, "
            f"max {refinement.max_steps}"
            if refinement
            else "off"
        )
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
        for index, ((name, policy), (signal, _, _)) in enumerate(
            zip(_island_policies(args), island_runs), 1
        ):
            print(
                f"    {index}. {name} | {signal}: "
                f"{', '.join(policy.evolvable_kinds)}"
            )
    print(
        f"  {args.steps} steps x dt {args.time_step:g} = {args.steps * args.time_step:g}"
        f" simulated time;  response range"
        f" [{min(trajectory.responses):+.3f}, {max(trajectory.responses):+.3f}]"
    )
    print("=" * 78)
    initialization = args.initialization
    if initialization == "auto":
        initialization = "dynamic" if args.benchmark == "narendra" else "seed"
    print(f"  initialization: {initialization}; restarts: {args.restarts}")

    result = None
    best_item = None
    history: list[float] = []
    for restart in range(args.restarts):
        run_seed = args.seed + restart
        policy = benchmark_policy(_operators(args))
        profiles = _island_policies(args)
        engine = benchmark_engine(
            dataset,
            policy=policy,
            seed=run_seed,
            population_size=args.population,
            criterion=args.criterion,
            time_step=args.time_step,
            node_penalty=args.node_penalty,
            refine=_refinement(args),
            topology_protection_generations=args.protect_topologies,
            topology_protection_size=protection_size,
            topology_parent_rate=protection_rate,
            simulation_workers=args.simulation_workers,
            island_count=args.islands,
            migration_interval=args.migration_interval if args.islands > 1 else 0,
            migration_size=args.migration_size if args.islands > 1 else 0,
            island_policies=tuple(profile for _, profile in profiles),
            island_datasets=island_datasets,
            island_score_scales=island_score_scales,
            complexity_schedule=schedule,
            island_exchange=args.island_exchange,
        )
        if initialization == "dynamic":
            population = []
            for island, (_, island_policy) in enumerate(profiles):
                island_dataset = island_datasets[island]
                if set(island_policy.evolvable_kinds) & STATEFUL_KINDS:
                    population.extend(dynamic_population(
                        island_dataset,
                        island_policy,
                        island_size,
                        run_seed + 1_000 + island,
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
                engine, population, args, 1.0 if diverse_signals else floor
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

    # The search optimises accuracy plus a size penalty, but the floor is a
    # pure accuracy figure, so the verdict must use the unpenalised error.
    true_error = evaluator.evaluate(best)
    scores = {
        "seed (y = u)": evaluator.evaluate(seed),
        "memoryless floor": floor,
        "evolved (accuracy)": true_error,
        (
            "evolved (local shaped ratio)"
            if diverse_signals
            else "evolved (with penalty)"
        ): best_item.score,
        "all-signal floor ratio": _aggregate_score(best, island_runs, args),
        "exact reference": evaluator.evaluate(reference),
    }
    signals = _signals(trajectory, best)
    _print_scores(scores, floor, best, signals, args.criterion)
    _print_graph(best)

    print("\nEVOLVED MODEL EQUATIONS")
    print("-" * 78)
    print(format_equations(best))
    print("\nTRUE PLANT EQUATIONS (for comparison)")
    print("-" * 78)
    print(format_equations(reference))

    drawn = reference if args.graph == "reference" else best
    _figure(args, title, signals, drawn, scores, floor, history)
    return 0


def _operators(args) -> tuple[str, ...]:
    if args.operators.strip() == "all":
        return FULL_OPERATORS
    return tuple(name.strip() for name in args.operators.split(",") if name.strip())


def _island_policies(args) -> list[tuple[str, SignalGraphPolicy]]:
    """Create deterministic signal-search niches, cycling for extra islands."""
    allowed = tuple(_operators(args))
    if args.islands == 1:
        return [("unrestricted", benchmark_policy(allowed))]
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
    return all(isfinite(value) and abs(value) < 1e6 for value in trajectory.responses)


def _controls(args) -> tuple[float, ...]:
    if args.input == "sine":
        return evaluation_controls(args.steps, args.amplitude, args.period)
    if args.input == "step":
        return step_controls(args.steps, args.hold, args.amplitude)
    return training_controls(args.steps, Random(args.data_seed), args.amplitude)


def _island_runs(args, trajectory_of) -> list[tuple[str, object, object]]:
    """Build one deterministic training trajectory per island."""
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
        (label, trajectory := trajectory_of(values), identification_dataset(trajectory))
        for label, values in controls
    ]


def _aggregate_score(graph: Graph, island_runs, args) -> float:
    """Mean error relative to each signal's memoryless floor."""
    ratios = []
    for _, trajectory, dataset in island_runs:
        error = SignalEvaluator(
            dataset, criterion=args.criterion, time_step=args.time_step
        ).evaluate(graph)
        floor = static_gain_floor(trajectory, args.criterion, args.time_step)
        ratios.append(error / max(abs(floor), 1e-12))
    return sum(ratios) / len(ratios)


def _evolve(engine, population: list[Graph], args, floor: float):
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
    raw_label = (
        "raw ratio"
        if args.island_signals == "diverse" and args.islands > 1
        else "raw accuracy"
    )
    print(
        f"{'gen':>6}  {'event':<9} {'fitness':>14} {raw_label:>14}  "
        f"{'winner':<20} {'mean':>12} {'nodes':>5}"
    )
    if args.islands > 1:
        print("        island change is measured since the prior report; negative is improvement")
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
        f"   (memoryless floor {floor:.6g})"
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
    raw_best = min(
        result.population,
        key=lambda item: item.raw_score if item.raw_score is not None else item.score,
    )
    raw_accuracy = raw_best.raw_score if raw_best.raw_score is not None else raw_best.score
    if not detailed:
        if exchange:
            ring = " -> ".join(f"I{index + 1}" for index in range(args.islands)) + " -> I1"
            winner = _winning_island(result)
            print(
                f"{generation:>6}  {_exchange_event(args):<9} {result.best.score:>14.6g} "
                f"{raw_accuracy:>14.6g}  "
                f"I{winner + 1} {names[winner]:<17} {'-':>12} {'-':>5}"
                f"  ({args.migration_size} each: {ring}; {_pressure(schedule, generation)})",
                flush=True,
            )
            _print_exchange_stats(engine)
        return previous

    population = result.population
    finite = [item.score for item in population if item.score < float("inf")]
    mean = sum(finite) / len(finite) if finite else float("inf")
    winner = _winning_island(result)
    event = "REPORT+X" if exchange else "REPORT"
    print(
        f"{generation:>6}  {event:<9} {result.best.score:>14.6g} "
        f"{raw_accuracy:>14.6g}  "
        f"I{winner + 1} {names[winner]:<17} {mean:>12.6g} "
        f"{len(result.best.graph.nodes):>5}",
        flush=True,
    )
    if schedule is not None:
        print(f"        complexity pressure: {_pressure(schedule, generation)}", flush=True)
    elapsed = perf_counter() - started
    rate = generation / elapsed if generation and elapsed > 0 else 0.0
    eta = (total - generation) / rate if rate > 0 else 0.0
    print(
        f"        runtime: {_duration(elapsed)} elapsed; {rate:.3f} gen/s; "
        f"ETA {_duration(eta)}",
        flush=True,
    )
    for index, island in enumerate(result.islands):
        finite_scores = [item.score for item in island if item.score < float("inf")]
        island_mean = (
            sum(finite_scores) / len(finite_scores) if finite_scores else float("inf")
        )
        best = island[0]
        raw_item = min(
            island,
            key=lambda item: item.raw_score if item.raw_score is not None else item.score,
        )
        raw = raw_item.raw_score if raw_item.raw_score is not None else raw_item.score
        node_counts = [len(item.graph.nodes) for item in island]
        stateful = sum(
            node.kind in STATEFUL_KINDS for node in best.graph.nodes.values()
        )
        nonlinear = sum(
            node.kind in OPERATOR_GROUPS["nonlinear"]
            for node in best.graph.nodes.values()
        )
        topologies = len({_topology_profile(item.graph) for item in island})
        change = "first" if previous is None else f"{island_bests[index] - previous[index]:+.3g}"
        print(
            f"        I{index + 1} {names[index]:<17} fit={best.score:<12.6g} "
            f"raw={raw:<12.6g} "
            f"change={change:>9} mean={island_mean:<12.6g} "
            f"nodes={len(best.graph.nodes):<3} avg={sum(node_counts) / len(node_counts):.1f} "
            f"range={min(node_counts)}..{max(node_counts)} memory={stateful:<2} "
            f"nonlinear={nonlinear:<2} topologies={topologies}/{len(island)} "
            f"finite={len(finite_scores)}/{len(island)}",
            flush=True,
        )
    if exchange:
        ring = " -> ".join(f"I{index + 1}" for index in range(args.islands)) + " -> I1"
        print(
            f"        {args.island_exchange}: {args.migration_size} pairing(s) around {ring}",
            flush=True,
        )
        _print_exchange_stats(engine)
    return island_bests


def _winning_island(result) -> int:
    return min(range(len(result.islands)), key=lambda index: result.islands[index][0].score)


def _pressure(schedule, generation: int) -> str:
    if schedule is None:
        return "fixed"
    return f"{schedule.phase_at(generation)} @ {schedule.node_weight_at(generation):g}"


def _exchange_event(args) -> str:
    return "CROSS" if args.island_exchange == "crossover" else "MIGRATE"


def _print_exchange_stats(engine) -> None:
    if not engine.last_exchange_stats:
        return
    values = "  ".join(
        f"I{index + 1}={accepted}/{attempted}"
        for index, (attempted, accepted) in enumerate(engine.last_exchange_stats)
    )
    print(f"        exchange children accepted: {values}", flush=True)


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
    scores: dict[str, float], floor: float, best: Graph, signals: dict, criterion: str
) -> None:
    print(f"\nFITNESS ({criterion.upper()}, lower is better)")
    print("-" * 78)
    width = max(len(name) for name in scores)
    for name, value in scores.items():
        print(f"  {name.ljust(width)}   {value:>14.6g}")

    if "model" in signals:
        fit = fit_percentage(signals["plant"], signals["model"])
        residual = [a - b for a, b in zip(signals["plant"], signals["model"])]
        rmse = (sum(value * value for value in residual) / len(residual)) ** 0.5
        print(f"\n  open-loop fit    {fit:>13.2f} %   (100 = exact, 0 = no better than the mean)")
        print(f"  residual RMSE    {rmse:>13.6g}")
        print(f"  residual peak    {max(abs(value) for value in residual):>13.6g}")

    stateful = [node for node in best.nodes.values() if node.kind in STATEFUL_KINDS]
    beat = scores["evolved (accuracy)"] < floor
    print("\nVERDICT")
    print("-" * 78)
    print(f"  below memoryless floor : {'YES' if beat else 'NO'}")
    print(f"  stateful nodes found   : {len(stateful)}"
          + (f"  ({', '.join(sorted(node.kind for node in stateful))})" if stateful else ""))
    if beat and stateful:
        print("  -> the model represents dynamics; it is not just fitting a gain.")
    else:
        print("  -> no dynamics identified; this is the best static gain on u(k).")


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


def _signals(trajectory, best: Graph) -> dict[str, list[float]]:
    """Open-loop simulation: the model sees only u(k), never the true output."""
    rows = [(control,) for control in trajectory.controls]
    signals = {
        "input": list(trajectory.controls),
        "plant": list(trajectory.responses),
    }
    try:
        signals["model"] = [row[0] for row in SignalSimulator().run_series(best, rows)]
    except SignalSimulationError as exc:
        print(f"  (the evolved model could not be simulated: {exc})")
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





def _figure(
    args, title: str, signals: dict, graph: Graph, scores: dict, floor: float, history: list
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

    fit = fit_percentage(signals["plant"], signals["model"]) if "model" in signals else 0.0
    figure = plt.figure(figsize=(14, sum(heights) + 1.0))
    grid = figure.add_gridspec(len(heights), 1, height_ratios=heights, hspace=0.55)
    figure.suptitle(
        f"{title}  --  open-loop identification   fit {fit:.1f}%   "
        f"{args.criterion.upper()} {scores['evolved (accuracy)']:.4g}"
        f"  (memoryless floor {floor:.4g})",
        fontsize=13,
    )

    time = [step * args.time_step for step in range(len(signals["plant"]))]
    label = "time" if args.time_step != 1.0 else "step k"
    _plot_input(figure.add_subplot(grid[0]), signals, time)
    _plot_comparison(figure.add_subplot(grid[1]), signals, time)
    _plot_error(figure.add_subplot(grid[2]), signals, time, label)
    _plot_fitness(figure.add_subplot(grid[3]), history, floor, args.criterion)
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


def _plot_input(axes, signals: dict[str, list[float]], time: list[float]) -> None:
    axes.plot(time, signals["input"], color="#6b7280", linewidth=0.9)
    axes.set_title("Excitation u(k)", loc="left", fontsize=10)
    axes.set_ylabel("u")
    axes.grid(alpha=0.25)
    axes.tick_params(labelbottom=False)


def _plot_comparison(axes, signals: dict[str, list[float]], time: list[float]) -> None:
    axes.plot(time, signals["plant"], color="#111827", linewidth=2.0, label="plant (measured)")
    if "model" in signals:
        fit = fit_percentage(signals["plant"], signals["model"])
        axes.plot(
            time,
            signals["model"],
            color="#dc2626",
            linewidth=1.3,
            label=f"AMEBA model (fit {fit:.1f}%)",
        )
    axes.set_title(
        "Open-loop response: measured vs identified model", loc="left", fontsize=11
    )
    axes.set_ylabel("y(k+1)")
    axes.grid(alpha=0.25)
    axes.legend(loc="upper right", fontsize=9)
    axes.tick_params(labelbottom=False)


def _plot_error(axes, signals: dict[str, list[float]], time: list[float], label: str) -> None:
    if "model" not in signals:
        axes.axis("off")
        return
    residual = [a - b for a, b in zip(signals["plant"], signals["model"])]
    peak = max(abs(value) for value in residual) or 1.0
    axes.axhline(0.0, color="#9ca3af", linewidth=0.8)
    axes.plot(time, residual, color="#b91c1c", linewidth=0.9)
    axes.fill_between(time, residual, color="#dc2626", alpha=0.25)
    rmse = (sum(value * value for value in residual) / len(residual)) ** 0.5
    axes.set_title(
        f"Residual  (RMSE {rmse:.4g}, peak {peak:.4g})", loc="left", fontsize=10
    )
    axes.set_xlabel(label)
    axes.set_ylabel("error")
    axes.grid(alpha=0.25)


def _plot_fitness(axes, history: list[float], floor: float, criterion: str) -> None:
    finite = [value for value in history if value < float("inf")]
    axes.plot(history, color="#dc2626", linewidth=1.6, label="best fitness")
    axes.axhline(
        floor,
        color="#2563eb",
        linestyle="--",
        linewidth=1.2,
        label=f"memoryless floor ({floor:.3g})",
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
        help="initial population; auto uses dynamic graphs for Narendra",
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
        default=1.0,
        help="sample time dt; total simulated time is steps * dt",
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
    parser.add_argument("--hold", type=int, default=10, help="step input hold length")
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
    parser.add_argument("--output", default="benchmark_report.png")
    parser.add_argument("--no-show", action="store_true", help="save the figure without opening it")
    return parser


if __name__ == "__main__":
    raise SystemExit(main())
