"""Record how each genome encoding converges, generation by generation.

The question is not which encoding finds the best model but how each one
*moves*: how fast the best and median fall, how quickly the population loses
its variety, how genomes grow, and what a single mutation typically does to
its parent -- improve it, leave it unchanged, or break it. Every method runs
the same GA, the same budget, and the same data, so only the encoding and its
variation differ.

    A-base        fixed-role records, +-1 inside each locus's own small alphabet
    A-runs        fixed-role records, every locus 0..998, symbols in synonym runs
    A-shuffle     fixed-role records, every locus 0..998, synonyms shuffled
    A-mixed       kind and source shuffled, parameters in runs, weights fine
    B-redundant   free sequence, every role an equal block of synonym runs
    C-two-level   free sequence, role chosen first, then a gene within it
    ref-dense     free sequence, blind +-1 on gap-free numbers (reference)
    ref-context   free sequence, context-aware gene choice (reference)

    python scripts/encoding_experiment.py --method A-fixed --out a.json
"""

from __future__ import annotations

import argparse
import json
import statistics
import sys
from math import isfinite
from pathlib import Path
from random import Random
from time import perf_counter

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from ameba.benchmarks import (  # noqa: E402
    BENCHMARKS,
    channel_training_controls,
    training_controls,
    trajectory_dataset,
    trajectory_floor,
)
from ameba_genome import (  # noqa: E402
    GAConfig,
    GeneticAlgorithm,
    GenomeFitness,
    Grammar,
    dense,
    model_size,
    mutate,
    numeric_mutate,
    one_point,
    phenotype_key,
    random_genome,
    splice,
)
from ameba_genome.codebook import gene_groups, redundant  # noqa: E402
from ameba_genome.fixed import FixedConfig, FixedEncoding  # noqa: E402
from ameba_genome.codon import CodonConfig, CodonEncoding  # noqa: E402
from ameba_genome.tags import TagEncoding  # noqa: E402
from ameba_genome.statespace import WIDTH, StateSpaceConfig, StateSpaceEncoding  # noqa: E402
from ameba_genome.ga import (  # noqa: E402
    CommaSelection,
    Crowding,
    Generational,
    Islands,
    LinearRanking,
    PlusSelection,
    Restart,
    Roulette,
    SteadyState,
    Tournament,
    Truncation,
)
from ameba_genome.numeric import TwoLevelMutation  # noqa: E402
from ameba.benchmarks.filterbank import bank_from_spec  # noqa: E402
from ameba.benchmarks.splits import suite  # noqa: E402
from ameba_signal import Dataset  # noqa: E402
from ameba.equations import format_equations  # noqa: E402
from ameba_genome.dictionary import label as gene_label  # noqa: E402
from ameba_graph.serialization import graph_to_dict  # noqa: E402
from ameba_signal import SignalEvaluator, SignalSimulator  # noqa: E402
from ameba_signal.operators import operators_in  # noqa: E402
from ameba_signal.stateful import STATEFUL_KINDS  # noqa: E402

METHODS = ("A-base", "A-runs", "A-shuffle", "A-mixed", "A-custom", "E-statespace", "E-codon", "E-tag", "B-redundant", "C-two-level",
           "ref-dense", "ref-context")


def _numbers(spec: str) -> list[str]:
    return spec.split(":")[1:]


def parse_selection(spec: str):
    """tournament:3 | truncation:0.2 | ranking:1.7 | roulette"""
    name, args = spec.split(":")[0], _numbers(spec)
    if name == "tournament":
        return Tournament(int(args[0]) if args else 3)
    if name == "truncation":
        return Truncation(float(args[0]) if args else 0.2)
    if name == "ranking":
        return LinearRanking(float(args[0]) if args else 1.7)
    if name == "roulette":
        return Roulette()
    raise SystemExit(f"unknown selection {spec!r}")


def parse_scheme(spec: str):
    """generational:2 | steady:1 | plus | comma:2:1 | crowding"""
    name, args = spec.split(":")[0], _numbers(spec)
    if name == "generational":
        return Generational(int(args[0]) if args else 2)
    if name == "steady":
        return SteadyState(int(args[0]) if args else 1)
    if name == "plus":
        return PlusSelection()
    if name == "comma":
        return CommaSelection(int(args[0]) if args else 2, int(args[1]) if len(args) > 1 else 0)
    if name == "crowding":
        return Crowding()
    raise SystemExit(f"unknown scheme {spec!r}")


CROSSOVERS = ("one_point", "two_point", "uniform_record", "uniform_locus", "transfer", "organelle", "none")


def build(method: str, grammar: Grammar, crossover: str = "one_point", encoding: dict | None = None,
          primary_inputs: int | None = None):
    """(translator, initial, mutate, crossover) for one method.

    ``crossover`` selects among the fixed encoding's operators; the free
    encodings each have one and ignore it.
    """
    if method == "E-tag":
        # A StateSpaceConfig, with the number of tags under "tags".
        settings = dict(encoding or {})
        tags = settings.pop("tags", 16)
        tagged = TagEncoding(grammar, StateSpaceConfig.from_dict(settings), tags, primary_inputs)
        return tagged, tagged.random_genome, tagged.mutate, tagged.crossover
    if method == "E-codon":
        # A StateSpaceConfig, with the codon settings under "codon".
        settings = dict(encoding or {})
        codon_settings = CodonConfig.from_dict(settings.pop("codon", {}))
        codon = CodonEncoding(grammar, StateSpaceConfig.from_dict(settings), codon_settings, primary_inputs)
        return codon, codon.random_genome, codon.mutate, codon.crossover
    if method == "E-statespace":
        statespace = StateSpaceEncoding(
            grammar, config=StateSpaceConfig.from_dict(encoding) if encoding else None,
            primary_inputs=primary_inputs)
        operator = statespace.organelle if crossover == "organelle" else statespace.crossover
        return statespace, statespace.random_genome, statespace.mutate, operator
    if method == "A-custom":
        fixed = FixedEncoding(grammar, config=FixedConfig.from_dict(encoding or {}))
        name = "crossover" if crossover in ("one_point", "none") else crossover
        return fixed, fixed.random_genome, fixed.mutate, getattr(fixed, name)
    if method.startswith("A-"):
        encoding = FixedEncoding(grammar, method[2:])
        name = "crossover" if crossover in ("one_point", "none") else crossover
        return encoding, encoding.random_genome, encoding.mutate, getattr(encoding, name)
    if method == "C-two-level":
        two_level = TwoLevelMutation(gene_groups())
        return (None, lambda rng: random_genome(rng, grammar, (3, 10)), two_level.mutate, one_point)
    if method == "ref-context":
        return (None, lambda rng: random_genome(rng, grammar, (3, 10)),
                lambda genome, rng: mutate(genome, rng, grammar), splice)
    book = redundant() if method == "B-redundant" else dense()
    synonyms: dict[int, list[int]] = {}
    for number, gene in book.to_gene.items():
        synonyms.setdefault(gene, []).append(number)

    def initial(rng):
        # Any synonym, not always the first of its run.
        return tuple(rng.choice(synonyms[gene]) for gene in random_genome(rng, grammar, (3, 10)))

    return book, initial, lambda genome, rng: numeric_mutate(genome, rng, book), one_point


def run(method: str, benchmark_name: str, seed: int, generations: int, population: int,
        extra_mutation: float, crossover_rate: float, crossover: str = "one_point",
        max_records: int | None = None, ga_parts: dict | None = None,
        steps: int = 150, hold: int = 10, progress: int = 0, encoding: dict | None = None,
        kinds: tuple[str, ...] | None = None, data: str = "single", node_penalty: float = 0.0,
        shortlist: int = 30, filterbank: str | None = None, init_plant_inputs: bool = False) -> dict:
    benchmark = BENCHMARKS[benchmark_name]
    # The multi-channel rungs are slow plants, so their input is held for
    # ``hold`` samples; white noise would be filtered out almost entirely.
    controls = (
        training_controls(steps, Random(7))
        if benchmark.channels == 1
        else channel_training_controls(steps, benchmark.channels, Random(7), 1.0, hold)
    )
    trajectory = benchmark.build(controls)
    dataset = trajectory_dataset(trajectory)
    floor = trajectory_floor(trajectory)
    # A filter bank is part of the model: the plant and its floor see u alone,
    # the model sees u and every filter output.
    bank = bank_from_spec(filterbank)
    plant_inputs = len(dataset.inputs[0])
    if bank is not None:
        dataset = Dataset(bank.augment(dataset.inputs), dataset.outputs)
    grammar = Grammar(len(dataset.inputs[0]), len(dataset.outputs[0]), *([kinds] if kinds else []))
    # Fresh state-space genomes read every input alike unless asked to read
    # only the plant's own (inputs-last layout only).
    translator, initial, vary, cross = build(method, grammar, crossover, encoding,
                                             plant_inputs if init_plant_inputs else None)
    # "single": the one trajectory every earlier run used. "diverse": the
    # benchmark's training suite -- noise, steps and a sinusoid, each from
    # rest -- scored as the mean ratio to their floors, so the floor is one.
    family = data if data in ("spectral", "octaves") else "diverse"
    training = (suite(benchmark, "train", hold, family=family, bank=bank) if data != "single"
                else SignalEvaluator(dataset))
    if data != "single":
        floor = 1.0
    fitness = GenomeFitness(training, grammar, codebook=translator, node_penalty=node_penalty)
    validation = suite(benchmark, "validation", hold, family=family, bank=bank)
    test = suite(benchmark, "test", hold, family=family, bank=bank)
    if crossover == "none":
        crossover_rate = 0.0
    max_genes = 200
    if method in ("E-codon", "E-tag"):
        # A start codon or a tag makes a record longer than the state-space
        # encoding's six numbers; the cap grows with it, so every encoding can
        # hold as many records.
        width = translator.width + (1 if method == "E-codon" else 0)
        max_genes = 200 * width // WIDTH
    if max_records is not None:
        if not method.startswith("A-"):
            raise SystemExit("--max-records applies to the fixed encodings only")
        max_genes = translator.tail() + 6 * max_records

    # (first parent, genome after crossover or None, child) per child, in order.
    pending: list = []
    capped = [0]

    def breed(first, second, rng):
        crossed = cross(first, second, rng) if second is not None else None
        child = vary(crossed if crossed is not None else first, rng)
        while rng.random() < extra_mutation:
            child = vary(child, rng)
        if len(child) > max_genes:
            # Handled here rather than by the GA, so every child is counted once.
            capped[0] += 1
            crossed = None
            child = vary(first, rng)
            if len(child) > max_genes:
                child = first
        pending.append((first, crossed))
        return child

    def classify(before, after, score_after):
        if not isfinite(score_after):
            return "invalid"
        if phenotype_key(fitness.decode(after)) == phenotype_key(fitness.decode(before)):
            return "same"
        score_before = fitness.raw(before)
        if score_after < score_before:
            return "better"
        return "equal" if score_after == score_before else "worse"

    def empty():
        return {"better": 0, "same": 0, "equal": 0, "worse": 0, "invalid": 0}

    outcomes: list[dict] = []
    crossover_outcomes: list[dict] = []
    mutation_outcomes: list[dict] = []
    cap_hits: list[float] = []

    def score_many(genomes):
        scores = fitness(genomes)
        if pending and len(pending) == len(genomes):
            overall, crossing, mutating = empty(), empty(), empty()
            for (parent, crossed), child, score in zip(pending, genomes, scores):
                overall[classify(parent, child, score)] += 1
                base = parent
                if crossed is not None:
                    crossing[classify(parent, crossed, fitness.raw(crossed))] += 1
                    base = crossed
                mutating[classify(base, child, score)] += 1
            outcomes.append(overall)
            crossover_outcomes.append(crossing)
            mutation_outcomes.append(mutating)
            cap_hits.append(capped[0] / len(genomes))
        pending.clear()
        capped[0] = 0
        return scores

    record = {"best": [], "median": [], "unique": [], "length": [], "nodes": []}

    def report(generation, genomes, scores):
        scores = fitness.raw_many(genomes)  # unshaped, whatever the penalty
        finite = [score for score in scores if isfinite(score)]
        graphs = [fitness.decode(genome) for genome in genomes]
        record["best"].append(min(finite) / floor if finite else None)
        record["median"].append(statistics.median(finite) / floor if finite else None)
        record["unique"].append(len({phenotype_key(graph) for graph in graphs}))
        record["length"].append(sum(len(genome) for genome in genomes) / len(genomes))
        record["nodes"].append(sum(model_size(graph) for graph in graphs) / len(graphs))
        if progress and (generation % progress == 0 or generation == generations):
            # One parseable line per checkpoint; run_experiments.py reads the
            # last one to show how far each running job has got.
            best, median = (float("inf") if v is None else v for v in (record["best"][-1], record["median"][-1]))
            print(f"progress gen {generation}/{generations} best {best:.4f} "
                  f"median {median:.4f} unique {record['unique'][-1] / len(genomes):.2f} "
                  f"nodes {record['nodes'][-1]:.1f} sims {fitness.simulations} "
                  f"time {perf_counter() - started:.0f}", flush=True)

    config = GAConfig(population_size=population, crossover_rate=crossover_rate,
                      shorter_wins_ties=False, max_genes=max_genes)
    started = perf_counter()
    rng = Random(seed)
    population_genomes = [initial(rng) for _ in range(population)]
    parts = dict(ga_parts or {})
    if parts.pop("clearing", False):
        parts["clearing"] = lambda genome: phenotype_key(fitness.decode(genome))
    result = GeneticAlgorithm(breed, score_many, config, seed, initial=initial, **parts).run(
        population_genomes, generations, report)
    best = min(result.population, key=lambda genome: (fitness.raw(genome), len(genome)))
    # The final pick by validation: the best ``shortlist`` distinct models by
    # training error, judged on data the search never optimised.
    ranked, seen = [], set()
    for genome in sorted(result.population, key=lambda g: (fitness.raw(g), len(g))):
        key = phenotype_key(fitness.decode(genome))
        if key not in seen:
            seen.add(key)
            ranked.append(genome)
        if len(ranked) >= shortlist:
            break
    selected = min(ranked, key=lambda g: (validation.evaluate(fitness.decode(g)), fitness.raw(g)))
    best_model = describe_best(best, fitness, translator, dataset, floor)
    best_model["validation"] = validation.evaluate(fitness.decode(best))
    best_model["test"] = test.ratios(fitness.decode(best))
    selected_model = describe_best(selected, fitness, translator, dataset, floor)
    selected_model["validation"] = validation.evaluate(fitness.decode(selected))
    selected_model["test"] = test.ratios(fitness.decode(selected))
    return {
        "best_model": best_model,
        "selected_model": selected_model,
        "seed": seed,
        "seconds": perf_counter() - started,
        "simulations": fitness.simulations,
        "outcomes": outcomes,
        "crossover_outcomes": crossover_outcomes,
        "mutation_outcomes": mutation_outcomes,
        "cap_hits": cap_hits,
        "restarts": result.restarts,
        **record,
    }


def describe_best(genome, fitness, translator, dataset, floor) -> dict:
    """The final best model in every form worth reading later.

    The raw genome, its genes in the dictionary's words, the decoded graph and
    its equations, and the error per output channel: a summed score hides a
    model that tracks one output and ignores the other.
    """
    graph = fitness.decode(genome)
    if translator is not None and hasattr(translator, "build"):
        genes = ()  # built straight into a graph: no dictionary genes to list
    else:
        genes = translator.translate(genome) if translator is not None else genome
    channels = len(dataset.outputs[0])
    per_channel = [0.0] * channels
    try:
        session = SignalSimulator().start(graph)
        for inputs, expected in zip(dataset.inputs, dataset.outputs):
            for channel, (value, target) in enumerate(zip(session.step(inputs), expected)):
                per_channel[channel] += (value - target) ** 2
    except Exception:  # an invalid model has no per-channel error
        per_channel = [float("inf")] * channels
    kinds: dict[str, int] = {}
    for node in graph.nodes.values():
        kinds[node.kind] = kinds.get(node.kind, 0) + 1
    return {
        "score": fitness.raw(genome),
        "ratio": fitness.raw(genome) / floor,
        "channel_ise": per_channel,
        "genome": list(genome),
        "genes": [gene_label(gene) if gene else "intron" for gene in genes],
        "kinds": kinds,
        "stateful": sum(1 for node in graph.nodes.values() if node.kind in STATEFUL_KINDS),
        "equations": format_equations(graph),
        "graph": graph_to_dict(graph),
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--method", choices=METHODS, required=True)
    parser.add_argument("--benchmark", choices=sorted(BENCHMARKS), default="narendra")
    parser.add_argument("--generations", type=int, default=1500)
    parser.add_argument("--population", type=int, default=100)
    parser.add_argument("--seeds", type=int, default=4)
    parser.add_argument("--first-seed", type=int, default=1)
    parser.add_argument("--steps", type=int, default=150, help="simulated time steps")
    parser.add_argument("--kinds", default=None,
                        help="node kinds: operator groups or names, comma-separated (e.g. soft or soft,delay)")
    parser.add_argument("--data", choices=("single", "diverse", "spectral", "octaves"), default="single",
                        help="single: one noise trajectory (as before); diverse: noise, steps and a sinusoid; "
                             "spectral: noise and band-limited multisines, tested on six octave bands; "
                             "octaves: like spectral, but training covers all six octaves equally")
    parser.add_argument("--filterbank", default=None,
                        help="fixed filters in front of the model: octaves, laguerre or laguerre:POLE:COUNT")
    parser.add_argument("--init-plant-inputs", action="store_true",
                        help="E-statespace, inputs-last: fresh genomes read only the plant's inputs, not a bank's")
    parser.add_argument("--node-penalty", type=float, default=0.0,
                        help="relative score cost per node, shaping selection only")
    parser.add_argument("--progress", type=int, default=50,
                        help="print a progress line every N generations (0: off)")
    parser.add_argument("--hold", type=int, default=10, help="multi-channel rungs: samples each input is held")
    parser.add_argument("--extra-mutation", type=float, default=0.3)
    parser.add_argument("--crossover-rate", type=float, default=0.5)
    parser.add_argument("--crossover", choices=CROSSOVERS, default="one_point",
                        help="fixed encodings only: which crossover; none disables it")
    parser.add_argument("--max-records", type=int, default=None,
                        help="fixed encodings only: size cap in node records (default: 200 genes)")
    parser.add_argument("--label", default=None, help="name recorded in the output")
    parser.add_argument("--encoding", default=None,
                        help="A-custom: a FixedConfig as JSON; E-statespace: a StateSpaceConfig as JSON, e.g. '{\"layouts\": \"runs\", \"sources\": 3}'")
    parser.add_argument("--selection", default="tournament:3",
                        help="tournament:K | truncation:F | ranking:S | roulette")
    parser.add_argument("--scheme", default="generational:2",
                        help="generational:E | steady:B | plus | comma:R:E | crowding")
    parser.add_argument("--islands", default=None, help="COUNT:INTERVAL:MIGRANTS")
    parser.add_argument("--clearing", action="store_true", help="rank phenotype clones last")
    parser.add_argument("--immigrants", type=float, default=0.0, help="fraction replaced fresh per generation")
    parser.add_argument("--restart", default=None, help="PATIENCE:KEEP")
    parser.add_argument("--out", required=True)
    args = parser.parse_args(argv)

    runs = []
    for seed in range(args.first_seed, args.first_seed + args.seeds):
        parts = {
            "selection": parse_selection(args.selection),
            "scheme": parse_scheme(args.scheme),
            "clearing": args.clearing,
            "immigrants": args.immigrants,
        }
        if args.islands:
            count, interval, migrants = (int(x) for x in args.islands.split(":"))
            parts["islands"] = Islands(count, interval, migrants)
        if args.restart:
            patience, keep = (int(x) for x in args.restart.split(":"))
            parts["restart"] = Restart(patience, keep)
        result = run(args.method, args.benchmark, seed, args.generations, args.population,
                     args.extra_mutation, args.crossover_rate, args.crossover, args.max_records, parts,
                     args.steps, args.hold, args.progress,
                     json.loads(args.encoding) if args.encoding else None,
                     operators_in(*args.kinds.split(",")) if args.kinds else None,
                     args.data, args.node_penalty, filterbank=args.filterbank,
                     init_plant_inputs=args.init_plant_inputs)
        test_mean = sum(result["best_model"]["test"].values()) / len(result["best_model"]["test"])
        picked = sum(result["selected_model"]["test"].values()) / len(result["selected_model"]["test"])
        print(f"{args.method} {args.benchmark} seed {seed}: train {result['best'][-1]:.4f} x floor, "
              f"test {test_mean:.4f} (validation pick {picked:.4f}), "
              f"{result['simulations']} sims, {result['seconds']:.1f}s", flush=True)
        runs.append(result)
    Path(args.out).write_text(json.dumps({
        "method": args.label or args.method, "benchmark": args.benchmark,
        "steps": args.steps, "hold": args.hold,
        "crossover": args.crossover, "max_records": args.max_records,
        "encoding": json.loads(args.encoding) if args.encoding else None,
        "kinds": args.kinds, "data": args.data, "node_penalty": args.node_penalty,
        "filterbank": args.filterbank, "init_plant_inputs": args.init_plant_inputs,
        "selection": args.selection, "scheme": args.scheme, "islands": args.islands,
        "clearing": args.clearing, "immigrants": args.immigrants, "restart": args.restart,
        "generations": args.generations, "population": args.population, "runs": runs,
    }))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
