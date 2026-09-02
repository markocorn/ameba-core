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
import sys
from concurrent.futures import ProcessPoolExecutor
from math import isfinite
from pathlib import Path
from random import Random
from time import perf_counter

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from ameba.benchmarks import (  # noqa: E402
    DEFAULT_NODE_PENALTY,
    FULL_OPERATORS,
    benchmark_crossover,
    benchmark_policy,
    dynamic_population,
    identification_dataset,
    linear,
    narendra,
    parameter_mutations,
    seed_graph,
    static_gain_floor,
    step_controls,
    structural_mutations,
    training_controls,
)
from ameba.equations import format_equations  # noqa: E402
from ameba_graph import Archive, ArchiveConfig, Graph  # noqa: E402
from ameba_graph.crossover import CrossoverError  # noqa: E402
from ameba_graph.model import GraphError  # noqa: E402
from ameba_graph.mutation import MutationError  # noqa: E402
from ameba_graph.serialization import graph_dumps  # noqa: E402
from ameba_signal import SignalEvaluator  # noqa: E402
from ameba_signal.stateful import STATEFUL_KINDS  # noqa: E402

BENCHMARKS = {
    "linear": (linear.linear_trajectory, "Third-order linear plant"),
    "narendra": (narendra.narendra_trajectory, "Narendra plant"),
}

_WORKER_EVALUATOR: SignalEvaluator | None = None


def _initialize_worker(evaluator: SignalEvaluator) -> None:
    global _WORKER_EVALUATOR
    _WORKER_EVALUATOR = evaluator


def _describe_in_worker(graph: Graph):
    if _WORKER_EVALUATOR is None:
        raise RuntimeError("Archive worker was not initialized")
    return _WORKER_EVALUATOR.describe(graph)


def main(argv: list[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    if args.capacity < 2:
        _parser().error("--capacity must be at least two")
    if args.batch < 1:
        _parser().error("--batch must be positive")
    if not 0.0 <= args.parameter_rate <= 1.0:
        _parser().error("--parameter-rate must be between zero and one")
    if not 0.0 <= args.crossover_rate <= 1.0:
        _parser().error("--crossover-rate must be between zero and one")

    trajectory_of, title = BENCHMARKS[args.benchmark]
    controls = training_controls(args.steps, Random(args.data_seed), args.amplitude)
    trajectory = trajectory_of(controls)
    dataset = identification_dataset(trajectory)
    floor = static_gain_floor(trajectory, args.criterion, args.time_step)
    evaluator = SignalEvaluator(
        dataset, criterion=args.criterion, time_step=args.time_step
    )
    policy = benchmark_policy(FULL_OPERATORS)
    parameter_ops = parameter_mutations()
    structural_ops = structural_mutations()
    crossover = benchmark_crossover()
    rng = Random(args.seed)

    archive = Archive(ArchiveConfig(
        capacity=args.capacity,
        probation=args.probation,
        threshold=args.threshold,
        threshold_quantile=args.threshold_quantile,
        novelty_admits=not args.require_improvement,
    ))

    print(f"\n{title}  --  behavioural archive search")
    print(f"  {args.steps} steps, {args.batches} batches x {args.batch} candidates")
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
    print(f"  memoryless floor: {floor:.6g}  (a model must score below this)")
    print(f"  workers: {args.workers}\n")

    seeds = _initial_graphs(args, dataset, policy)
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
    try:
        for graph in seeds:
            score, descriptor = evaluator.describe(graph)
            archive.insert(graph, _shaped(score, graph, args.node_penalty), descriptor)

        interval = args.progress or max(1, args.batches // 20)
        header = f"{'batch':>6}  {'best':>13}  {'/floor':>8}  {'held':>4}  {'thresh':>7}  {'new':>4}  {'tuned':>5}"
        print(header)
        _report(archive, 0, floor, 0, 0)
        for batch in range(1, args.batches + 1):
            candidates = [
                _candidate(archive, rng, policy, parameter_ops, structural_ops,
                           crossover, args)
                for _ in range(args.batch)
            ]
            scored = _score(candidates, evaluator, executor)
            admitted = tuned = 0
            for graph, (score, descriptor) in zip(candidates, scored):
                outcome = archive.insert(
                    graph, _shaped(score, graph, args.node_penalty), descriptor
                )
                if outcome.reason == "novel":
                    admitted += 1
                elif outcome.reason == "improved":
                    tuned += 1
            if batch % interval == 0 or batch == args.batches:
                _report(archive, batch, floor, admitted, tuned)
    finally:
        if executor is not None:
            executor.shutdown()

    _summary(archive, floor, evaluator, perf_counter() - started)
    if args.save_graph:
        Path(args.save_graph).write_text(graph_dumps(archive.best.graph))
        print(f"\n  best graph written to {args.save_graph}")
    return 0


def _initial_graphs(args, dataset, policy) -> list[Graph]:
    """Fill the archive from diverse executable architectures, or from the seed."""
    if args.initialization == "seed":
        return [seed_graph() for _ in range(args.capacity)]
    return dynamic_population(
        dataset,
        policy,
        args.capacity,
        args.seed + 1_000,
        args.initial_nodes_min,
        args.initial_nodes_max,
    )


def _candidate(archive, rng, policy, parameter_ops, structural_ops, crossover, args) -> Graph:
    """Derive one candidate from the archive.

    Parents are drawn uniformly, never by score. Biasing this toward the best
    member is exactly how the archive would collapse back onto the plateau it
    exists to escape.
    """
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


def _report(archive, batch: int, floor: float, admitted: int, tuned: int) -> None:
    best = archive.best
    print(
        f"{batch:>6}  {best.score:>13.6g}  {best.score / floor:>8.4f}  "
        f"{len(archive):>4}  {archive.threshold():>7.4f}  {admitted:>4}  {tuned:>5}"
    )


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
    parser.add_argument("--benchmark", choices=sorted(BENCHMARKS), default="narendra")
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
    parser.add_argument("--criterion", default="ise")
    parser.add_argument("--time-step", type=float, default=1.0)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--data-seed", type=int, default=7)
    parser.add_argument("--workers", type=int, default=1)
    parser.add_argument(
        "--node-penalty", type=float, default=0.0,
        help="fraction of its score each node costs; measured harmful here, default off",
    )
    parser.add_argument(
        "--save-graph", default=None, help="write the best graph as JSON",
    )
    parser.add_argument("--progress", type=int, default=0)
    return parser


if __name__ == "__main__":
    raise SystemExit(main())
