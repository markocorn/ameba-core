"""Record a coupled two-input, two-output plant as an identification dataset.

The default plant is a stable second-order state-space system in which
everything is coupled: each input drives both states, each state feeds both, and
each output mixes both states.

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
    python scripts/make_mimo_dataset.py --plant tank-nonminphase

The same script also records the quadruple-tank rungs. Its defaults are
therefore per plant: each dataset was recorded at the settings its own rung
expects, so naming the plant alone regenerates the committed file exactly.
"""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from random import Random

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from ameba.benchmarks import memoryless_floor, quadruple_tank  # noqa: E402
from ameba_signal import Dataset  # noqa: E402
from ameba_signal.serialization import dataset_to_dict  # noqa: E402

A = ((0.70, -0.30), (0.20, 0.80))
B = ((1.00, 0.30), (0.20, 1.00))
C = ((1.00, 0.50), (-0.40, 1.00))


@dataclass(frozen=True, slots=True)
class PlantSpec:
    """One dataset source, with the settings that reproduce it.

    These live here rather than as argument defaults because the plants are not
    recorded at common settings. The tank needs a longer run at a smaller
    amplitude, its input held for several samples, and a sample time of its
    own, so ``--plant`` on its own has to carry all of that.
    """

    output: str
    steps: int
    amplitude: float
    hold: int = 1
    time_step: float = 1.0


PLANTS = {
    "coupled": PlantSpec("examples/mimo.dataset.json", 150, 1.0),
    "tank-minphase": PlantSpec(
        "examples/tank-minphase.dataset.json",
        300,
        0.5,
        quadruple_tank.HOLD,
        quadruple_tank.TIME_STEP,
    ),
    "tank-nonminphase": PlantSpec(
        "examples/tank-nonminphase.dataset.json",
        300,
        0.5,
        quadruple_tank.HOLD,
        quadruple_tank.TIME_STEP,
    ),
}


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


def coupled_dataset(steps: int, amplitude: float, seed: int) -> Dataset:
    """White excitation through the coupled state-space plant."""
    rng = Random(seed)
    controls = tuple(
        (rng.uniform(-amplitude, amplitude), rng.uniform(-amplitude, amplitude))
        for _ in range(steps)
    )
    return Dataset(controls, coupled_trajectory(controls))


def tank_dataset(
    parameters: quadruple_tank.TankParameters,
    steps: int,
    amplitude: float,
    seed: int,
    hold: int,
) -> Dataset:
    """The plant's own held excitation, which is what identifies a tank."""
    controls = quadruple_tank.excitation(steps, Random(seed), amplitude, hold)
    return Dataset(controls, quadruple_tank.simulate(controls, parameters))


def _dataset(args) -> Dataset:
    if args.plant == "coupled":
        return coupled_dataset(args.steps, args.amplitude, args.data_seed)
    parameters = (
        quadruple_tank.MINIMUM_PHASE
        if args.plant == "tank-minphase"
        else quadruple_tank.NON_MINIMUM_PHASE
    )
    return tank_dataset(
        parameters, args.steps, args.amplitude, args.data_seed, args.hold
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plant", choices=sorted(PLANTS), default="coupled")
    parser.add_argument("--steps", type=int, default=None, help="rows to record")
    parser.add_argument("--amplitude", type=float, default=None, help="input amplitude")
    parser.add_argument(
        "--hold", type=int, default=None, help="samples each level is held"
    )
    parser.add_argument("--data-seed", type=int, default=7)
    parser.add_argument("--output", default=None)
    args = parser.parse_args(argv)

    spec = PLANTS[args.plant]
    if args.steps is None:
        args.steps = spec.steps
    if args.amplitude is None:
        args.amplitude = spec.amplitude
    if args.hold is None:
        args.hold = spec.hold
    output = spec.output if args.output is None else args.output

    dataset = _dataset(args)
    Path(output).write_text(
        json.dumps(dataset_to_dict(dataset), indent=2), encoding="utf-8"
    )

    floor = memoryless_floor(dataset, "ise", spec.time_step)
    inputs = len(dataset.inputs[0])
    outputs = len(dataset.outputs[0])
    print(
        f"wrote {output}: {len(dataset.inputs)} rows, "
        f"{inputs} inputs, {outputs} outputs"
    )
    print(f"  memoryless floor: {floor:.6g}")
    print(f"  response range:   "
          + ", ".join(
              f"y{p} [{min(r[p] for r in dataset.outputs):.3g}, "
              f"{max(r[p] for r in dataset.outputs):.3g}]"
              for p in range(outputs)
          ))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
