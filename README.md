# AMEBA

**Architecture Mapping Evolutionary Based Algorithm** (AMEBA) evolves graph
architectures independently of the systems used to evaluate them.

The new Python implementation is split into three layers:

- `ameba_graph`: domain-neutral graph models and evolutionary operations.
- `ameba_signal`: a mathematical-signal simulator for evaluating graphs.
- `ameba`: integration, configuration, and command-line workflows.

The previous Java implementations are preserved under [`legacy/`](legacy/README.md).

## Development

AMEBA requires Python 3.11 or newer. Third-party dependencies are fine — add
them to `dependencies` in `pyproject.toml` as the project needs them. The only
standing preference is that `ameba_graph` and `ameba_signal` keep to the
standard library where it costs nothing, so the evolutionary and simulation
layers stay easy to embed; the application layer has no such restriction.

`matplotlib` is currently the one runtime dependency, used by the reporting
script.

```shell
python -m pip install -e .
python -m unittest discover -s tests -v
python -m ameba
```

The second command runs the test suite. The third runs a small signal-graph
demonstration through the same evaluator boundary that evolution uses.

See [docs/architecture.md](docs/architecture.md) for the dependency rules that
keep graph evolution independent from signal simulation.

## CLI workflows

```shell
ameba evaluate examples/linear.graph.json examples/linear.dataset.json
ameba evolve examples/linear.graph.json examples/linear.dataset.json \
  --config configs/default.toml --checkpoint checkpoint.json --best-graph best.graph.json
ameba resume checkpoint.json examples/linear.dataset.json --generations 100
ameba inspect checkpoint.json
ameba visualize best.graph.json --output graph.html
```

Graphs, datasets, and checkpoints use explicit schema versions. Checkpoints also
store the random-generator state, so resuming produces the same sequence as an
uninterrupted run with the same configuration. Optional topology-age protection
also stores lineage ages in the checkpoint.

The default evolution portfolio includes bounded parameter and weight mutation,
edge insertion/removal/rewiring, node splitting/replacement/removal/bypass,
two-parent aligned-attribute crossover, and structural crossover. Every operator
works on a copy, respects structure/endpoint/attribute locks, and returns only a
graph accepted by the active domain policy.

Population simulation can use multiple processes while preserving score order
and deterministic random behavior. Set `simulation_workers` in the TOML config
for `ameba evolve`/`resume`, or pass `--simulation-workers` to the benchmark
report. Worker processes require a normal script/module entry point; on Windows,
do not launch multiprocessing experiments from code piped through standard input.

Evolution can also split the total population into independent islands with
periodic ring exchange. Crossover exchange keeps both parents in their islands
and inserts only a destination-valid hybrid child; direct migration remains
available for experiments. For example:

```toml
[evolution]
population_size = 24
island_count = 4
migration_interval = 10
migration_size = 1
island_exchange = "crossover"
```

Each island gets six candidates in this example. Checkpoints retain island
boundaries and the migration phase, so resumed runs remain deterministic.

Signal islands may also use different node-generation profiles:

```toml
[[islands]]
name = "simple/static"
evolvable_kinds = ["add", "negate", "constant"]

[[islands]]
name = "delay-heavy"
evolvable_kinds = ["delay", "add", "multiply", "constant"]

[[islands]]
name = "nonlinear"
evolvable_kinds = ["sin", "tanh", "square", "sqrt", "abs", "exp", "log"]

[[islands]]
name = "unrestricted"
evolvable_kinds = [
  "add", "multiply", "negate", "reciprocal", "sin", "tanh", "square",
  "sqrt", "abs", "exp", "log", "delay", "integral", "derivative",
  "filter_lp", "filter_hp", "constant",
]
```

The number of `[[islands]]` tables must equal `island_count`. Profiles govern
which nodes reproduction creates locally; migration is what lets discoveries
cross between profiles.

See [docs/java-reproduction-audit.md](docs/java-reproduction-audit.md) for the
legacy Java reliability review and the explicit migration status of each idea.

## Benchmarks

`ameba.benchmarks` provides single-input/single-output identification tasks. The
model sees only `u(k)` and must carry its own state through delay nodes, so it
has to represent the plant's dynamics rather than fit a static map. A model is
credited only when it scores below the *memoryless floor*, the best result
attainable by `y(k+1) = c·u(k)`, which is unreachable without state.

Two rungs are provided: a third-order linear plant, and the Narendra–Parthasarathy
nonlinear plant. Evolution currently solves the linear rung — converging on
three cascaded low-pass sections rather than the reference delay chain — and
stalls exactly at the memoryless floor on the nonlinear one.

See [docs/benchmarks.md](docs/benchmarks.md) for the plants, the pass criterion,
and the standing results.

To run one and see what it found:

```shell
python scripts/benchmark_report.py
python scripts/benchmark_report.py --benchmark narendra --generations 300
python scripts/benchmark_report.py --graph reference --no-show
```

The benchmark runner also supports specialized islands directly. `--population`
is the total population across all islands; `--restarts` remains a separate set
of independent complete runs:

```shell
python scripts/benchmark_report.py --benchmark narendra --islands 4 \
  --migration-interval 10 --migration-size 1 --population 16 \
  --simulation-workers 10 --generations 1000 --no-show
```

Multi-island benchmark runs use diverse deterministic inputs by default: the
requested primary input plus distinct random, sine, and step excitations. Each
island score is normalized by that signal's memoryless floor, and the final
winner is rescored across every signal. Pass `--island-signals shared` to use
one input trajectory on every island.

An optional complexity cycle lets structures expand under a low node penalty,
then compresses them under a higher penalty. Raw simulation fitness is retained,
so changing pressure does not rerun simulations and remains checkpoint-safe:

```shell
python scripts/benchmark_report.py --benchmark narendra --islands 4 \
  --oscillating-penalty --expansion-generations 25 \
  --compression-generations 25 --expansion-node-penalty 0 \
  --node-penalty 0.002
```

The script prints the scores, the evolved graph, and the model's difference
equations recovered from that graph, then opens a figure with the signals, the
graph, and the equations. `ameba.equations.format_equations(graph)` does the
equation recovery on its own for any signal graph.
