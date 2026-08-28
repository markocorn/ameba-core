"""AMEBA command-line workflows."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Sequence

from ameba_graph import Edge, EvolutionEngine, Graph, Node
from ameba_graph.checkpoint import checkpoint_dumps, checkpoint_loads
from ameba_graph.crossover import (
    AlignedAttributeCrossover,
    CrossoverPortfolio,
    InducedSubgraphInsertionCrossover,
    TypedSubgraphReplacementCrossover,
    UniformGraphCrossover,
)
from ameba_graph.mutation import (
    AddEdge,
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
from ameba_graph.serialization import graph_dumps, graph_loads
from ameba_signal import Dataset, SignalEvaluator, SignalGraphPolicy, SignalSimulator
from ameba_signal.serialization import dataset_loads

from .config import AppConfig, load_config
from .visualization import write_graph_html


def example_graph() -> Graph:
    """Return a graph that calculates y = 2*x + 1."""
    return Graph(
        nodes=[
            Node("x", "input", {"index": 0}),
            Node("two", "constant", {"value": 2.0}),
            Node("product", "multiply"),
            Node("one", "constant", {"value": 1.0}),
            Node("sum", "add"),
            Node("y", "output", {"index": 0}),
        ],
        edges=[
            Edge("e1", "x", "product"),
            Edge("e2", "two", "product"),
            Edge("e3", "product", "sum"),
            Edge("e4", "one", "sum"),
            Edge("e5", "sum", "y"),
        ],
    )


def main(argv: Sequence[str] | None = None) -> int:
    parser = _parser()
    args = parser.parse_args(argv)
    command = args.command or "demo"
    try:
        if command == "demo":
            _demo()
        elif command == "evaluate":
            _evaluate(args)
        elif command == "evolve":
            _evolve(args)
        elif command == "resume":
            _resume(args)
        elif command == "inspect":
            _inspect(args)
        elif command == "visualize":
            _visualize(args)
        else:
            parser.error(f"Unknown command: {command}")
    except (OSError, ValueError) as exc:
        parser.error(str(exc))
    return 0


def _demo() -> None:
    graph = example_graph()
    result = SignalSimulator().run(graph, [3.0])
    dataset = Dataset(inputs=((0.0,), (1.0,), (2.0,)), outputs=((1.0,), (3.0,), (5.0,)))
    score = SignalEvaluator(dataset).evaluate(graph)
    print(f"AMEBA signal demo: input=3.0 output={result[0]} fitness={score}")


def _evaluate(args: argparse.Namespace) -> None:
    graph = _read_graph(args.graph)
    dataset = _read_dataset(args.dataset)
    SignalGraphPolicy().validate(graph)
    score = SignalEvaluator(dataset).evaluate(graph)
    print(json.dumps({"score": score}, allow_nan=True, sort_keys=True))


def _evolve(args: argparse.Namespace) -> None:
    config = load_config(args.config)
    graph = _read_graph(args.graph)
    dataset = _read_dataset(args.dataset)
    with _engine(config, dataset) as engine:
        result = engine.run(
            [graph.copy() for _ in range(config.evolution.population_size)],
            config.generations,
        )
        _write_checkpoint(args.checkpoint, checkpoint_dumps(engine.checkpoint(result)))
    if args.best_graph:
        Path(args.best_graph).write_text(graph_dumps(result.best.graph), encoding="utf-8")
    print(json.dumps({"generation": result.generations, "best_score": result.best.score}))


def _resume(args: argparse.Namespace) -> None:
    config = load_config(args.config)
    dataset = _read_dataset(args.dataset)
    checkpoint = checkpoint_loads(Path(args.checkpoint).read_text(encoding="utf-8"))
    with _engine(config, dataset) as engine:
        generations = config.generations if args.generations is None else args.generations
        result = engine.resume(checkpoint, generations)
        output = args.output or args.checkpoint
        _write_checkpoint(output, checkpoint_dumps(engine.checkpoint(result)))
    if args.best_graph:
        Path(args.best_graph).write_text(graph_dumps(result.best.graph), encoding="utf-8")
    print(json.dumps({"generation": result.generations, "best_score": result.best.score}))


def _inspect(args: argparse.Namespace) -> None:
    checkpoint = checkpoint_loads(Path(args.checkpoint).read_text(encoding="utf-8"))
    print(
        json.dumps(
            {
                "generation": checkpoint.generation,
                "population_size": len(checkpoint.population),
                "island_count": len(checkpoint.island_sizes) or 1,
                "best_score": checkpoint.best.score,
                "best_nodes": len(checkpoint.best.graph.nodes),
                "best_edges": len(checkpoint.best.graph.edges),
            },
            sort_keys=True,
        )
    )


def _visualize(args: argparse.Namespace) -> None:
    graph = _read_graph(args.graph)
    write_graph_html(graph, args.output, args.title)
    print(json.dumps({"output": str(Path(args.output))}))


def _engine(config: AppConfig, dataset: Dataset) -> EvolutionEngine:
    policy = SignalGraphPolicy(
        config.signal.evolvable_kinds,
        (config.signal.weight_min, config.signal.weight_max),
    )
    island_policies = tuple(
        SignalGraphPolicy(
            island.evolvable_kinds,
            (config.signal.weight_min, config.signal.weight_max),
        )
        for island in config.islands
    ) or None
    return EvolutionEngine(
        evaluator=SignalEvaluator(dataset),
        policy=policy,
        mutations=[
            MutationPortfolio(
                [
                    MutateNodeAttributes(),
                    MutateEdgeAttributes(),
                    SplitEdge(),
                    AddEdge(),
                    RemoveEdge(),
                    MoveEdgeSource(),
                    MoveEdgeTarget(),
                    ReplaceNode(),
                    RemoveNodeBypass(),
                    RemoveNode(),
                ]
            )
        ],
        crossover=CrossoverPortfolio(
            [
                AlignedAttributeCrossover(),
                InducedSubgraphInsertionCrossover(),
                TypedSubgraphReplacementCrossover(),
                UniformGraphCrossover(),
            ]
        ),
        config=config.evolution,
        seed=config.seed,
        simulation_workers=config.simulation_workers,
        island_policies=island_policies,
    )


def _read_graph(path: str) -> Graph:
    return graph_loads(Path(path).read_text(encoding="utf-8"))


def _read_dataset(path: str) -> Dataset:
    return dataset_loads(Path(path).read_text(encoding="utf-8"))


def _write_checkpoint(path: str, data: str) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(data, encoding="utf-8")


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="ameba",
        description="Architecture Mapping Evolutionary Based Algorithm",
    )
    commands = parser.add_subparsers(dest="command")
    commands.add_parser("demo", help="run the built-in signal graph demonstration")

    evaluate = commands.add_parser("evaluate", help="evaluate one graph against a dataset")
    evaluate.add_argument("graph")
    evaluate.add_argument("dataset")

    evolve = commands.add_parser("evolve", help="start evolution from a seed graph")
    evolve.add_argument("graph")
    evolve.add_argument("dataset")
    evolve.add_argument("--config", default="configs/default.toml")
    evolve.add_argument("--checkpoint", default="checkpoint.json")
    evolve.add_argument("--best-graph")

    resume = commands.add_parser("resume", help="resume a deterministic checkpoint")
    resume.add_argument("checkpoint")
    resume.add_argument("dataset")
    resume.add_argument("--config", default="configs/default.toml")
    resume.add_argument("--generations", type=int)
    resume.add_argument("--output")
    resume.add_argument("--best-graph")

    inspect = commands.add_parser("inspect", help="summarize an evolution checkpoint")
    inspect.add_argument("checkpoint")

    visualize = commands.add_parser("visualize", help="write a standalone graph visualization")
    visualize.add_argument("graph")
    visualize.add_argument("--output", default="graph.html")
    visualize.add_argument("--title", default="AMEBA graph")
    return parser


if __name__ == "__main__":
    raise SystemExit(main())
