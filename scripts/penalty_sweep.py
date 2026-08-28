"""Sweep the parsimony weight and report what it costs and buys.

    python scripts/penalty_sweep.py
    python scripts/penalty_sweep.py --benchmark narendra --refine
    python scripts/penalty_sweep.py --penalties 0,1e-6,1e-3,1e-2 --seeds 8

Two things are measured against the same runs, because the parsimony weight
trades one for the other:

* **bloat** -- total nodes, and how many of them cannot reach the output at all;
* **reach** -- the unpenalised accuracy, and how often a run beats the
  memoryless floor, which is the only real evidence of dynamics.

A weight large enough to matter against genuine gradients turns out to be far
larger than bloat control needs, so the useful settings are very small.
"""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path
from random import Random
from statistics import median

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from ameba.benchmarks import (  # noqa: E402
    benchmark_engine,
    identification_dataset,
    linear,
    narendra,
    seed_graph,
    static_gain_floor,
    training_controls,
)
from ameba_graph import RefinementConfig, prune  # noqa: E402
from ameba_signal import SignalEvaluator  # noqa: E402

PLANTS = {
    "linear": (linear.linear_trajectory, "Third-order linear plant"),
    "narendra": (narendra.narendra_trajectory, "Narendra plant"),
}


def main(argv: list[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    weights = [float(item) for item in args.penalties.split(",") if item.strip()]
    names = list(PLANTS) if args.benchmark == "both" else [args.benchmark]

    refinement = (
        RefinementConfig(
            min_steps=args.refine_min,
            patience=args.refine_patience,
            max_steps=args.refine_max,
        )
        if args.refine
        else None
    )

    for name in names:
        _sweep(name, weights, args, refinement)
    return 0


def _sweep(name: str, weights: list[float], args, refinement) -> None:
    trajectory_of, title = PLANTS[name]
    trajectory = trajectory_of(
        training_controls(args.steps, Random(args.data_seed), args.amplitude)
    )
    dataset = identification_dataset(trajectory)
    accuracy = SignalEvaluator(dataset)
    floor = static_gain_floor(trajectory)
    seed = seed_graph()

    print(f"\n{title}  --  {args.steps} steps, {args.generations} generations, "
          f"{args.seeds} seeds, refinement {'on' if refinement else 'off'}")
    print(f"  memoryless floor {floor:.5f}")
    print("=" * 78)
    print(f"{'penalty':>10} {'median ISE':>12} {'best':>11} {'nodes':>7} "
          f"{'dead':>6} {'below floor':>12} {'time':>7}", flush=True)

    for weight in weights:
        scores: list[float] = []
        sizes: list[int] = []
        dead: list[int] = []
        started = time.time()
        # A refined row can take minutes, so report each run as it lands rather
        # than leaving the terminal silent until the whole row is done.
        print(f"{weight:>10g} ", end="", flush=True)
        for seed_index in range(args.seeds):
            engine = benchmark_engine(
                dataset,
                seed=seed_index,
                population_size=args.population,
                node_penalty=weight,
                refine=refinement,
            )
            best = engine.run(
                [seed.copy() for _ in range(args.population)], args.generations
            ).best.graph
            terminals = {
                node.id for node in best.nodes.values() if node.kind == "output"
            }
            scores.append(accuracy.evaluate(best))
            sizes.append(len(best.nodes))
            dead.append(len(best.nodes) - len(prune(best, terminals).nodes))
            print("." if scores[-1] >= floor else "+", end="", flush=True)

        beat = sum(1 for value in scores if value < floor)
        print(
            f"  {median(scores):>12.5f} {min(scores):>11.5f} "
            f"{median(sizes):>7.1f} {median(dead):>6.1f} {beat:>8}/{args.seeds}"
            f" {time.time() - started:>6.0f}s",
            flush=True,
        )


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--benchmark", choices=("linear", "narendra", "both"), default="both")
    parser.add_argument(
        "--penalties",
        default="0,1e-6,1e-3,5e-2",
        help="comma-separated parsimony weights to compare",
    )
    parser.add_argument("--seeds", type=int, default=6, help="runs per weight")
    parser.add_argument("--generations", type=int, default=150)
    parser.add_argument("--population", type=int, default=16)
    parser.add_argument("--steps", type=int, default=150)
    parser.add_argument("--amplitude", type=float, default=1.0)
    parser.add_argument("--data-seed", type=int, default=7)
    parser.add_argument(
        "--refine",
        action="store_true",
        help="tune parameters after each structural change",
    )
    parser.add_argument("--refine-min", type=int, default=20)
    parser.add_argument("--refine-patience", type=int, default=15)
    parser.add_argument("--refine-max", type=int, default=120)
    return parser


if __name__ == "__main__":
    raise SystemExit(main())
