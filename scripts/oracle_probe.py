"""Diagnostic only: give AMEBA the Narendra simulator's exact pre-output state.

No neural network or measured-output correction is used. The known simulator
runs from rest on u alone. At row k the extra inputs are y(k), y(k-1), y(k-2),
u(k-1); the target is y(k+1), which is NEVER supplied as an input. This is an
ideal-model state provider, not evidence of identification from u alone.

    python scripts/oracle_probe.py --check-only
    python scripts/oracle_probe.py --generations 1000 --population 300 --seeds 12

Runs sequentially, saving each completed arm/seed and resuming only if the
configuration AND Python source fingerprint match. Existing experiment scripts
are untouched. The GA retains maternal state inheritance in every arm; the
extra state channels are optional information, not replacement chromosomes.

Arms: raw u; u + four zeros; u + four band-pass outputs; u + exact states;
u + the same states delayed by --delay samples. All share the original u-only
static baseline. Beating it with supplied states is NOT proof of learned memory.
Comparing oracle with delayed oracle tests state freshness, not the phase
response of a band-pass filter. Null scores in JSON mean non-finite errors.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import statistics
import sys
from dataclasses import dataclass
from pathlib import Path
from random import Random
from time import perf_counter

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from ameba.benchmarks import BENCHMARKS, narendra
from ameba.benchmarks.filterbank import bank_from_spec
from ameba.benchmarks.splits import suite
from ameba.equations import format_equations
from ameba_graph import Graph, Node
from ameba_graph.serialization import graph_to_dict
from ameba_genome import GAConfig, GeneticAlgorithm, GenomeFitness, Grammar, model_size, phenotype_key
from ameba_genome.statespace import StateSpaceConfig, StateSpaceEncoding


ARMS = ("raw", "zeros", "bands", "oracle", "oracle-delay")
STATE_LABELS = ("y(k)", "y(k-1)", "y(k-2)", "u(k-1)")


@dataclass(frozen=True)
class OracleStates:
    delay: int = 0

    def __post_init__(self):
        if self.delay < 0:
            raise ValueError("State delay must be non-negative")

    def augment(self, rows):
        if any(len(row) != 1 for row in rows):
            raise ValueError("This diagnostic is only for the single-input Narendra plant")
        controls = tuple(float(row[0]) for row in rows)
        # Reuse the actual numerical model. Only its PAST responses are read.
        responses = narendra.narendra_trajectory(controls).responses
        states = [
            tuple(responses[k - lag] if k >= lag else 0.0 for lag in (1, 2, 3))
            + (controls[k - 1] if k else 0.0,)
            for k in range(len(controls))
        ]
        return tuple(
            (u,) + (states[k - self.delay] if k >= self.delay else (0.0,) * 4)
            for k, u in enumerate(controls)
        )


def input_provider(arm, delay):
    if arm == "raw":
        return None
    if arm == "zeros":
        return bank_from_spec("dummy:zero:4")
    if arm == "bands":
        return bank_from_spec("bands:2:2,3,4,5:norm")
    if arm in ("oracle", "oracle-delay"):
        return OracleStates(delay if arm == "oracle-delay" else 0)
    raise ValueError(f"Unknown arm: {arm}")


def readout_reference():
    """Exact arithmetic readout, with the reference's delays replaced by inputs.

    This positive control is evaluated separately, never seeded into the GA.
    """
    original = narendra.reference_graph()
    indices = {name: i + 1 for i, name in enumerate((*narendra.RESPONSE_DELAYS, narendra.CONTROL_DELAY))}
    return Graph(
        [Node(n.id, "input", {"index": indices[n.id]}) if n.id in indices else n
         for n in original.nodes.values()],
        [edge for edge in original.edges.values() if edge.target not in indices],
    )


def positive_control(family):
    reference = readout_reference()
    results = {
        split: suite(BENCHMARKS["narendra"], split, family=family, bank=OracleStates()).ratios(reference)
        for split in ("train", "validation", "test")
    }
    worst = max(value for scores in results.values() for value in scores.values())
    if not math.isfinite(worst) or worst > 1e-20:
        raise RuntimeError(f"Oracle timing/readout control failed: {worst}")
    return results


def run_arm(arm, seed, args):
    bank = input_provider(arm, args.delay)
    train, validation, test = [
        suite(BENCHMARKS["narendra"], split, family=args.data, bank=bank)
        for split in ("train", "validation", "test")
    ]
    width = len(train.members[0][1].dataset.inputs[0])
    grammar = Grammar(width, 1)
    encoding = StateSpaceEncoding(grammar, config=StateSpaceConfig(
        output_kinds="static", output_sources=2,
        source_layout="inputs-last", inheritance="maternal",
    ))
    fitness = GenomeFitness(train, grammar, codebook=encoding, node_penalty=0.003)
    config = GAConfig(population_size=args.population, shorter_wins_ties=False)

    def breed(first, second, rng):
        child = encoding.crossover(first, second, rng) if second is not None else first
        child = encoding.mutate(child, rng)
        while rng.random() < 0.3:
            child = encoding.mutate(child, rng)
        if len(child) > config.max_genes:
            child = encoding.mutate(first, rng)
            if len(child) > config.max_genes:
                child = first
        return child

    history = []
    started = perf_counter()

    def progress(generation, genomes, scores):
        raw = min(fitness.raw_many(genomes))
        history.append(raw)
        if args.progress and (generation % args.progress == 0 or generation == args.generations):
            print(f"{arm} seed {seed} gen {generation}/{args.generations} train {raw:.5f}", flush=True)

    rng = Random(seed)
    initial = [encoding.random_genome(rng) for _ in range(args.population)]
    result = GeneticAlgorithm(breed, fitness, config, seed, initial=encoding.random_genome).run(
        initial, args.generations, progress)
    shortlist, seen = [], set()
    for genome in sorted(result.population, key=lambda g: (fitness.raw(g), len(g))):
        key = phenotype_key(fitness.decode(genome))
        if key not in seen:
            seen.add(key)
            shortlist.append(genome)
        if len(shortlist) == 30:
            break
    selected = min(shortlist, key=lambda g: (validation.evaluate(fitness.decode(g)), fitness.raw(g)))
    graph = fitness.decode(selected)
    ratios = test.ratios(graph)
    return {
        "diagnostic_only": True, "arm": arm, "seed": seed,
        "inputs": ["u(k)"] + (list(STATE_LABELS) if arm == "oracle" else
                   [f"state[{i}](k-{args.delay})" for i in range(4)] if arm == "oracle-delay" else
                   [f"feature[{i}]" for i in range(width - 1)]),
        "train": fitness.raw(selected), "validation": validation.evaluate(graph),
        "test": ratios, "test_mean": statistics.mean(ratios.values()),
        "nodes": model_size(graph), "genome": list(selected),
        "graph": graph_to_dict(graph), "equations": format_equations(graph),
        "train_best_history": history, "simulations": fitness.simulations,
        "seconds": perf_counter() - started,
    }


def source_fingerprint():
    digest = hashlib.sha256()
    for path in sorted([*ROOT.joinpath("src").rglob("*.py"), Path(__file__).resolve()]):
        digest.update(path.relative_to(ROOT).as_posix().encode())
        digest.update(path.read_bytes())
    return digest.hexdigest()


def json_safe(value):
    if isinstance(value, float) and not math.isfinite(value):
        return None
    if isinstance(value, dict):
        return {key: json_safe(item) for key, item in value.items()}
    if isinstance(value, (list, tuple)):
        return [json_safe(item) for item in value]
    return value


def save(path, value):
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(json_safe(value), indent=2, allow_nan=False), encoding="utf-8")
    temporary.replace(path)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check-only", action="store_true")
    parser.add_argument("--arms", nargs="+", choices=ARMS, default=list(ARMS))
    parser.add_argument("--data", choices=("diverse", "spectral", "octaves"), default="octaves")
    parser.add_argument("--delay", type=int, default=1, help="extra delay of oracle states only")
    parser.add_argument("--generations", type=int, default=200)
    parser.add_argument("--population", type=int, default=100)
    parser.add_argument("--seeds", type=int, default=4)
    parser.add_argument("--first-seed", type=int, default=1)
    parser.add_argument("--progress", type=int, default=50)
    parser.add_argument("--out", type=Path, default=ROOT / "results" / "oracle-probe")
    args = parser.parse_args(argv)
    if args.delay < 1 or args.generations < 0 or args.population < 3 or args.seeds < 1 or args.progress < 0:
        parser.error("require delay >= 1, generations >= 0, population >= 3, seeds >= 1, progress >= 0")
    if len(set(args.arms)) != len(args.arms):
        parser.error("each arm may appear only once")
    control = positive_control(args.data)
    print("DIAGNOSTIC: exact simulator state, not a learned observer or u-only identification.")
    print(f"Exact readout control, worst ratio: {max(v for s in control.values() for v in s.values()):.3g}")
    if args.check_only:
        return 0
    settings = {k: v for k, v in vars(args).items() if k not in ("out", "progress", "check_only")}
    manifest = {"settings": settings, "source_sha256": source_fingerprint(), "positive_control": control}
    args.out.mkdir(parents=True, exist_ok=True)
    manifest_path = args.out / "manifest.json"
    if manifest_path.exists():
        if json.loads(manifest_path.read_text(encoding="utf-8")) != manifest:
            parser.error("output directory belongs to different settings or source; choose a new --out")
    else:
        if list(args.out.glob("*.json")):
            parser.error("output directory has results without a manifest; choose a new --out")
        save(manifest_path, manifest)
    runs = []
    for seed in range(args.first_seed, args.first_seed + args.seeds):
        for arm in args.arms:
            path = args.out / f"{arm}-s{seed}.json"
            if path.exists():
                run = json.loads(path.read_text(encoding="utf-8"))
            else:
                if source_fingerprint() != manifest["source_sha256"]:
                    raise RuntimeError("Source changed during the probe; stopped before starting another run")
                run = run_arm(arm, seed, args)
                save(path, run)
            runs.append(run)
            score = run["test_mean"] if run["test_mean"] is not None else math.inf
            print(f"{arm:13} seed {seed:3} test {score:.5f}", flush=True)
    summary = {}
    for arm in args.arms:
        selected = [run for run in runs if run["arm"] == arm]
        values = [run["test_mean"] if run["test_mean"] is not None else math.inf for run in selected]
        summary[arm] = {
            "runs": len(values), "median_test": statistics.median(values),
            "median_by_signal": {
                label: statistics.median(r["test"][label] if r["test"][label] is not None else math.inf
                                         for r in selected)
                for label in selected[0]["test"]
            },
        }
    save(args.out / "summary.json", summary)
    print("Median test ratios (same raw-input static baseline):")
    for arm, values in summary.items():
        print(f"  {arm:13} {values['median_test']:.5f}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
