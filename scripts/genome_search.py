"""Search a benchmark with a classical GA over integer genomes.

The model is a flat sequence of gene IDs from a fixed dictionary; a total,
deterministic decoder turns it into a signal graph, and mutation and crossover
act on the integers only. Scored, floored, and judged exactly as the other two
search methods are, so the three are comparable on the same rung.

    python scripts/genome_search.py --benchmark linear --generations 300
"""

from __future__ import annotations

import argparse
import json
import sys
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path
from random import Random
from time import perf_counter

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from ameba.benchmarks import (  # noqa: E402
    BENCHMARKS,
    channel_training_controls,
    stateless_floor,
    training_controls,
    trajectory_dataset,
    trajectory_floor,
)
from ameba.equations import format_equations  # noqa: E402
from ameba.plotting import draw_graph  # noqa: E402
from ameba_genome import (  # noqa: E402
    CODEBOOKS,
    DEFAULT_MUTATION_WEIGHTS,
    DICTIONARY_VERSION,
    GAConfig,
    GeneticAlgorithm,
    GenomeFitness,
    Grammar,
    describe,
    label,
    model_size,
    mutate,
    numeric_mutate,
    one_point,
    random_genome,
    splice,
)
from ameba_graph import Graph  # noqa: E402
from ameba_graph.serialization import graph_to_dict  # noqa: E402
from ameba_signal import SignalEvaluator, SignalSimulator  # noqa: E402
from ameba_signal.operators import EVOLVABLE_OPERATORS, operators_in  # noqa: E402
from ameba_signal.stateful import STATEFUL_KINDS  # noqa: E402

_WORKER_EVALUATOR: SignalEvaluator | None = None


def _initialize_worker(evaluator: SignalEvaluator) -> None:
    global _WORKER_EVALUATOR
    _WORKER_EVALUATOR = evaluator


def _evaluate_in_worker(graph: Graph) -> float:
    if _WORKER_EVALUATOR is None:
        raise RuntimeError("Genome worker was not initialized")
    return _WORKER_EVALUATOR.evaluate(graph)


def main(argv: list[str] | None = None) -> int:
    sys.stdout.reconfigure(line_buffering=True)
    args = _parser().parse_args(argv)
    benchmark = BENCHMARKS[args.benchmark]
    if args.time_step is None:
        args.time_step = benchmark.time_step

    trajectory = benchmark.build(_controls(args, benchmark.channels))
    dataset = trajectory_dataset(trajectory)
    closed_form = trajectory_floor(trajectory, args.criterion, args.time_step)
    needs_baseline = args.stateless_baseline or not benchmark.linear_floor_is_strict
    baseline = (
        stateless_floor(dataset, args.criterion, args.time_step) if needs_baseline else None
    )
    # As in the other drivers: the closed form is itself reachable without
    # state, so the stricter of the two is the bar.
    floor = closed_form if baseline is None else min(closed_form, baseline)

    kinds = operators_in(*args.kinds) if args.kinds else EVOLVABLE_OPERATORS
    grammar = Grammar(len(dataset.inputs[0]), len(dataset.outputs[0]), kinds)
    codebook = CODEBOOKS[args.codebook]()
    if args.variation == "context" and args.codebook != "ordered":
        raise SystemExit("context variation works on dictionary IDs; use --codebook ordered")
    evaluator = SignalEvaluator(dataset, criterion=args.criterion, time_step=args.time_step)
    weights = dict(DEFAULT_MUTATION_WEIGHTS)
    if args.no_insert_record:
        weights["insert_record"] = 0.0
    config = GAConfig(
        population_size=args.population,
        elite_size=args.elite,
        tournament_size=args.tournament,
        crossover_rate=args.crossover_rate,
        max_genes=args.max_genes,
        shorter_wins_ties=not args.neutral_drift,
    )

    print(f"{benchmark.title}  --  {len(dataset.inputs)} steps, "
          f"{args.generations} generations x {args.population} genomes, "
          f"{args.criterion.upper()} fitness")
    print(f"  memoryless floor: {floor:.6g}"
          + (f"  (closed form {closed_form:.6g}, stateless search {baseline:.6g})"
             if baseline is not None else "  (closed form)"))
    print(f"  interface {grammar.inputs} in / {grammar.outputs} out; "
          f"{len(kinds)} node kinds; dictionary v{DICTIONARY_VERSION}")
    print(f"  {args.variation} variation, {args.codebook} codebook "
          f"({codebook.density:.1%} of {codebook.low}..{codebook.high} are genes)")
    print(f"  crossover {args.crossover_rate}, elite {args.elite}, "
          f"tournament {args.tournament}, max {args.max_genes} genes, "
          f"node penalty {args.node_penalty}"
          + (", neutral drift" if args.neutral_drift else ""))

    executor = (
        ProcessPoolExecutor(args.workers, initializer=_initialize_worker, initargs=(evaluator,))
        if args.workers > 1
        else None
    )
    results = []
    try:
        for seed in range(args.seed, args.seed + args.runs):
            results.append((seed, *_run(args, seed, grammar, codebook, evaluator, weights, config, floor, executor)))
    finally:
        if executor is not None:
            executor.shutdown()

    best_seed, best, fitness, history = min(results, key=lambda item: item[2].raw(item[1]))
    if len(results) > 1:
        _seed_table(results, floor, best_seed)
    graph = fitness.decode(best)
    score = fitness.raw(best)
    genes = codebook.translate(best)
    print(f"\n  best genome (seed {best_seed}, {len(best)} genes, "
          f"{sum(1 for gene in genes if gene)} known to the {args.codebook} codebook):")
    print(f"    {list(best)}")
    print("    " + describe([gene for gene in genes if gene]).replace("\n", "\n    "))
    print("\n  decodes to:")
    print(format_equations(graph, indent="    "))
    print(f"\n  score {score:.6g} = {score / floor:.4f} x floor, "
          f"{model_size(graph)} nodes "
          f"({sum(1 for n in graph.nodes.values() if n.kind in STATEFUL_KINDS)} stateful): "
          + ("BELOW the memoryless floor" if score < floor else "not below the floor"))

    if args.save:
        payload = {
            "dictionary_version": DICTIONARY_VERSION,
            "benchmark": args.benchmark,
            "grammar": {"inputs": grammar.inputs, "outputs": grammar.outputs, "kinds": list(kinds)},
            "genome": list(best),
            "codebook": args.codebook,
            "labels": [label(gene) if gene else "intron" for gene in genes],
            "score": score,
            "floor": floor,
            "seed": best_seed,
            "graph": graph_to_dict(graph),
        }
        Path(args.save).write_text(json.dumps(payload, indent=2))
        print(f"  genome written to {args.save}")
    if args.figure:
        _render(args.figure, graph, dataset, history, floor, score, benchmark.title)
    return 0


def _run(args, seed, grammar, codebook, evaluator, weights, config, floor, executor):
    rng = Random(seed)
    # Both variations start from the same structured random genomes, written in
    # the codebook's numbers, so a comparison measures variation alone.
    population = [
        codebook.encode(random_genome(rng, grammar, tuple(args.initial_nodes)))
        for _ in range(args.population)
    ]
    fitness = GenomeFitness(
        evaluator, grammar, args.node_penalty,
        map_function=(
            (lambda _, graphs: executor.map(_evaluate_in_worker, graphs, chunksize=4))
            if executor is not None
            else map
        ),
        codebook=codebook,
    )

    if args.variation == "numeric":
        def cross(first, second, rng):
            return one_point(first, second, rng)

        def vary(genome, rng):
            return numeric_mutate(genome, rng, codebook)
    else:
        def cross(first, second, rng):
            return splice(first, second, rng)

        def vary(genome, rng):
            return mutate(genome, rng, grammar, weights)

    def breed(first, second, rng):
        child = cross(first, second, rng) if second is not None else first
        # At least one mutation, then a geometric number more.
        child = vary(child, rng)
        while rng.random() < args.extra_mutation:
            child = vary(child, rng)
        return child

    started = perf_counter()

    def report(generation, genomes, scores):
        if generation % args.report_every and generation != args.generations:
            return
        index = min(range(len(genomes)), key=lambda i: (scores[i], len(genomes[i])))
        raw = fitness.raw(genomes[index])
        size = model_size(fitness.decode(genomes[index]))
        mean_length = sum(len(g) for g in genomes) / len(genomes)
        print(f"  seed {seed}  gen {generation:>5}  best {raw:>11.6g}  "
              f"{raw / floor:>7.4f} x floor  {size:>3} nodes  "
              f"{len(genomes[index]):>3} genes (mean {mean_length:5.1f})  "
              f"{fitness.simulations:>7} sims  {perf_counter() - started:7.1f}s")

    def stop(history):
        window = args.stall_patience
        return bool(window) and len(history) > window and history[-1] >= history[-1 - window]

    engine = GeneticAlgorithm(breed, fitness, config, seed)
    result = engine.run(population, args.generations, report, stop if args.stall_patience else None)
    best = min(result.population, key=lambda genome: (fitness.raw(genome), len(genome)))
    return best, fitness, result.history


def _seed_table(results, floor, best_seed):
    print(f"\n  across {len(results)} seeds:")
    for seed, best, fitness, _ in results:
        score = fitness.raw(best)
        print(f"    seed {seed:>4}  {score:>12.6g}  {score / floor:>8.4f} x floor"
              + ("   <- best" if seed == best_seed else ""))
    below = sum(1 for _, best, fitness, _ in results if fitness.raw(best) < floor)
    print(f"    {below}/{len(results)} runs below the floor")


def _render(path, graph, dataset, history, floor, score, title) -> None:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    session = SignalSimulator().start(graph)
    modelled = [session.step(row) for row in dataset.inputs]
    channels = len(dataset.outputs[0])
    figure = plt.figure(figsize=(16, 3.0 * max(channels + 1, 3)))
    grid = figure.add_gridspec(channels + 1, 2, width_ratios=(1.1, 1.0))
    axes = figure.add_subplot(grid[:, 0])
    draw_graph(axes, graph)
    axes.set_title(f"{model_size(graph)} nodes (decoded genome)", fontsize=10)
    axes.axis("off")
    steps = range(len(dataset.inputs))
    for channel in range(channels):
        panel = figure.add_subplot(grid[channel, 1])
        measured = [row[channel] for row in dataset.outputs]
        predicted = [row[channel] for row in modelled]
        panel.plot(steps, measured, linewidth=1.4, label="plant")
        panel.plot(steps, predicted, linewidth=1.1, linestyle="--", label="model")
        panel.set_ylabel(f"y{channel}", fontsize=9)
        panel.legend(fontsize=7, loc="upper right")
        if channel == 0:
            panel.set_title(title, fontsize=10)
    progress = figure.add_subplot(grid[channels, 1])
    progress.plot(range(len(history)), history, linewidth=1.2)
    progress.axhline(floor, linestyle=":", color="crimson")
    progress.set_yscale("log")
    progress.set_xlabel("generation", fontsize=9)
    progress.set_title(f"final {score:.6g} against floor {floor:.6g} (dotted)", fontsize=9)
    figure.tight_layout()
    figure.savefig(path, dpi=120)
    plt.close(figure)
    print(f"  figure written to {path}")


def _controls(args, channels: int):
    if channels == 1:
        return training_controls(args.steps, Random(args.data_seed), args.amplitude)
    return channel_training_controls(
        args.steps, channels, Random(args.data_seed), args.amplitude, args.hold
    )


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--benchmark", choices=sorted(BENCHMARKS), default="linear")
    parser.add_argument("--generations", type=int, default=300)
    parser.add_argument("--population", type=int, default=60)
    parser.add_argument("--elite", type=int, default=2)
    parser.add_argument("--tournament", type=int, default=3)
    parser.add_argument("--crossover-rate", type=float, default=0.5)
    parser.add_argument("--extra-mutation", type=float, default=0.3,
                        help="probability of each further mutation after the first")
    parser.add_argument("--max-genes", type=int, default=200)
    parser.add_argument("--initial-nodes", type=int, nargs=2, default=(3, 10))
    parser.add_argument("--node-penalty", type=float, default=0.0,
                        help="relative score cost per expressed node")
    parser.add_argument("--variation", choices=("context", "numeric"), default="context",
                        help="context: genes chosen from what the position allows; "
                             "numeric: integers edited blind (+-1, redraw, insert, delete)")
    parser.add_argument("--codebook", choices=sorted(CODEBOOKS), default="ordered",
                        help="which integer stands for which gene")
    parser.add_argument("--neutral-drift", action="store_true",
                        help="leave score ties to chance instead of preferring the shorter genome")
    parser.add_argument("--no-insert-record", action="store_true",
                        help="gene-level mutations only, no composite record insertion")
    parser.add_argument("--kinds", nargs="+", default=None,
                        help="operator groups or names to allow (default: all)")
    parser.add_argument("--stall-patience", type=int, default=0,
                        help="stop after this many generations without improvement")
    parser.add_argument("--stateless-baseline", action="store_true",
                        help="also search a memoryless baseline (automatic where the closed form is not strict)")
    parser.add_argument("--steps", type=int, default=150)
    parser.add_argument("--amplitude", type=float, default=1.0)
    parser.add_argument("--hold", type=int, default=10)
    parser.add_argument("--criterion", default="ise")
    parser.add_argument("--time-step", type=float, default=None)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--runs", type=int, default=1, help="independent seeds from --seed")
    parser.add_argument("--data-seed", type=int, default=7)
    parser.add_argument("--workers", type=int, default=1)
    parser.add_argument("--report-every", type=int, default=25)
    parser.add_argument("--save", default=None, help="write the best genome as JSON")
    parser.add_argument("--figure", default=None, help="write a PNG of the best model")
    return parser


if __name__ == "__main__":
    raise SystemExit(main())
