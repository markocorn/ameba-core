"""A long-running exploration campaign: screen widely, confirm, move on.

Comparing two variants head to head drowns in seed noise: effects of a few
hundredths need hundreds of seeds. Pooled over many random configurations,
though, every level of every axis gets hundreds of runs, so its effect shows
even when no single configuration is run often. A campaign repeats

``screen``   ``configs`` random combinations of the axes' levels, plus the
             base, each on every benchmark with ``seeds`` seeds and a small
             budget. Every level is judged against the rest of its axis.
``confirm``  the best few configurations, the one made of every axis' best
             level, and the base, with many seeds and the full budget.
             The best becomes the new base if it beats the old one.

and before the next screen drops levels that clearly hurt. Every run is
tested on the six octave bands, whatever it trained on; scores are compared
as percentiles within a benchmark, so the three plants weigh the same.

    python scripts/campaign.py run campaigns/explore.json [--jobs 30] [--nice 19]
    python scripts/campaign.py status campaigns/explore.json
    python scripts/campaign.py report campaigns/explore.json

Runs go to ``results/campaign-<name>/phase-NN-<kind>/``; a run already there
is never redone, so a stopped campaign resumes where it was. Jobs start at a
low priority (``--nice``): anything else started on the machine takes the CPU
first and the campaign slows down meanwhile. A file ``PAUSE`` in the results
directory stops new jobs from starting. The definition is re-read at every
phase, so an axis can be changed while the campaign runs.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import math
import os
import random
import signal
import statistics
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXPERIMENT = ROOT / "scripts" / "encoding_experiment.py"
sys.path.insert(0, str(ROOT / "src"))

from ameba.benchmarks import BENCHMARKS  # noqa: E402
from ameba.benchmarks.filterbank import bank_from_spec  # noqa: E402

#: The gene dictionary names at most this many model inputs.
MAX_INPUTS = 10


# --- configurations ----------------------------------------------------------

#: Families of cells a model may be built from, as --kinds lists. States are
#: delays in every family, so even "linear" has dynamics; memory kinds add
#: filters, integrals and derivatives on top. A power y^u needs no node of its
#: own: exp(u * log|y|) is in the "power" family.
KIND_FAMILIES = {
    "all": None,
    "linear": "add,negate,constant,delay",
    "arithmetic": "arithmetic,constant,delay",
    "polynomial": "add,multiply,negate,square,constant,delay",
    "saturating": "add,multiply,negate,tanh,constant",
    "trigonometric": "add,multiply,negate,sin,constant",
    "power": "add,multiply,negate,exp,log,abs,constant",
    "memoryless": "arithmetic,nonlinear,constant",
    "soft": "soft",
    "soft-mixed": "soft,add",
}


def arguments(config: dict, budget: int) -> list[str]:
    """encoding_experiment.py arguments for one configuration and budget."""
    encoding = {"output_kinds": config["output_kinds"], "state_kinds": config["state_kinds"],
                "output_sources": int(config["output_sources"]), "source_layout": "inputs-last",
                "inheritance": config["inheritance"]}
    addressing = config["addressing"]
    tags = int(addressing.removeprefix("tags")) if addressing.startswith("tags") else None
    if config["codon"] == "records":
        method = "E-codon"
        encoding["codon"] = {"indels": "record", "reset": "role", "alphabet": 0, **({"tags": tags} if tags else {})}
    elif tags:
        method = "E-tag"
        encoding["tags"] = tags
    else:
        method = "E-statespace"
    cells = str(config.get("cells", "1"))
    if cells != "1":
        mode, count = cells.rstrip("0123456789"), int(cells.lstrip("abcdefghijklmnopqrstuvwxyz"))
        encoding["cells"] = {"count": count, "mode": mode}
    population = int(config["population"])
    args = ["--method", method, "--encoding", json.dumps(encoding, separators=(",", ":")),
            "--population", str(population), "--generations", str(max(1, budget // population)),
            "--selection", config["selection"], "--scheme", config["scheme"],
            "--crossover-rate", str(config["crossover_rate"]), "--node-penalty", str(config["node_penalty"]),
            "--immigrants", str(config["immigrants"]), "--data", config["data"],
            "--test-family", "octaves", "--progress", "0"]
    if KIND_FAMILIES[config["kinds"]]:
        args += ["--kinds", KIND_FAMILIES[config["kinds"]]]
    if config["bank"] != "none":
        args += ["--filterbank", config["bank"]]
    if config["islands"] != "none":
        args += ["--islands", config["islands"]]
    if config["clearing"] == "yes":
        args += ["--clearing"]
    if config["restart"] != "none":
        args += ["--restart", config["restart"]]
    return args


def model_inputs(config: dict, benchmark: str) -> int:
    """How many inputs a model has: the plant's, its filters', other cells'."""
    channels = BENCHMARKS[benchmark].channels
    bank = bank_from_spec(config["bank"])
    inputs = channels * (1 + (bank.width() if bank else 0))
    cells = str(config.get("cells", "1"))
    if cells.startswith("chain"):
        inputs += (int(cells.removeprefix("chain")) - 1) * channels
    return inputs


def config_id(config: dict) -> str:
    """A short, stable name for a configuration."""
    canonical = json.dumps({axis: str(value) for axis, value in sorted(config.items())}, sort_keys=True)
    return "c" + hashlib.sha1(canonical.encode()).hexdigest()[:10]


def describe(config: dict, base: dict) -> str:
    """A configuration as its differences from the base."""
    changes = [f"{axis}={config[axis]}" for axis in config if str(config[axis]) != str(base.get(axis))]
    return ", ".join(changes) or "izhodišče"


# --- statistics ----------------------------------------------------------------


def mwu(a: list[float], b: list[float]) -> float:
    """Two-sided Mann-Whitney U, normal approximation with tie correction."""
    n1, n2 = len(a), len(b)
    if not n1 or not n2:
        return float("nan")
    pooled = sorted([(v, 0) for v in a] + [(v, 1) for v in b])
    ranks, ties, i = [0.0] * len(pooled), 0.0, 0
    while i < len(pooled):
        j = i
        while j < len(pooled) and pooled[j][0] == pooled[i][0]:
            j += 1
        for k in range(i, j):
            ranks[k] = (i + j + 1) / 2
        ties += (j - i) ** 3 - (j - i)
        i = j
    r1 = sum(r for r, (_, g) in zip(ranks, pooled) if g == 0)
    u = r1 - n1 * (n1 + 1) / 2
    n = n1 + n2
    variance = n1 * n2 / 12 * ((n + 1) - ties / (n * (n - 1)))
    if variance <= 0:
        return 1.0
    z = (u - n1 * n2 / 2) / math.sqrt(variance)
    return math.erfc(abs(z) / math.sqrt(2))


def holm(pvalues: dict) -> dict:
    order = sorted(pvalues, key=lambda k: pvalues[k])
    adjusted, running = {}, 0.0
    for i, key in enumerate(order):
        running = max(running, min(1.0, pvalues[key] * (len(order) - i)))
        adjusted[key] = running
    return adjusted


def percentiles(scores: list[float]) -> list[float]:
    """Each score's rank among all, 0 (best) to 1 (worst); ties share a rank."""
    order = sorted(range(len(scores)), key=lambda i: scores[i])
    out, i = [0.0] * len(scores), 0
    while i < len(order):
        j = i
        while j < len(order) and scores[order[j]] == scores[order[i]]:
            j += 1
        for k in range(i, j):
            out[order[k]] = ((i + j - 1) / 2) / max(1, len(scores) - 1)
        i = j
    return out


# --- phases --------------------------------------------------------------------


class Campaign:
    def __init__(self, definition: Path) -> None:
        self.definition = definition
        self.load()
        self.root = ROOT / "results" / f"campaign-{self.spec['name']}"
        self.root.mkdir(parents=True, exist_ok=True)
        self.state_path = self.root / "state.json"
        if self.state_path.exists():
            self.state = json.loads(self.state_path.read_text(encoding="utf-8"))
        else:
            self.state = {"phase": 1, "kind": "screen", "base": self.spec["base"], "pruned": {}, "history": []}
            self.save_state()

    def load(self) -> None:
        self.spec = json.loads(self.definition.read_text(encoding="utf-8"))

    def save_state(self) -> None:
        tmp = self.state_path.with_suffix(".tmp")
        tmp.write_text(json.dumps(self.state, indent=1), encoding="utf-8")
        tmp.replace(self.state_path)

    def log(self, message: str) -> None:
        line = f"{time.strftime('%Y-%m-%d %H:%M:%S')} {message}"
        print(line, flush=True)
        with open(self.root / "campaign.log", "a", encoding="utf-8") as handle:
            handle.write(line + "\n")

    def phase_dir(self, phase: int | None = None, kind: str | None = None) -> Path:
        return self.root / f"phase-{phase or self.state['phase']:02d}-{kind or self.state['kind']}"

    def base(self) -> dict:
        """The base, with any axis added since it was set taken from the definition."""
        base = dict(self.state["base"])
        for axis, levels in self.spec["axes"].items():
            base.setdefault(axis, self.spec["base"].get(axis, levels[0]))
        self.state["base"] = base
        return base

    def axes(self) -> dict:
        """The axes' levels, less any pruned ones, with the base's level always kept."""
        axes = {}
        for axis, levels in self.spec["axes"].items():
            keep = [level for level in levels if level not in self.state["pruned"].get(axis, [])]
            base = self.base()[axis]
            axes[axis] = keep if base in keep else [base] + keep
        return axes

    def plan(self) -> dict:
        """The phase's configurations and jobs; made once, then read back."""
        directory = self.phase_dir()
        path = directory / "phase.json"
        if path.exists():
            return json.loads(path.read_text(encoding="utf-8"))
        self.load()
        kind = self.state["kind"]
        settings = self.spec[kind]
        base = self.base()
        if kind == "screen":
            rng = random.Random(f"{self.spec['name']}-{self.state['phase']}")
            axes = self.axes()
            configs = {config_id(base): base}
            while len(configs) < settings["configs"] + 1:
                config = {axis: rng.choice(levels) for axis, levels in axes.items()}
                # A combination some plant cannot have is drawn again, so no
                # level loses runs to it on one plant only.
                if all(model_inputs(config, b) <= MAX_INPUTS for b in self.spec["benchmarks"]):
                    configs.setdefault(config_id(config), config)
        else:
            configs = {config_id(c): c for c in self.state["candidates"]}
            configs.setdefault(config_id(base), base)
        jobs = []
        for seed in range(1, settings["seeds"] + 1):
            for cid in configs:
                for benchmark in self.spec["benchmarks"]:
                    jobs.append({"config": cid, "benchmark": benchmark, "seed": seed})
        plan = {"phase": self.state["phase"], "kind": kind, "budget": settings["budget"], "base": config_id(base),
                "configs": configs, "jobs": jobs, "created": time.strftime("%Y-%m-%d %H:%M:%S")}
        directory.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(plan, indent=1), encoding="utf-8")
        self.log(f"phase {plan['phase']} {kind}: {len(configs)} configurations, {len(jobs)} runs")
        return plan

    def out_path(self, job: dict, directory: Path | None = None) -> Path:
        return (directory or self.phase_dir()) / "runs" / job["benchmark"] / f"{job['config']}-s{job['seed']}.json"

    # --- running ---------------------------------------------------------------

    def run(self, jobs_at_once: int, nice: int) -> None:
        running: dict[Path, tuple[subprocess.Popen, object]] = {}

        def stop(*_):
            for process, _ in running.values():
                process.terminate()
            self.log("stopped")
            sys.exit(0)

        signal.signal(signal.SIGTERM, stop)
        signal.signal(signal.SIGINT, stop)
        self.log(f"campaign {self.spec['name']} running, {jobs_at_once} at once, nice {nice}")
        while True:
            plan = self.plan()
            for path in list(running):
                process, log = running[path]
                code = process.poll()
                if code is None:
                    continue
                log.close()
                del running[path]
                tmp = path.with_suffix(".tmp")
                if code == 0 and tmp.exists():
                    tmp.replace(path)
                    path.with_suffix(".log").unlink(missing_ok=True)
                else:
                    path.with_suffix(".failed").write_text(f"exit {code}\n", encoding="utf-8")
                    self.log(f"failed: {path.relative_to(self.root)} (exit {code})")
            pending = [job for job in plan["jobs"]
                       if not self.out_path(job).exists() and not self.out_path(job).with_suffix(".failed").exists()]
            waiting = [job for job in pending if self.out_path(job) not in running]
            paused = (self.root / "PAUSE").exists()
            while waiting and len(running) < jobs_at_once and not paused:
                job = waiting.pop(0)
                path = self.out_path(job)
                path.parent.mkdir(parents=True, exist_ok=True)
                command = [sys.executable, str(EXPERIMENT), *arguments(plan["configs"][job["config"]], plan["budget"]),
                           "--benchmark", job["benchmark"], "--first-seed", str(job["seed"]), "--seeds", "1",
                           "--out", str(path.with_suffix(".tmp"))]
                log = open(path.with_suffix(".log"), "w", encoding="utf-8")
                process = subprocess.Popen(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT,
                                           preexec_fn=(lambda: os.nice(nice)) if os.name != "nt" else None)
                running[path] = (process, log)
            self.write_status(plan, len(pending), len(running), paused)
            if not pending and not running:
                self.finish_phase(plan)
                continue
            time.sleep(5)

    def write_status(self, plan: dict, pending: int, running: int, paused: bool) -> None:
        status = {"phase": plan["phase"], "kind": plan["kind"], "runs": len(plan["jobs"]),
                  "done": len(plan["jobs"]) - pending, "running": running, "paused": paused,
                  "failed": sum(1 for _ in self.phase_dir().glob("runs/*/*.failed")),
                  "base": plan["base"], "updated": time.strftime("%Y-%m-%d %H:%M:%S")}
        (self.root / "status.json").write_text(json.dumps(status, indent=1), encoding="utf-8")

    # --- analysing ---------------------------------------------------------------

    def results(self, plan: dict, directory: Path) -> list[dict]:
        """One row per finished run: configuration, benchmark, seed, test score."""
        rows = []
        for job in plan["jobs"]:
            path = self.out_path(job, directory)
            if not path.exists():
                continue
            run = json.loads(path.read_text(encoding="utf-8"))["runs"][0]
            test = run["selected_model"]["test"]
            score = sum(test.values()) / len(test)
            rows.append({**job, "score": score if math.isfinite(score) else float("inf"), "bands": test,
                         "equations": run["selected_model"]["equations"], "train": run["best"][-1],
                         "nodes": run["nodes"][-1], "seconds": run["seconds"]})
        by_benchmark: dict[str, list[dict]] = {}
        for row in rows:
            by_benchmark.setdefault(row["benchmark"], []).append(row)
        for group in by_benchmark.values():
            for row, rank in zip(group, percentiles([r["score"] for r in group])):
                row["rank"] = rank
        return rows

    def analyse(self, plan: dict, directory: Path) -> dict:
        rows = self.results(plan, directory)
        configs = plan["configs"]
        effects, pvalues = {}, {}
        for axis in self.spec["axes"]:
            levels = sorted({str(c[axis]) for c in configs.values()})
            if len(levels) < 2:
                continue
            effects[axis] = {}
            for level in levels:
                inside = [r["rank"] for r in rows if str(configs[r["config"]][axis]) == level]
                outside = [r["rank"] for r in rows if str(configs[r["config"]][axis]) != level]
                p = mwu(inside, outside)
                effects[axis][level] = {"runs": len(inside), "median": statistics.median(inside) if inside else None,
                                        "rest": statistics.median(outside) if outside else None, "p": p}
                pvalues[(axis, level)] = p
        for (axis, level), p in holm({k: v for k, v in pvalues.items() if not math.isnan(v)}).items():
            effects[axis][level]["holm"] = p
        per_config: dict[str, list[float]] = {}
        for row in rows:
            per_config.setdefault(row["config"], []).append(row["rank"])
        expected = len(self.spec["benchmarks"]) * self.spec[plan["kind"]]["seeds"]
        ranking = sorted(((statistics.mean(v), cid, len(v)) for cid, v in per_config.items()), key=lambda x: x[0])
        summary = {"phase": plan["phase"], "kind": plan["kind"], "runs": len(rows), "effects": effects,
                   "ranking": [{"config": cid, "mean_rank": m, "runs": n, "complete": n == expected} for m, cid, n in ranking],
                   "per_benchmark": {}}
        for cid in [r[1] for r in ranking[:10]] + [plan["base"]]:
            summary["per_benchmark"][cid] = {
                b: statistics.median([r["score"] for r in rows if r["config"] == cid and r["benchmark"] == b] or [float("nan")])
                for b in self.spec["benchmarks"]}
        best = {}
        for b in self.spec["benchmarks"]:
            group = [r for r in rows if r["benchmark"] == b and math.isfinite(r["score"])]
            if group:
                top = min(group, key=lambda r: r["score"])
                best[b] = {"config": top["config"], "seed": top["seed"], "score": top["score"], "equations": top["equations"]}
        summary["best_models"] = best
        if plan["kind"] == "confirm":
            base_ranks = per_config.get(plan["base"], [])
            summary["versus_base"] = {cid: mwu(v, base_ranks) for cid, v in per_config.items() if cid != plan["base"]}
        (directory / "analysis.json").write_text(json.dumps(summary, indent=1, default=str), encoding="utf-8")
        write_report(self, plan, summary, directory / "report.html")
        return summary

    def finish_phase(self, plan: dict) -> None:
        directory = self.phase_dir()
        summary = self.analyse(plan, directory)
        self.load()
        configs = plan["configs"]
        record = {"phase": plan["phase"], "kind": plan["kind"], "finished": time.strftime("%Y-%m-%d %H:%M:%S")}
        if plan["kind"] == "screen":
            complete = [r["config"] for r in summary["ranking"] if r["complete"]]
            top = complete[: self.spec["confirm"]["top"]]
            # The configuration made of every axis' best level.
            composite = dict(self.state["base"])
            for axis, levels in summary["effects"].items():
                scored = {level: effect for level, effect in levels.items() if effect["median"] is not None}
                if scored:
                    best_level = min(scored, key=lambda l: scored[l]["median"])
                    composite[axis] = next(l for l in self.spec["axes"][axis] if str(l) == best_level)
            candidates = [configs[cid] for cid in top] + [composite]
            self.state["candidates"] = list({config_id(c): c for c in candidates}.values())
            # Drop levels that clearly hurt; never the base's.
            limit = self.spec.get("prune_holm", 0.01)
            for axis, levels in summary["effects"].items():
                for level, effect in levels.items():
                    if (effect.get("holm", 1.0) < limit and effect["median"] is not None and effect["median"] > effect["rest"]
                            and str(self.state["base"][axis]) != level):
                        pruned = self.state["pruned"].setdefault(axis, [])
                        original = next(l for l in self.spec["axes"][axis] if str(l) == level)
                        if original not in pruned:
                            pruned.append(original)
            record["candidates"] = [config_id(c) for c in self.state["candidates"]]
            self.state["kind"] = "confirm"
        else:
            winner = summary["ranking"][0]["config"]
            p = summary["versus_base"].get(winner, 1.0)
            record.update({"winner": winner, "p": p})
            if winner != plan["base"] and p < self.spec.get("adopt_p", 0.05):
                self.state["base"] = configs[winner]
                record["new_base"] = True
                self.log(f"new base: {winner} (p = {p:.3g})")
            self.state["kind"] = "screen"
            self.state["phase"] += 1
        self.state["history"].append(record)
        self.state.pop("candidates", None) if self.state["kind"] == "screen" else None
        if self.state["kind"] == "confirm":
            self.state["phase"] += 1
        self.save_state()
        self.log(f"phase {plan['phase']} {plan['kind']} finished: {summary['runs']} runs analysed")


# --- report --------------------------------------------------------------------


def write_report(campaign: Campaign, plan: dict, summary: dict, path: Path) -> None:
    e = html.escape
    effects_rows = []
    for axis, levels in summary["effects"].items():
        for level, effect in sorted(levels.items(), key=lambda kv: (kv[1]["median"] is None, kv[1]["median"] or 0)):
            if effect["median"] is None or effect["rest"] is None:
                effects_rows.append(f"<tr><td>{e(axis)}</td><td>{e(level)}</td><td>0</td><td colspan=4>brez zagonov</td></tr>")
                continue
            diff = effect["median"] - effect["rest"]
            width = min(100, abs(diff) * 400)
            tone = "good" if diff < 0 else "bad"
            mark = " ★" if effect.get("holm", 1) < 0.05 else ""
            effects_rows.append(
                f"<tr><td>{e(axis)}</td><td>{e(level)}</td><td>{effect['runs']}</td><td>{effect['median']:.3f}</td>"
                f"<td><span class='bar {tone}' style='width:{width:.0f}%'></span>{diff:+.3f}</td>"
                f"<td>{effect['p']:.2g}</td><td>{effect.get('holm', float('nan')):.2g}{mark}</td></tr>")
    config_rows = []
    for row in summary["ranking"][:15]:
        cid = row["config"]
        bench = summary["per_benchmark"].get(cid, {})
        cells = "".join(f"<td>{bench.get(b, float('nan')):.3f}</td>" for b in campaign.spec["benchmarks"])
        vs = summary.get("versus_base", {}).get(cid)
        label = describe(plan["configs"][cid], plan["configs"][plan["base"]]) if cid in plan["configs"] else cid
        config_rows.append(f"<tr><td class='small'>{e(label)}<br><span class='mono muted'>{e(cid)}</span></td><td>{row['mean_rank']:.3f}</td><td>{row['runs']}</td>{cells}"
                           f"<td>{'' if vs is None else f'{vs:.2g}'}</td></tr>")
    models = "".join(
        f"<h3>{e(b)} · test {m['score']:.3f} · seme {m['seed']}</h3>"
        f"<p class='small'>{e(describe(plan['configs'][m['config']], plan['configs'][plan['base']]))}</p>"
        f"<pre>{e(m['equations'] if isinstance(m['equations'], str) else chr(10).join(m['equations']))}</pre>"
        for b, m in summary["best_models"].items())
    history = "".join(f"<li>{e(json.dumps(h, ensure_ascii=False))}</li>" for h in campaign.state["history"])
    bench_heads = "".join(f"<th>{e(b)}</th>" for b in campaign.spec["benchmarks"])
    page = f"""<!doctype html><html lang="sl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kampanja {e(campaign.spec['name'])}</title><style>
:root{{--bg:#fbfaf7;--fg:#1d1d1b;--muted:#6b6a65;--line:#e4e1d8;--good:#2f7d5b;--bad:#b4533a;--panel:#fff}}
@media (prefers-color-scheme:dark){{:root{{--bg:#161614;--fg:#ecebe6;--muted:#a19f97;--line:#33322e;--good:#5fbf92;--bad:#e08a70;--panel:#1e1e1b}}}}
body{{background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif;margin:0;padding:24px 16px}}
main{{max-width:1100px;margin:auto}} h1{{font-size:24px;margin:0 0 4px}} .muted{{color:var(--muted)}}
table{{border-collapse:collapse;width:100%;margin:12px 0 28px;background:var(--panel)}} th,td{{border-bottom:1px solid var(--line);padding:5px 8px;text-align:left;font-variant-numeric:tabular-nums}}
th{{font-weight:600;color:var(--muted);font-size:13px}} .bar{{display:inline-block;height:8px;margin-right:6px;border-radius:2px;vertical-align:middle}}
.good{{background:var(--good)}} .bad{{background:var(--bad)}} .mono{{font-family:ui-monospace,monospace;font-size:12px;word-break:break-all}}
.small{{font-size:12px}} pre{{background:var(--panel);border:1px solid var(--line);padding:10px;overflow-x:auto;font-size:12px}}
.wrap{{overflow-x:auto}}</style></head><body><main>
<h1>Kampanja »{e(campaign.spec['name'])}« · faza {summary['phase']} ({'sito' if summary['kind'] == 'screen' else 'potrditev'})</h1>
<p class="muted">{summary['runs']} zagonov · sistemi: {e(', '.join(campaign.spec['benchmarks']))} · proračun {plan['budget']} ocen na zagon ·
rang = percentil testa znotraj sistema (0 najboljši, 1 najslabši) · test: šest oktavnih pasov</p>
<h2>Učinki ravni</h2><p class="muted small">Mediana ranga zagonov s to ravnjo proti vsem ostalim na isti osi; zeleno = bolje.
★ = značilno po Holmovem popravku čez vse ravni.</p>
<div class="wrap"><table><tr><th>os</th><th>raven</th><th>zagonov</th><th>mediana ranga</th><th>razlika</th><th>p</th><th>Holm</th></tr>{''.join(effects_rows)}</table></div>
<h2>Najboljše konfiguracije</h2><p class="muted small">Povprečni rang čez sisteme in semena; po sistemih mediana testa. Izhodišče: {e(json.dumps(plan['configs'][plan['base']], ensure_ascii=False))}</p>
<div class="wrap"><table><tr><th>konfiguracija</th><th>rang</th><th>zagonov</th>{bench_heads}<th>p proti izhodišču</th></tr>{''.join(config_rows)}</table></div>
<h2>Najboljši modeli</h2>{models}
<h2>Zgodovina</h2><ul class="small">{history}</ul></main></body></html>"""
    path.write_text(page, encoding="utf-8")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("command", choices=("run", "status", "report"))
    parser.add_argument("definition", type=Path)
    parser.add_argument("--jobs", type=int, default=max(1, (os.cpu_count() or 2) - 2))
    parser.add_argument("--nice", type=int, default=19)
    args = parser.parse_args(argv)
    campaign = Campaign(args.definition)
    if args.command == "run":
        campaign.run(args.jobs, args.nice)
    elif args.command == "status":
        status = campaign.root / "status.json"
        print(status.read_text(encoding="utf-8") if status.exists() else "not started")
    else:
        plan = campaign.plan()
        summary = campaign.analyse(plan, campaign.phase_dir())
        print(f"report: {campaign.phase_dir() / 'report.html'} ({summary['runs']} runs)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
