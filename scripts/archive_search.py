"""Search a benchmark with a behavioural archive instead of a population.

The population engine ranks every candidate on one number, so the plateau wins:
a structurally new model is judged against tuned incumbents while its own
weights are still whatever the mutation produced, loses, and is never seen
again. This driver keeps a small fixed archive of models that *fail
differently* -- compared by the Spearman correlation of their residuals -- and
lets each one compete only against its own neighbourhood.

Parameter mutation is not a refinement phase here. A retuned copy of a member
is behaviourally near that member, so it lands in the same niche and replaces
its own parent when it scores better. Local search is what the archive does to
itself between batches.

    python scripts/archive_search.py --benchmark narendra --batches 200
"""

from __future__ import annotations

import argparse
import json
import sys
from concurrent.futures import ProcessPoolExecutor
from math import isfinite
from pathlib import Path
from random import Random
from time import perf_counter

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from ameba.benchmarks import (  # noqa: E402
    BENCHMARKS,
    DEFAULT_NODE_PENALTY,
    PlantTrajectory,
    FULL_OPERATORS,
    benchmark_crossover,
    benchmark_policy,
    channel_training_controls,
    dynamic_population,
    memoryless_floor,
    parameter_mutations,
    seed_graph,
    static_seed_graph,
    stateless_floor,
    step_controls,
    structural_mutations,
    training_controls,
    trajectory_dataset,
    trajectory_floor,
)
from ameba.equations import format_equations  # noqa: E402
from ameba.plotting import draw_graph  # noqa: E402
from ameba_graph import (  # noqa: E402
    Archive,
    ArchiveConfig,
    GenerationConfig,
    Graph,
    GraphGenerationError,
    neighbourhood_profile,
    GraphGenerator,
)
from ameba_graph.crossover import CrossoverError  # noqa: E402
from ameba_graph.model import GraphError  # noqa: E402
from ameba_graph.mutation import MutationError  # noqa: E402
from ameba_graph.serialization import graph_dumps  # noqa: E402
from ameba_signal import (  # noqa: E402
    Dataset,
    SignalEvaluator,
    SignalSimulator,
    interface_scaffold,
)
from ameba_signal.serialization import dataset_from_dict  # noqa: E402
from ameba_signal.stateful import STATEFUL_KINDS  # noqa: E402

_WORKER_EVALUATOR: SignalEvaluator | None = None


def _initialize_worker(evaluator: SignalEvaluator) -> None:
    global _WORKER_EVALUATOR
    _WORKER_EVALUATOR = evaluator


def _describe_in_worker(graph: Graph):
    if _WORKER_EVALUATOR is None:
        raise RuntimeError("Archive worker was not initialized")
    return _WORKER_EVALUATOR.describe(graph)


def main(argv: list[str] | None = None) -> int:
    # Redirected to a file, stdout would otherwise block-buffer and show
    # nothing until the run ends -- useless for watching a long search.
    sys.stdout.reconfigure(line_buffering=True)
    args = _parser().parse_args(argv)
    if args.capacity < 2:
        _parser().error("--capacity must be at least two")
    if args.batch < 1:
        _parser().error("--batch must be positive")
    if not 0.0 <= args.parameter_rate <= 1.0:
        _parser().error("--parameter-rate must be between zero and one")
    if not 0.0 <= args.crossover_rate <= 1.0:
        _parser().error("--crossover-rate must be between zero and one")

    if args.time_step is None:
        args.time_step = BENCHMARKS[args.benchmark].time_step
    if args.dataset:
        dataset, closed_form, title = _measured_problem(args)
        needs_baseline = args.stateless_baseline
    else:
        benchmark = BENCHMARKS[args.benchmark]
        title = benchmark.title
        trajectory = benchmark.build(_benchmark_controls(args, benchmark.channels))
        dataset = trajectory_dataset(trajectory)
        closed_form = trajectory_floor(trajectory, args.criterion, args.time_step)
        needs_baseline = not benchmark.linear_floor_is_strict
    # A closed-form optimum is itself reachable with no state, so it is a
    # legitimate memoryless score, and a search that fails to reach it is
    # under-searched rather than reassuring. The stricter of the two is the bar.
    baseline = (
        stateless_floor(
            dataset,
            args.criterion,
            args.time_step,
            population_size=args.batch,
            generations=args.batches,
        )
        if needs_baseline
        else None
    )
    floor = closed_form if baseline is None else min(closed_form, baseline)
    evaluator = SignalEvaluator(
        dataset, criterion=args.criterion, time_step=args.time_step
    )
    policy = benchmark_policy(FULL_OPERATORS)
    parameter_ops = parameter_mutations()
    structural_ops = structural_mutations()
    crossover = benchmark_crossover()

    config = ArchiveConfig(
        capacity=args.capacity,
        probation=args.probation,
        threshold=args.threshold,
        threshold_quantile=args.threshold_quantile,
        threshold_min=args.threshold_min,
        threshold_max=args.threshold_max,
        novelty_admits=not args.require_improvement,
        size_tiebreak=args.size_tiebreak,
        local_competition=not args.no_local_competition,
        min_nodes=args.min_nodes,
        structure_rule=args.structure_rule,
    )

    print(f"\n{title}  --  behavioural archive search")
    print(f"  {len(dataset.inputs)} steps, {args.batches} batches x {args.batch} candidates")
    print(
        f"  archive: capacity {args.capacity}, probation {args.probation}, "
        + (
            f"fixed threshold {args.threshold:g}"
            if args.threshold is not None
            else f"adaptive threshold at quantile {args.threshold_quantile:g}"
        )
    )
    print(
        f"  novel candidates: "
        + ("must beat the worst member" if args.require_improvement else "admitted on novelty")
    )
    print(
        f"  operator mix: {args.parameter_rate:.0%} parameter, "
        f"{args.crossover_rate:.0%} crossover of the rest"
    )
    print(f"  node penalty: {args.node_penalty:g} of score per node")
    if baseline is None:
        print(f"  memoryless floor: {floor:.6g}  (a model must score below this)")
    else:
        print(f"  pass criterion: {floor:.6g}  (a model must score below this)")
        print(
            f"    closed-form floor {closed_form:.6g}; a memoryless search at this "
            f"budget reached {baseline:.6g} "
            f"({baseline / closed_form:.2f}x the form)"
        )
    print(f"  workers: {args.workers}\n")

    executor = (
        ProcessPoolExecutor(
            max_workers=args.workers,
            initializer=_initialize_worker,
            initargs=(evaluator,),
        )
        if args.workers > 1
        else None
    )
    started = perf_counter()
    results: list[tuple[int, Archive, list[float]]] = []
    try:
        for index in range(args.seeds):
            seed = args.seed + index
            if args.seeds > 1:
                print(f"\nSEED {seed}  ({index + 1}/{args.seeds})")
            archive, history = _search(
                args, seed, config, dataset, policy, evaluator, floor,
                parameter_ops, structural_ops, crossover, executor,
            )
            results.append((seed, archive, history))
            if args.seeds > 1:
                member = archive.best
                done = [item.best.score for _, item, _ in results]
                print(
                    f"  seed {seed} done: {member.score:.6g} "
                    f"({member.score / floor:.4f} x floor), "
                    f"{len(member.graph.nodes)} nodes, "
                    + ("below floor" if member.score < floor else "at/above floor")
                    + f"  |  {sum(1 for s in done if s < floor)}/{len(done)} below so far, "
                    f"best {min(done):.6g}, {_clock(perf_counter() - started)} elapsed"
                )
    finally:
        if executor is not None:
            executor.shutdown()

    best_seed, best_archive, best_history = min(
        results, key=lambda item: item[1].best.score
    )
    _summary(best_archive, floor, evaluator, perf_counter() - started)
    if args.seeds > 1:
        _seed_table(results, floor, best_seed)
    if args.save_graph:
        Path(args.save_graph).write_text(graph_dumps(best_archive.best.graph))
        print(f"\n  best graph written to {args.save_graph}")
    if args.figure:
        _render(args.figure, best_archive.best.graph, dataset, best_history,
                floor, evaluator, title)
    return 0


def _search(args, seed, config, dataset, policy, evaluator, floor,
            parameter_ops, structural_ops, crossover, executor) -> Archive:
    """Spend this seed's whole batch budget, restarting a run that has died.

    A stalled run does not recover: one was measured frozen on the memoryless
    floor for eleven thousand consecutive batches. Left alone it spends the
    budget proving that again. With ``--stall-patience`` the budget is instead
    re-spent from a fresh initialization, and the best archive across attempts
    is what the seed reports -- more attempts at the growth mode rather than a
    longer wait on a dead one.
    """
    best: Archive | None = None
    best_history: list[float] = []
    remaining = args.batches
    attempt = 0
    while remaining > 0:
        archive, used, history = _attempt(
            args, seed + attempt * 100_003, config, dataset, policy, evaluator,
            floor, parameter_ops, structural_ops, crossover, executor, remaining,
        )
        if best is None or archive.best.score < best.best.score:
            best, best_history = archive, history
        remaining -= used
        attempt += 1
        if attempt > 1 or used < args.batches:
            print(
                f"    attempt {attempt}: {archive.best.score:.6g} after {used} "
                f"batches; {remaining} left, best so far {best.best.score:.6g}"
            )
        if not args.stall_patience:
            break
    assert best is not None
    return best, best_history


def _attempt(args, seed, config, dataset, policy, evaluator, floor,
             parameter_ops, structural_ops, crossover, executor,
             budget: int) -> tuple[Archive, int, list[float]]:
    """One run from a fresh initialization, stopping early once it stalls."""
    rng = Random(seed)
    archive = Archive(config)
    for graph in _initial_graphs(args, seed, dataset, policy):
        score, descriptor = evaluator.describe(graph)
        archive.insert(
            graph, _shaped(score, graph, args.node_penalty), descriptor,
            _structure(graph, args),
        )

    started = perf_counter()
    interval = args.progress or max(1, args.batches // 40)
    print(
        f"{'batch':>7} {'best':>13} {'/floor':>8} {'held':>5} {'thresh':>7} "
        f"{'new':>4} {'tuned':>6} {'nodes':>6} {'elapsed':>8} {'eta':>8}"
    )
    _report(archive, 0, budget, floor, 0, 0, 0.0)
    immigrants = _immigrants(args, dataset, policy)
    stalled_since = archive.best.score
    stalled_for = 0
    history = [archive.best.score]
    for batch in range(1, budget + 1):
        candidates = [
            _candidate(archive, rng, policy, parameter_ops, structural_ops,
                       crossover, args, immigrants)
            for _ in range(args.batch)
        ]
        admitted = tuned = 0
        for graph, (score, descriptor) in zip(
            candidates, _score(candidates, evaluator, executor)
        ):
            outcome = archive.insert(
                graph, _shaped(score, graph, args.node_penalty), descriptor,
                _structure(graph, args),
            )
            if outcome.reason == "novel":
                admitted += 1
            elif outcome.reason in ("improved", "simplified"):
                tuned += 1
        history.append(archive.best.score)
        if batch % interval == 0 or batch == budget:
            _report(
                archive, batch, budget, floor, admitted, tuned,
                perf_counter() - started,
            )
        if args.stall_patience:
            if archive.best.score < stalled_since * (1.0 - args.stall_improvement):
                stalled_since = archive.best.score
                stalled_for = 0
            else:
                stalled_for += 1
                if stalled_for >= args.stall_patience:
                    return archive, batch, history
    return archive, budget, history


def _seed_table(results, floor: float, best_seed: int) -> None:
    """Per-seed outcomes, because the spread here is wider than most effects."""
    scores = sorted(archive.best.score for _, archive, _ in results)
    middle = len(scores) // 2
    median = (
        scores[middle]
        if len(scores) % 2
        else (scores[middle - 1] + scores[middle]) / 2.0
    )
    print(f"\n  across {len(results)} seeds:")
    print(f"    {'seed':>6}  {'best':>13}  {'/floor':>8}  {'nodes':>5}  verdict")
    for seed, archive, _ in results:
        member = archive.best
        print(
            f"    {seed:>6}  {member.score:>13.6g}  {member.score / floor:>8.4f}  "
            f"{len(member.graph.nodes):>5}  "
            + ("below floor" if member.score < floor else "at/above floor")
            + ("   <- best" if seed == best_seed else "")
        )
    below = sum(1 for _, archive, _ in results if archive.best.score < floor)
    print(
        f"    median {median:.6g} ({median / floor:.4f} x floor); "
        f"{below}/{len(results)} runs below the floor"
    )


def _render(path, graph, dataset, history, floor, evaluator, title) -> None:
    """Draw the winning model beside what it actually predicts.

    One panel per output channel, because a coupled plant is only judged
    honestly channel by channel: a model can track one output well and leave
    another untouched, and a single summed score hides that entirely.
    """
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    session = SignalSimulator().start(graph)
    modelled = [session.step(row) for row in dataset.inputs]
    channels = len(dataset.outputs[0])

    figure = plt.figure(figsize=(17, 3.0 * max(channels + 1, 3)))
    grid = figure.add_gridspec(channels + 1, 2, width_ratios=(1.15, 1.0))

    axes = figure.add_subplot(grid[:, 0])
    draw_graph(axes, graph)
    axes.set_title(
        f"{len(graph.nodes)} nodes, "
        f"{sum(1 for n in graph.nodes.values() if n.kind in STATEFUL_KINDS)} stateful",
        fontsize=10,
    )
    axes.axis("off")

    steps = range(len(dataset.inputs))
    for channel in range(channels):
        panel = figure.add_subplot(grid[channel, 1])
        measured = [row[channel] for row in dataset.outputs]
        predicted = [row[channel] for row in modelled]
        panel.plot(steps, measured, linewidth=1.4, label="plant")
        panel.plot(steps, predicted, linewidth=1.1, linestyle="--", label="model")
        panel.plot(
            steps, [m - p for m, p in zip(measured, predicted)],
            linewidth=0.8, alpha=0.55, label="error",
        )
        panel.axhline(0.0, linewidth=0.5, color="0.6")
        panel.set_ylabel(f"y{channel}", fontsize=9)
        panel.legend(fontsize=7, ncol=3, loc="upper right")
        panel.tick_params(labelsize=8)
        if channel == 0:
            panel.set_title(title, fontsize=10)

    progress = figure.add_subplot(grid[channels, 1])
    progress.plot(range(len(history)), history, linewidth=1.2)
    progress.axhline(floor, linestyle=":", linewidth=1.0, color="crimson")
    progress.set_yscale("log")
    progress.set_xlabel("batch", fontsize=9)
    progress.set_ylabel("best score", fontsize=9)
    progress.set_title(
        f"final {evaluator.evaluate(graph):.6g} against memoryless floor "
        f"{floor:.6g} (dotted)", fontsize=9,
    )
    progress.tick_params(labelsize=8)

    figure.tight_layout()
    figure.savefig(path, dpi=120)
    plt.close(figure)
    print(f"\n  figure written to {path}")


def _benchmark_controls(args, channels: int):
    """The excitation the search trains on, for either width of interface.

    A multi-channel benchmark gets a held input (``--hold``). The plants with
    more than one channel are the slow ones, and such a plant filters
    sample-to-sample noise out almost entirely, so an unheld sequence would
    carry no dynamics worth identifying.
    """
    if channels == 1:
        return training_controls(args.steps, Random(args.data_seed), args.amplitude)
    return channel_training_controls(
        args.steps, channels, Random(args.data_seed), args.amplitude, args.hold
    )


def _measured_problem(args):
    """Identify a plant from recorded data rather than a simulated benchmark.

    Everything downstream of here already works from a dataset, so pointing the
    search at measured input/output rows needs nothing but the file, at any
    number of channels. The pass criterion travels with it: the memoryless
    floor is the best static linear map from the inputs at one step to the
    outputs at the next, so a model only demonstrates dynamics by beating it.
    """
    dataset = dataset_from_dict(json.loads(Path(args.dataset).read_text()))
    if not dataset.inputs or not dataset.outputs:
        raise SystemExit(f"{args.dataset} contains no rows")
    title = (
        f"{Path(args.dataset).name}  ({len(dataset.inputs)} rows, "
        f"{len(dataset.inputs[0])} in / {len(dataset.outputs[0])} out)"
    )
    return dataset, memoryless_floor(dataset, args.criterion, args.time_step), title


def _immigrants(args, dataset, policy):
    """A source of fresh random graphs, or None when immigration is off.

    Candidates are screened for executability on a short probe of the dataset,
    the same way the initial archive is built. An immigrant that raises in the
    simulator carries no descriptor, so the archive could only throw it away --
    better to spend the attempts here than a slot in the batch.
    """
    if not args.immigrant_rate:
        return None
    generator = GraphGenerator(
        policy,
        GenerationConfig(
            min_nodes=args.initial_nodes_min,
            max_nodes=args.initial_nodes_max,
            edge_probability=0.18,
            attempts=500,
        ),
    )
    scaffold = interface_scaffold(len(dataset.inputs[0]), len(dataset.outputs[0]))
    probe = SignalEvaluator(Dataset(dataset.inputs[:30], dataset.outputs[:30]))

    def generate(rng: Random) -> Graph | None:
        try:
            return generator.generate(
                rng, scaffold, accept=lambda graph: isfinite(probe.evaluate(graph))
            )
        except (GraphGenerationError, GraphError, ValueError):
            return None

    return generate


def _initial_graphs(args, seed: int, dataset, policy) -> list[Graph]:
    """Fill the archive from diverse executable architectures, or from the seed."""
    if args.initialization == "seed":
        interface = (len(dataset.inputs[0]), len(dataset.outputs[0]))
        return [static_seed_graph(*interface) for _ in range(args.capacity)]
    return dynamic_population(
        dataset,
        policy,
        args.capacity,
        seed + 1_000,
        args.initial_nodes_min,
        args.initial_nodes_max,
    )


def _candidate(archive, rng, policy, parameter_ops, structural_ops, crossover,
               args, immigrants=None) -> Graph:
    """Derive one candidate from the archive, or import an unrelated one.

    Parents are drawn uniformly, never by score. Biasing this toward the best
    member is exactly how the archive would collapse back onto the plateau it
    exists to escape.

    Every other operator here descends from something the archive already
    holds, so once its members stop offering new structure the search has no
    way to invent any -- which is what a stalled run looks like. An immigrant
    is generated from nothing and is almost certainly behaviourally distant, so
    it enters as a new niche, takes probation, and is tuned in place from there.
    """
    if immigrants is not None and rng.random() < args.immigrant_rate:
        immigrant = immigrants(rng)
        if immigrant is not None:
            return immigrant
    members = archive.members
    parent = rng.choice(members)
    graph = parent.graph.copy()
    try:
        if rng.random() < args.parameter_rate:
            return rng.choice(parameter_ops).mutate(graph, policy, rng)
        if len(members) > 1 and rng.random() < args.crossover_rate:
            partner = rng.choice([item for item in members if item is not parent])
            graph = crossover.cross(graph, partner.graph, policy, rng)
        return rng.choice(structural_ops).mutate(graph, policy, rng)
    except (MutationError, CrossoverError, GraphError, ValueError):
        # A rejected operation still costs a slot in the batch; the archive
        # will simply score the untouched copy and find it is not an
        # improvement on the parent it came from.
        return parent.graph.copy()


def _structure(graph: Graph, args):
    """What the graph is made of and how it is wired, or None when unused."""
    if args.structure_rule == "off":
        return None
    return neighbourhood_profile(graph, args.structure_depth)


def _shaped(score: float, graph: Graph, node_weight: float) -> float:
    """Charge a candidate a fraction of its own score per node.

    Nothing in an error criterion argues against size, so an archive left to
    itself accumulates neutral structure -- a long run here reached sixty nodes
    for a model whose useful part was a fraction of that. The charge is
    relative, matching the population engine, so it stays below the local
    gradient instead of vetoing real improvements once the score falls.
    """
    if node_weight == 0.0 or not isfinite(score):
        return score
    return score + node_weight * len(graph.nodes) * abs(score)


def _score(candidates, evaluator, executor):
    if executor is None:
        return [evaluator.describe(graph) for graph in candidates]
    return list(executor.map(_describe_in_worker, candidates))


def _report(
    archive, batch: int, total: int, floor: float,
    admitted: int, tuned: int, elapsed: float,
) -> None:
    best = archive.best
    remaining = (elapsed / batch) * (total - batch) if batch else 0.0
    print(
        f"{batch:>7} {best.score:>13.6g} {best.score / floor:>8.4f} "
        f"{len(archive):>5} {archive.threshold():>7.4f} {admitted:>4} {tuned:>6} "
        f"{len(best.graph.nodes):>6} {_clock(elapsed):>8} {_clock(remaining):>8}"
    )


def _clock(seconds: float) -> str:
    if seconds < 60:
        return f"{seconds:.0f}s"
    if seconds < 3600:
        return f"{int(seconds) // 60}m{int(seconds) % 60:02d}s"
    return f"{int(seconds) // 3600}h{(int(seconds) % 3600) // 60:02d}m"


def _summary(archive, floor: float, evaluator, seconds: float) -> None:
    best = archive.best
    stateful = [
        node for node in best.graph.nodes.values() if node.kind in STATEFUL_KINDS
    ]
    print(f"\n  finished in {seconds:.1f}s after {archive.insertions} insertions")
    print(f"  best {best.score:.10g}   floor {floor:.6g}   ratio {best.score / floor:.4f}")
    print(
        "  verdict: "
        + (
            "BELOW THE FLOOR -- the model is using state"
            if best.score < floor
            else "at or above the floor -- no dynamics learned"
        )
    )
    print(f"  best graph: {len(best.graph.nodes)} nodes, {len(stateful)} stateful, "
          f"tuned {best.improvements} times in place")

    print("\n  archive, best first:")
    print(f"    {'score':>13}  {'/floor':>8}  {'nodes':>5}  {'state':>5}  {'tuned':>5}")
    for member in archive.ranked():
        memory = sum(
            node.kind in STATEFUL_KINDS for node in member.graph.nodes.values()
        )
        print(
            f"    {member.score:>13.6g}  {member.score / floor:>8.4f}  "
            f"{len(member.graph.nodes):>5}  {memory:>5}  {member.improvements:>5}"
        )

    print("\n  best model equations:")
    for line in format_equations(best.graph).splitlines():
        print(f"    {line}")


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--benchmark", choices=sorted(BENCHMARKS), default="narendra"
    )
    parser.add_argument(
        "--stateless-baseline",
        action="store_true",
        help=(
            "find the pass criterion by memoryless search as well as closed "
            "form; automatic for a benchmark whose closed-form floor is known "
            "not to hold, and needed explicitly for --dataset"
        ),
    )
    parser.add_argument(
        "--dataset", default=None,
        help="identify a plant from recorded input/output rows instead of a "
             "simulated benchmark; overrides --benchmark",
    )
    parser.add_argument("--batches", type=int, default=200)
    parser.add_argument(
        "--batch", type=int, default=16,
        help="candidates generated and evaluated together each batch",
    )
    parser.add_argument("--capacity", type=int, default=10, help="models held at once")
    parser.add_argument(
        "--probation", type=int, default=5,
        help="insertions a newly admitted model cannot be evicted for",
    )
    parser.add_argument(
        "--threshold", type=float, default=None,
        help="fixed same-idea distance; omit to adapt from the archive itself",
    )
    parser.add_argument("--threshold-quantile", type=float, default=0.25)
    parser.add_argument(
        "--threshold-min", type=float, default=0.0,
        help="floor under the adaptive threshold, so it cannot collapse until "
             "every candidate reads as novel and no niche consolidates",
    )
    parser.add_argument(
        "--threshold-max", type=float, default=2.0,
        help="cap over the adaptive threshold, so it cannot pin high and admit "
             "no new structure at all",
    )
    parser.add_argument(
        "--require-improvement", action="store_true",
        help="make novel candidates also beat the member they displace",
    )
    parser.add_argument(
        "--parameter-rate", type=float, default=0.5,
        help="share of candidates that are parameter-only retunings",
    )
    parser.add_argument("--crossover-rate", type=float, default=0.4)
    parser.add_argument(
        "--initialization", choices=("dynamic", "seed"), default="dynamic",
    )
    parser.add_argument("--initial-nodes-min", type=int, default=10)
    parser.add_argument("--initial-nodes-max", type=int, default=14)
    parser.add_argument("--steps", type=int, default=150, help="simulated time steps; matches benchmark_report")
    parser.add_argument("--amplitude", type=float, default=1.0)
    parser.add_argument(
        "--hold",
        type=int,
        default=10,
        help="samples each level is held, for multi-channel benchmarks",
    )
    parser.add_argument("--criterion", default="ise")
    parser.add_argument(
        "--time-step",
        type=float,
        default=None,
        help="sample time dt (default: the benchmark's own sample time)",
    )
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--data-seed", type=int, default=7)
    parser.add_argument("--workers", type=int, default=1)
    parser.add_argument(
        "--node-penalty", type=float, default=0.0,
        help="fraction of its score each node costs; measured harmful here, default off",
    )
    parser.add_argument(
        "--seeds", type=int, default=1,
        help="independent runs at consecutive seeds; the spread is wide, so "
             "any config comparison needs several",
    )
    parser.add_argument(
        "--size-tiebreak", type=float, default=0.0,
        help="within a niche, prefer a smaller graph scoring within this "
             "relative tolerance; parsimony that cannot reach the plateau",
    )
    parser.add_argument(
        "--no-local-competition", action="store_true",
        help="drop same-niche candidates instead of letting them replace their "
             "neighbour; ablates in-place tuning",
    )
    parser.add_argument(
        "--structure-rule", choices=("off", "max", "mean", "only"), default="off",
        help="also compare candidates by what they are made of, not only how "
             "they fail; 'max' calls two things one idea only if both agree",
    )
    parser.add_argument(
        "--structure-depth", type=int, default=1,
        help="neighbourhood rounds in the structural label; 0 is a bag of "
             "kinds, 2 over-discriminates on graphs this size",
    )
    parser.add_argument(
        "--immigrant-rate", type=float, default=0.0,
        help="share of candidates generated from scratch rather than derived "
             "from an archive member; the only source of genuinely new structure",
    )
    parser.add_argument(
        "--stall-patience", type=int, default=0,
        help="abandon and restart a run whose best has not improved for this "
             "many batches; a stalled run was measured never to recover",
    )
    parser.add_argument(
        "--stall-improvement", type=float, default=1e-4,
        help="relative gain that counts as progress against --stall-patience",
    )
    parser.add_argument(
        "--min-nodes", type=int, default=0,
        help="refuse candidates smaller than this; size predicts success here, "
             "so the useful guard is a floor rather than a penalty",
    )
    parser.add_argument(
        "--save-graph", default=None, help="write the best graph as JSON",
    )
    parser.add_argument(
        "--figure", default=None,
        help="write a PNG showing the winning model, its prediction per output "
             "channel, and the search progress",
    )
    parser.add_argument("--progress", type=int, default=0)
    return parser


if __name__ == "__main__":
    raise SystemExit(main())
