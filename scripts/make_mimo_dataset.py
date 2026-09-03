"""Record a coupled two-input, two-output plant as an identification dataset.

The plant is a stable second-order state-space system in which everything is
coupled: each input drives both states, each state feeds both, and each output
mixes both states.

    x(k+1) = A x(k) + B u(k)
    y(k+1) = C x(k+1)

    A = [[ 0.70, -0.30],   B = [[1.00, 0.30],   C = [[ 1.00, 0.50],
         [ 0.20,  0.80]]        [0.20, 1.00]]        [-0.40, 1.00]]

A has complex eigenvalues at 0.75 +/- 0.24i, modulus 0.787, so the system is
stable and genuinely oscillatory rather than a smoothed version of its input.
Because both outputs are built from the same two states, a model cannot treat
the two channels as separate problems -- it has to carry shared state, which is
the property a MIMO rung is meant to test and a pair of SISO fits would miss.

    python scripts/make_mimo_dataset.py --output examples/mimo.dataset.json
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from random import Random

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from ameba.benchmarks import memoryless_floor  # noqa: E402
from ameba_signal import Dataset  # noqa: E402
from ameba_signal.serialization import dataset_to_dict  # noqa: E402

A = ((0.70, -0.30), (0.20, 0.80))
B = ((1.00, 0.30), (0.20, 1.00))
C = ((1.00, 0.50), (-0.40, 1.00))


def coupled_trajectory(controls) -> tuple[tuple[tuple[float, ...], ...], ...]:
    """Run the plant from rest, recording y(k+1) against u(k)."""
    state = [0.0, 0.0]
    responses = []
    for row in controls:
        state = [
            sum(a * x for a, x in zip(A[i], state)) + sum(b * u for b, u in zip(B[i], row))
            for i in range(2)
        ]
        responses.append(tuple(sum(c * x for c, x in zip(C[p], state)) for p in range(2)))
    return tuple(responses)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--steps", type=int, default=150)
    parser.add_argument("--amplitude", type=float, default=1.0)
    parser.add_argument("--data-seed", type=int, default=7)
    parser.add_argument("--output", default="examples/mimo.dataset.json")
    args = parser.parse_args(argv)

    rng = Random(args.data_seed)
    controls = tuple(
        (rng.uniform(-args.amplitude, args.amplitude),
         rng.uniform(-args.amplitude, args.amplitude))
        for _ in range(args.steps)
    )
    dataset = Dataset(controls, coupled_trajectory(controls))
    Path(args.output).write_text(
        json.dumps(dataset_to_dict(dataset), indent=2), encoding="utf-8"
    )

    floor = memoryless_floor(dataset)
    print(f"wrote {args.output}: {args.steps} rows, 2 inputs, 2 outputs")
    print(f"  memoryless floor: {floor:.6g}")
    print(f"  response range:   "
          + ", ".join(
              f"y{p} [{min(r[p] for r in dataset.outputs):.3g}, "
              f"{max(r[p] for r in dataset.outputs):.3g}]"
              for p in range(2)
          ))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
