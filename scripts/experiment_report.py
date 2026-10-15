"""Build the HTML convergence report of one GA experiment.

Reads every result file of the experiment named in the spec and writes
``report.html`` into its result directory: the best and median score over
generations, population diversity, genome size, how often offspring hit the
size cap, and what crossover and mutation each do to a child. Open the file in
any browser; it needs no server.

    python scripts/experiment_report.py experiments/ga-crossover.json
"""

import html
import json
import statistics
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(ROOT / "src"))
POINTS = 150
KEYS = ("better", "same", "equal", "worse", "invalid")


def column_stats(runs, key, fn):
    length = min(len(run[key]) for run in runs)
    out = []
    for index in range(length):
        column = [run[key][index] for run in runs if run[key][index] is not None]
        out.append(fn(column) if column else None)
    return out


def thin(values):
    step = max(1, len(values) // POINTS)
    picked = list(range(0, len(values), step))
    if picked[-1] != len(values) - 1:
        picked.append(len(values) - 1)
    return picked, [None if values[i] is None else round(values[i], 6) for i in picked]


def shares(runs, key, lo_f=0.0, hi_f=1.0):
    totals = dict.fromkeys(KEYS, 0)
    for run in runs:
        rows = run.get(key) or []
        for row in rows[int(len(rows) * lo_f):int(len(rows) * hi_f)]:
            for k in KEYS:
                totals[k] += row[k]
    count = sum(totals.values()) or 1
    return {k: v / count for k, v in totals.items()}


def held_out(runs) -> dict:
    """Test-set ratios: of the training best, and of the validation pick.

    Each run's test score is the mean ratio over the test suite's signals; the
    summary is the median across runs, and how many runs are below one on the
    test set as a whole. Runs from before test scoring was added have none.
    """
    out = {}
    for name, key in (("test", "best_model"), ("test_selected", "selected_model")):
        means = [sum(r[key]["test"].values()) / len(r[key]["test"])
                 for r in runs if r.get(key) and r[key].get("test")]
        out[name] = statistics.median(means) if means else None
        out[name + "_below"] = sum(1 for v in means if v < 1.0) if means else None
    # Per test signal, the median ratio of the validation pick (or the
    # training best where there is none): the spectral chart's points.
    by_signal: dict[str, list[float]] = {}
    for run in runs:
        model = run.get("selected_model") or run.get("best_model") or {}
        for label, value in (model.get("test") or {}).items():
            by_signal.setdefault(label, []).append(value if value == value else float("inf"))
    out["test_by_signal"] = {label: statistics.median(values) for label, values in by_signal.items()}
    return out


def block(directory: Path, benchmark: str, series):
    result = {"methods": {}}
    for item in series:
        paths = sorted(directory.glob(f"{benchmark}-{item['key']}-s*.json")) + sorted(
            directory.glob(f"{benchmark}-{item['key']}.json"))
        if not paths:
            continue
        loaded = [json.loads(p.read_text()) for p in paths]
        runs = [run for data in loaded for run in data["runs"]]
        population = loaded[0]["population"]
        entry = {}
        for key in ("best", "median", "unique", "length", "nodes"):
            values = column_stats(runs, key, statistics.median)
            if key == "unique":
                values = [v / population for v in values]
            picked, entry[key] = thin(values)
            entry["x"] = picked
        if all(run.get("cap_hits") for run in runs):
            cap = [0.0] + column_stats(runs, "cap_hits", statistics.mean)
            entry["cap"] = [None if v is None else round(v, 5) for v in (cap[i] if i < len(cap) else None for i in entry["x"])]
        else:
            entry["cap"] = [0 for _ in entry["x"]]
        finals = [run["best"][-1] for run in runs]
        entry["summary"] = {
            "seeds": len(runs), "below": sum(1 for v in finals if v < 1.0),
            "best": min(finals), "median": statistics.median(finals),
            "sims": statistics.mean(run["simulations"] for run in runs),
            "seconds": statistics.mean(run["seconds"] for run in runs),
            "unique": statistics.mean(run["unique"][-1] for run in runs) / population,
            "length": statistics.mean(run["length"][-1] for run in runs),
            "nodes": statistics.mean(run["nodes"][-1] for run in runs),
            "restarts": statistics.mean(sum(run.get("restarts") or [0]) for run in runs),
            "finals": finals,
            **held_out(runs),
        }
        for prefix, key in (("", "outcomes"), ("crossover_", "crossover_outcomes"), ("mutation_", "mutation_outcomes")):
            entry[prefix + "outcomes"] = shares(runs, key)
            entry[prefix + "outcomesEarly"] = shares(runs, key, 0.0, 0.1)
            entry[prefix + "outcomesLate"] = shares(runs, key, 0.5, 1.0)
        result["methods"][item["key"]] = entry
        result["generations"] = loaded[0]["generations"]
        result["population"] = population
    return result


PALETTE = ["var(--s1)", "var(--s2)", "var(--s3)", "var(--s4)", "var(--s5)", "var(--s6)"]
REFERENCE_STYLES = [("var(--ref1)", "6 4"), ("var(--ref2)", "2 3"), ("var(--ref1)", "2 3")]
#: Above this many series the charts show only the leaders and the references.
CHART_LIMIT = 8


def chart_series(series, first_block):
    """The series to draw, and every series for the table, best first.

    With a handful of series every one is drawn in the colours its spec gives.
    With dozens, lines would be unreadable, so the charts show the six with
    the lowest median final score plus the references, coloured here; the
    table still lists them all.
    """
    methods = first_block["methods"]
    present = [item for item in series if item["key"] in methods]
    ranked = sorted(present, key=lambda item: methods[item["key"]]["summary"]["median"])
    table = [{"key": item["key"], "label": item["label"], "group": item.get("group", "")} for item in ranked]
    if len(present) <= CHART_LIMIT:
        drawn = [{k: item.get(k, "") for k in ("key", "label", "color", "dash")} for item in present]
        return drawn, table
    references = [item for item in present if item.get("group") == "reference"]
    leaders = [item for item in ranked if item.get("group") != "reference"][:len(PALETTE)]
    drawn = [{"key": item["key"], "label": item["label"], "color": color, "dash": ""}
             for item, color in zip(leaders, PALETTE)]
    drawn += [{"key": item["key"], "label": item["label"], "color": color, "dash": dash}
              for item, (color, dash) in zip(references, REFERENCE_STYLES)]
    return drawn, table


def cards(series):
    out = []
    for item in series:
        dash = {"6 4": " dash", "2 3": " dot"}.get(item.get("dash", ""), "")
        color = item.get("color") or "var(--muted)"
        example = f'<div class="mut">{item["example"]}</div>' if item.get("example") else ""
        out.append(
            f'<div class="method" style="--c: {color}"><div class="key"><span class="swatch{dash}"></span>'
            f'{html.escape(item["label"])}</div><p>{item["desc"]}</p>{example}</div>')
    return "\n    ".join(out)


SIGNAL_LABELS = {
    "noise-11": "nov šum (seme 11)",
    "noise-23": "nov šum (seme 23)",
    "sine": "počasna sinusoida",
    "steps": "dolge stopnice",
    **{f"band-{i + 1}": f"pas {i + 1} ({lo:g}–{hi:g})".replace(".", ",")
       for i, (lo, hi) in enumerate(((0.004, 0.01), (0.01, 0.025), (0.025, 0.06), (0.06, 0.15), (0.15, 0.3), (0.3, 0.5)))},
}


def _family(spec) -> str:
    common = spec.get("common", [])
    data = common[common.index("--data") + 1] if "--data" in common else "single"
    return data if data in ("spectral", "octaves") else "diverse"


def _hold(spec) -> int:
    common = spec.get("common", [])
    return int(common[common.index("--hold") + 1]) if "--hold" in common else 10


def timeseries(spec, directory: Path, benchmark_name: str, keys) -> dict | None:
    """Plant and model responses over time, for the series the charts draw.

    For each series two runs are shown: the representative one (median test
    error) and the best. The model is the validation pick where a run has one,
    otherwise the training best. Signals are the test suite plus the first
    training trajectory, so a viewer sees both what the model was fitted to
    and what it never saw.
    """
    from ameba.benchmarks import BENCHMARKS
    from ameba.benchmarks.splits import suite
    from ameba_graph.serialization import graph_from_dict
    from ameba_signal import SignalSimulationError, SignalSimulator

    benchmark = BENCHMARKS[benchmark_name]
    hold = _hold(spec)
    from ameba.benchmarks.filterbank import bank_from_spec
    common = spec.get("common", [])
    # The bank of each series is in its own args; the panels need the raw input
    # anyway, so suites are built per series below when a series has one.
    test = suite(benchmark, "test", hold, family=_family(spec))
    train = suite(benchmark, "train", hold, family=_family(spec))
    signals = [("train", "učni šum (seme 7)", train.members[0])] + [
        ("test", SIGNAL_LABELS.get(label, label), member) for member in test.members for label in [member[0]]
    ]

    def rows(values):
        return [[round(float(value), 5) for value in channel] for channel in zip(*values)]

    out_signals = [
        {"label": label, "kind": kind, "plant": rows(evaluator.dataset.outputs),
         "inputs": rows([row[:benchmark.channels] for row in evaluator.dataset.inputs])}
        for kind, label, (_, evaluator, _) in signals
    ]
    models = {}
    args_of = {item["key"]: common + item.get("args", []) for item in spec["series"]}

    def bank_of(key):
        args = args_of.get(key, [])
        return bank_from_spec(args[len(args) - 1 - args[::-1].index("--filterbank") + 1]) if "--filterbank" in args else None

    for key in keys:
        bank = bank_of(key)
        runs = []
        for path in sorted(directory.glob(f"{benchmark_name}-{key}-s*.json")) + sorted(
                directory.glob(f"{benchmark_name}-{key}.json")):
            for run in json.loads(path.read_text())["runs"]:
                model = run.get("selected_model") or run.get("best_model")
                if model and model.get("graph"):
                    runs.append((run["seed"], model))
        if not runs:
            continue
        scored = []
        for seed, model in runs:
            graph = graph_from_dict(model["graph"])
            ratios = model.get("test") or suite(benchmark, "test", hold, family=_family(spec), bank=bank).ratios(graph)
            values = list(ratios.values())
            mean = sum(values) / len(values) if all(v == v and v != float("inf") for v in values) else float("inf")
            scored.append((mean, seed, model, graph, ratios))
        scored.sort(key=lambda item: item[0])
        picks = {"median": scored[(len(scored) - 1) // 2], "best": scored[0]}
        models[key] = {}
        for name, (mean, seed, model, graph, ratios) in picks.items():
            outputs = []
            for _, _, (_, evaluator, _) in signals:
                try:
                    session = SignalSimulator().start(graph)
                    inputs = bank.augment(evaluator.dataset.inputs) if bank else evaluator.dataset.inputs
                    outputs.append(rows([session.step(row) for row in inputs]))
                except SignalSimulationError:
                    outputs.append(None)
            models[key][name] = {
                "seed": seed, "test_mean": mean if mean != float("inf") else None,
                "test": {SIGNAL_LABELS.get(k, k): (v if v == v and v != float("inf") else None) for k, v in ratios.items()},
                "equations": model.get("equations", ""),
                "nodes": len(model["graph"]["nodes"]) - 2,
                "outputs": outputs,
            }
    return {"signals": out_signals, "models": models} if models else None


def main(spec_path: str) -> None:
    spec = json.loads(Path(spec_path).read_text(encoding="utf-8"))
    directory = ROOT / spec["dir"]
    series = spec["series"]
    data = {"big": [], "default": spec["benchmarks"][0]}
    for benchmark in spec["benchmarks"]:
        data[benchmark] = block(directory, benchmark, series)
    data["series"], data["table"] = chart_series(series, data[spec["benchmarks"][0]])
    data["timeseries"] = timeseries(spec, directory, spec["benchmarks"][0], [item["key"] for item in data["series"]])
    template = (HERE / "experiment_report.html").read_text(encoding="utf-8")
    for marker, value in (("/*TITLE*/", spec["title"]), ("/*EYEBROW*/", spec["eyebrow"]), ("/*H1*/", spec["h1"]),
                          ("/*LEDE*/", spec["lede"]), ("/*CARDS_H2*/", spec["cards_h2"]), ("/*CARDS*/", cards(series))):
        template = template.replace(marker, value)
    findings = spec.get("findings") or "<li>Še ni zapisanih ugotovitev; dodaj jih v polje \"findings\" v opisu preizkusa.</li>"
    template = template.replace("<!--FINDINGS-->", findings)
    template = template.replace("/*DATA*/null", json.dumps(data))
    out = directory / "report.html"
    out.write_text(template, encoding="utf-8")
    print(f"report written to {out}")


if __name__ == "__main__":
    main(sys.argv[1])
