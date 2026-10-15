"""Run every job of a GA experiment in parallel, then build its report.

An experiment is one JSON file in ``experiments/``. It names the benchmarks,
the number of seeds, the arguments every run shares, and one entry per series
with the arguments that make it different. Each (benchmark, series, seed) is
one call to ``scripts/encoding_experiment.py`` writing one JSON file into the
experiment's result directory.

While it runs, a status table is printed every ``--refresh`` seconds: per
series, how many runs finished, how many of those are below the memoryless
floor, their median and best, and how far the running ones have got. A line is
also printed whenever a run finishes.

Jobs whose result file already exists are skipped, so an interrupted batch
resumes where it stopped; delete the directory to start over.

    python scripts/run_experiments.py experiments/wb-genome.json
    python scripts/run_experiments.py experiments/wb-genome.json --cpu 0.5 --refresh 30
    python scripts/run_experiments.py experiments/wb-genome.json --report-only

Do not edit the code under ``src/`` while a batch runs: each job imports it
when it starts, so a later job would run a different program.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import statistics
import subprocess
import sys
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from pathlib import Path
from time import perf_counter

ROOT = Path(__file__).resolve().parent.parent
PROGRESS = re.compile(r"progress gen (\d+)/(\d+) best (\S+)")


class Job:
    def __init__(self, benchmark: str, series: dict, seed: int, out: Path, command: list[str]) -> None:
        self.benchmark, self.series, self.seed = benchmark, series, seed
        self.out, self.command = out, command
        self.log = out.with_suffix(".log")
        self.state = "done" if out.exists() else "pending"
        self.final: float | None = None
        self.code = 0

    @property
    def key(self) -> str:
        return self.series["key"]

    def progress(self) -> tuple[int, int, float] | None:
        """(generation, generations, best) from the last progress line, if any."""
        try:
            with self.log.open("rb") as handle:
                handle.seek(0, os.SEEK_END)
                handle.seek(max(0, handle.tell() - 2000))
                tail = handle.read().decode(errors="replace")
        except OSError:
            return None
        matches = PROGRESS.findall(tail)
        if not matches:
            return None
        generation, total, best = matches[-1]
        return int(generation), int(total), float(best)

    def result(self) -> float | None:
        if self.final is None and self.out.exists():
            try:
                self.final = json.loads(self.out.read_text())["runs"][0]["best"][-1]
            except (OSError, ValueError, KeyError, IndexError):
                return None
        return self.final


def plan(spec: dict, results: Path) -> list[Job]:
    """Every job, seed by seed across the series.

    Interleaving matters: with fewer workers than jobs, a series-by-series
    order would run the first few series to completion before the rest even
    start, and the status table would show nothing about them for hours.
    """
    jobs = []
    for seed in range(1, spec["seeds"] + 1):
        for benchmark in spec["benchmarks"]:
            for series in spec["series"]:
                out = results / f"{benchmark}-{series['key']}-s{seed}.json"
                command = [
                    sys.executable, str(ROOT / "scripts" / "encoding_experiment.py"),
                    "--benchmark", benchmark, "--seeds", "1", "--first-seed", str(seed),
                    "--label", series["key"], "--out", str(out),
                    *spec.get("common", []), *spec.get("benchmark_args", {}).get(benchmark, []),
                    *series.get("args", []),
                ]
                jobs.append(Job(benchmark, series, seed, out, command))
    return jobs


def lowered(priority: str) -> dict:
    """Keyword arguments that start a run below normal priority.

    The runs then take only the processor time nothing else wants, so the
    machine stays usable for other work while a batch runs.
    """
    if priority != "low":
        return {}
    if os.name == "nt":
        return {"creationflags": subprocess.BELOW_NORMAL_PRIORITY_CLASS}
    return {"preexec_fn": lambda: os.nice(10)}


class KeepAwake:
    """Stop Windows from sleeping while a batch runs; a no-op elsewhere.

    A machine left alone for a weekend goes to sleep, and every run with it.
    The request lasts only while this process holds it, so the normal power
    settings return the moment the batch ends or is stopped.
    """

    def __enter__(self):
        if os.name == "nt":
            import ctypes
            # ES_CONTINUOUS | ES_SYSTEM_REQUIRED: keep the system, not the display, on.
            ctypes.windll.kernel32.SetThreadExecutionState(0x80000000 | 0x00000001)
        return self

    def __exit__(self, *exc):
        if os.name == "nt":
            import ctypes
            ctypes.windll.kernel32.SetThreadExecutionState(0x80000000)


def clock(seconds: float) -> str:
    seconds = max(0, int(seconds))
    return f"{seconds // 3600}:{seconds % 3600 // 60:02d}:{seconds % 60:02d}"


#: Above this many series the status table lists only the leaders and groups.
COMPACT_ABOVE = 15
LEADERS = 12


def table(jobs: list[Job], started: float, title: str) -> str:
    """The status block: one row per series, then totals and an estimate."""
    finished = [job for job in jobs if job.state == "done"]
    running = [job for job in jobs if job.state == "running"]
    fraction = 0.0
    lines = []
    rows = []
    width = max(len(job.series.get("label", job.key)) for job in jobs)
    lines.append(f"  {'različica':<{width}}  končani  pod mejo  mediana  najboljše   "
                 f"tečejo: koliko · gen · najboljše (mediana)")
    for key in dict.fromkeys(job.key for job in jobs):
        group = [job for job in jobs if job.key == key]
        finals = [job.result() for job in group if job.state == "done"]
        finals = [value for value in finals if value is not None]
        live = [(job, job.progress()) for job in group if job.state == "running"]
        live = [(job, p) for job, p in live if p is not None]
        for job in group:
            if job.state == "done":
                fraction += 1.0
            elif job.state == "running":
                p = job.progress()
                fraction += p[0] / p[1] if p else 0.0
        below = sum(1 for value in finals if value < 1.0)
        median = f"{statistics.median(finals):.3f}" if finals else "—"
        best = f"{min(finals):.3f}" if finals else "—"
        if live:
            gens = min(p[0] for _, p in live), max(p[0] for _, p in live)
            total = live[0][1][1]
            span = f"{gens[0]}" if gens[0] == gens[1] else f"{gens[0]}–{gens[1]}"
            bests = [p[2] for _, p in live]
            running_text = (f"{len(live)} · {span}/{total} · {min(bests):.3f}"
                            f" (mediana {statistics.median(bests):.3f})")
        else:
            running_text = ""
        label = group[0].series.get("label", key)
        # Rank by what is known: finished medians first, else the running best.
        known = finals or [p[2] for _, p in live]
        rank = statistics.median(known) if known else float("inf")
        rows.append((rank, group[0].series.get("group", ""), finals, live, (
            f"  {label:<{width}}  {len(finals):>3}/{len(group):<3}  {below:>8}  {median:>7}  {best:>9}   {running_text}")))
    if len(rows) <= COMPACT_ABOVE:
        lines += [row[-1] for row in rows]
    else:
        # Too many series to list every minute: the leaders, then one line per group.
        lines += [row[-1] for row in sorted(rows, key=lambda row: row[0])[:LEADERS]]
        lines.append(f"  … in še {len(rows) - LEADERS} različic; po skupinah:")
        for name in dict.fromkeys(row[1] for row in rows):
            members = [row for row in rows if row[1] == name]
            finals_all = [v for row in members for v in row[2]]
            started_runs = sum(len(row[2]) + len(row[3]) for row in members)
            best_known = min((row[0] for row in members), default=float("inf"))
            best_text = f"{best_known:.3f}" if best_known != float("inf") else "—"
            lines.append(f"    {name or '(brez skupine)':<32} {len(members):>3} različic · {len(finals_all):>4} končanih · "
                         f"{started_runs:>4} začetih · najboljša mediana {best_text}")
    elapsed = perf_counter() - started
    share = fraction / len(jobs)
    eta = clock(elapsed * (1 - share) / share) if share > 0.01 else "—"
    header = (f"── {datetime.now():%H:%M:%S} · {title} · končanih {len(finished)}/{len(jobs)} · "
              f"teče {len(running)} · napredek {share:.0%} · pretečeno {clock(elapsed)} · do konca ≈ {eta} ──")
    return "\n".join([header, *lines])


def main(argv: list[str] | None = None) -> int:
    sys.stdout.reconfigure(line_buffering=True, encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("spec", help="experiment JSON file")
    parser.add_argument("--cpu", type=float, default=0.8,
                        help="fraction of logical cores to use (default 0.8)")
    parser.add_argument("--jobs", type=int, default=None,
                        help="runs at once; overrides --cpu")
    parser.add_argument("--priority", choices=("low", "normal"), default="low",
                        help="process priority of the runs (default low, so the PC stays responsive)")
    parser.add_argument("--refresh", type=int, default=60, help="seconds between status tables")
    parser.add_argument("--report-only", action="store_true", help="skip running, rebuild the report")
    parser.add_argument("--stop-after", type=float, default=None,
                        help="hours after which no new run starts; running ones finish, then the report is built")
    parser.add_argument("--allow-sleep", action="store_true", help="do not keep the machine awake")
    args = parser.parse_args(argv)
    if args.jobs is None:
        args.jobs = max(1, int((os.cpu_count() or 2) * args.cpu))

    spec_path = Path(args.spec)
    spec = json.loads(spec_path.read_text(encoding="utf-8"))
    results = ROOT / spec["dir"]
    results.mkdir(parents=True, exist_ok=True)
    title = spec.get("title", spec_path.stem)

    if not args.report_only:
        jobs = plan(spec, results)
        pending = [job for job in jobs if job.state == "pending"]
        print(f"{spec_path.name}: {len(jobs)} zagonov, {len(jobs) - len(pending)} že končanih, "
              f"{len(pending)} za zagnati, {args.jobs} hkrati od {os.cpu_count()} jeder, "
              f"prioriteta {'nizka' if args.priority == 'low' else 'normalna'}")
        print(f"rezultati: {results}")
        started = perf_counter()
        stop = threading.Event()

        deadline = None if args.stop_after is None else started + args.stop_after * 3600

        def run(job: Job) -> Job:
            if deadline is not None and perf_counter() > deadline:
                job.state = "skipped"
                return job
            job.state = "running"
            with job.log.open("w") as handle:
                job.code = subprocess.call(job.command, cwd=ROOT, stdout=handle, stderr=subprocess.STDOUT,
                                           **lowered(args.priority))
            job.state = "done" if job.code == 0 else "failed"
            if job.code == 0:
                job.log.unlink(missing_ok=True)
            return job

        def monitor() -> None:
            while not stop.wait(args.refresh):
                print(table(jobs, started, title))

        watcher = threading.Thread(target=monitor, daemon=True)
        watcher.start()
        failures = 0
        awake = None if args.allow_sleep else KeepAwake().__enter__()
        if awake:
            print("računalnik ostane buden, dokler serija teče")
        if deadline is not None:
            print(f"novi zagoni se ne začnejo več po {args.stop_after:g} h")
        try:
            with ThreadPoolExecutor(args.jobs) as pool:
                futures = [pool.submit(run, job) for job in pending]
                for done, future in enumerate(as_completed(futures), 1):
                    job = future.result()
                    if job.state == "skipped":
                        continue
                    if job.state == "failed":
                        failures += 1
                        print(f"  NAPAKA  {job.out.name} (glej {job.log.name})")
                    else:
                        final = job.result()
                        verdict = "pod mejo" if final is not None and final < 1.0 else "ni pod mejo"
                        print(f"  [{done}/{len(pending)}] {job.series.get('label', job.key)}, seme {job.seed}: "
                              f"{final:.4f} × meja ({verdict})  {clock(perf_counter() - started)}")
        except KeyboardInterrupt:
            print("\nPrekinjeno. Končani zagoni so shranjeni; ponovni zagon nadaljuje.")
            stop.set()
            return 1
        finally:
            if awake:
                awake.__exit__(None, None, None)
        stop.set()
        skipped = sum(1 for job in jobs if job.state == "skipped")
        if skipped:
            print(f"{skipped} zagonov ni bilo začetih zaradi --stop-after; ponovni zagon jih opravi")
        print(table(jobs, started, title))
        if failures:
            print(f"{failures} zagonov ni uspelo; poročilo zajame ostale")

    subprocess.check_call([sys.executable, str(ROOT / "scripts" / "experiment_report.py"), str(spec_path)], cwd=ROOT)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
