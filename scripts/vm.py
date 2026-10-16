"""Run experiments on a remote machine over SSH.

The address and login come from ``.env`` in the project root
(``VM-NETWORK-IP``, ``USER-NAME``, ``PASSWORD``) and are never printed.
Needs ``paramiko``.

    python scripts/vm.py info                         what the machine has
    python scripts/vm.py push experiments/X.json      code, the spec, and any
                                                      results it already has
    python scripts/vm.py run experiments/X.json       start it in the background
    python scripts/vm.py status experiments/X.json    finished runs, last log lines
    python scripts/vm.py fetch experiments/X.json     copy finished results back
    python scripts/vm.py stop experiments/X.json      stop a running batch

    python scripts/vm.py campaign start campaigns/C.json    copy the code to the
                                                      campaign's own directory and start it
    python scripts/vm.py campaign status campaigns/C.json   phase, progress, log
    python scripts/vm.py campaign fetch campaigns/C.json    reports, analyses and
                                                      state (add --runs for every run)
    python scripts/vm.py campaign pause|resume campaigns/C.json
    python scripts/vm.py campaign update campaigns/C.json   stop, copy the current
                                                      code and definition, start again
    python scripts/vm.py campaign stop campaigns/C.json

A campaign runs from its own copy of the code, so pushing new code for an
experiment never changes a campaign halfway through a phase; ``update`` does
that deliberately, and the campaign resumes where it was.

The code is copied, not cloned, so uncommitted work runs as it is on disk.
A run started on the machine keeps going after this script exits.
"""

from __future__ import annotations

import io
import json
import sys
import tarfile
from pathlib import Path

import paramiko

ROOT = Path(__file__).resolve().parent.parent
REMOTE = "ameba-core"
#: What a run needs; results come along per spec, never wholesale.
CODE = ("src", "scripts", "experiments", "tests", "pyproject.toml")
SKIP = ("__pycache__", ".pyc")


def credentials() -> tuple[str, str, str]:
    values = {}
    for line in (ROOT / ".env").read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.removeprefix("export ").split("=", 1)
            values[key.strip()] = value.strip().strip('"').strip("'")
    return values["VM-NETWORK-IP"], values["USER-NAME"], values["PASSWORD"]


def connect() -> paramiko.SSHClient:
    host, user, password = credentials()
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(host, username=user, password=password, timeout=20)
    return client


def shell(client: paramiko.SSHClient, command: str) -> str:
    _, stdout, stderr = client.exec_command(command)
    out = stdout.read().decode(errors="replace")
    err = stderr.read().decode(errors="replace")
    return out + (("\n" + err) if err.strip() else "")


def results_dir(spec: Path) -> str:
    return json.loads(spec.read_text(encoding="utf-8"))["dir"]


def push(client: paramiko.SSHClient, spec: Path | None, remote: str = REMOTE, results: str | None = None) -> None:
    buffer = io.BytesIO()
    with tarfile.open(fileobj=buffer, mode="w:gz") as tar:
        def keep(info):
            return None if any(part in info.name for part in SKIP) else info
        for name in CODE:
            tar.add(ROOT / name, arcname=name, filter=keep)
        if spec is not None and results is None and (ROOT / results_dir(spec)).exists():
            tar.add(ROOT / results_dir(spec), arcname=results_dir(spec), filter=keep)
        if spec is not None and spec.parts[0] == "campaigns":
            tar.add(ROOT / "campaigns", arcname="campaigns", filter=keep)
    buffer.seek(0)
    shell(client, f"mkdir -p {remote}")
    with client.open_sftp() as sftp:
        sftp.putfo(buffer, f"{remote}/push.tar.gz")
    print(shell(client, f"cd {remote} && tar xzf push.tar.gz && rm push.tar.gz && echo pushed to {remote}"))


def fetch(client: paramiko.SSHClient, spec: Path, remote: str = REMOTE, directory: str | None = None,
          exclude: str = "") -> None:
    directory = directory or results_dir(spec)
    shell(client, f"cd {remote} && tar czf fetch.tar.gz {exclude} {directory}")
    buffer = io.BytesIO()
    with client.open_sftp() as sftp:
        sftp.getfo(f"{remote}/fetch.tar.gz", buffer)
    shell(client, f"rm {remote}/fetch.tar.gz")
    buffer.seek(0)
    with tarfile.open(fileobj=buffer, mode="r:gz") as tar:
        members = [m for m in tar.getmembers() if m.isfile() and m.name.startswith(directory + "/")
                   and ".." not in Path(m.name).parts]
        tar.extractall(ROOT, members=members, filter="data")
    finished = sum(1 for m in members if m.name.endswith(".json"))
    print(f"fetched {len(members)} files into {directory} ({finished} finished runs)")


def campaign(client: paramiko.SSHClient, action: str, definition: Path, extra: list[str]) -> None:
    name = json.loads(definition.read_text(encoding="utf-8"))["name"]
    remote = f"ameba-campaign-{name}"
    results = f"results/campaign-{name}"
    run = f"scripts/campaign.py run {definition.as_posix()}"
    # "[p]ython3" matches the daemon but not the shell that runs pgrep or pkill.
    pattern = f"[p]ython3 {run}"

    def start() -> None:
        jobs = extra[extra.index("--jobs") + 1] if "--jobs" in extra else "30"
        print(shell(client, f"cd {remote} && mkdir -p {results} && nohup python3 {run} --jobs {jobs} --nice 19 "
                            f">> {results}/daemon.log 2>&1 < /dev/null & echo started campaign {name}"))

    def stop() -> None:
        print(shell(client, f"pkill -TERM -f '{pattern}'; sleep 3; echo stopped campaign {name}"))

    if action == "start":
        push(client, definition, remote, results)
        start()
    elif action == "update":
        stop()
        push(client, definition, remote, results)
        start()
    elif action == "stop":
        stop()
    elif action in ("pause", "resume"):
        command = f"touch {results}/PAUSE" if action == "pause" else f"rm -f {results}/PAUSE"
        print(shell(client, f"cd {remote} && {command} && echo {action}d"))
    elif action == "status":
        print(shell(client, f"cd {remote} && cat {results}/status.json 2>/dev/null; echo; "
                            f"echo daemon: $(pgrep -fc '{pattern}'); tail -n 8 {results}/campaign.log 2>/dev/null"))
    elif action == "fetch":
        exclude = "" if "--runs" in extra else "--exclude='*/runs'"
        fetch(client, definition, remote, results, exclude)


def main(argv: list[str]) -> int:
    # The runner's log holds Slovenian text; a Windows console may not.
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if not argv or argv[0] not in ("info", "push", "run", "status", "fetch", "stop", "campaign"):
        print(__doc__)
        return 1
    command = argv[0]
    if command == "campaign":
        if len(argv) < 3 or argv[1] not in ("start", "status", "fetch", "pause", "resume", "update", "stop"):
            print(__doc__)
            return 1
        client = connect()
        try:
            campaign(client, argv[1], Path(argv[2]), argv[3:])
        finally:
            client.close()
        return 0
    spec = Path(argv[1]) if len(argv) > 1 else None
    if command in ("run", "status", "fetch", "stop") and spec is None:
        print(f"{command} needs a spec, e.g. experiments/X.json")
        return 1
    client = connect()
    try:
        if command == "info":
            print(shell(client, "hostname; nproc; free -h | head -2; python3 --version; df -h ~ | tail -1; uptime"))
        elif command == "push":
            push(client, spec)
        elif command == "run":
            name = spec.stem
            print(shell(client, f"cd {REMOTE} && mkdir -p {results_dir(spec)} && "
                                f"nohup python3 scripts/run_experiments.py {spec.as_posix()} --allow-sleep {' '.join(argv[2:])} "
                                f"> {results_dir(spec)}/runner.log 2>&1 < /dev/null & echo started {name}"))
        elif command == "status":
            directory = results_dir(spec)
            print(shell(client, f"cd {REMOTE} && echo finished: $(ls {directory}/*.json 2>/dev/null | wc -l); "
                                f"echo running: $(pgrep -fc '[a]meba-core/scripts/encoding_experiment.py') ; "
                                f"tail -c 1500 {directory}/runner.log 2>/dev/null"))
        elif command == "fetch":
            fetch(client, spec)
        elif command == "stop":
            # Only this checkout's runs: a campaign's workers live in its own directory.
            print(shell(client, f"pkill -f '[r]un_experiments.py {spec.as_posix()}'; "
                                f"pkill -f '[a]meba-core/scripts/encoding_experiment.py'; echo stopped"))
    finally:
        client.close()
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
