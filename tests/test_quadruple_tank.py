"""Johansson's quadruple-tank process, checked against its own physics.

There is no reference implementation to compare against, so the plant is
verified from properties it must satisfy: it rests where the equilibrium
solver says it does, mass is conserved, integration has converged, and the
valve setting produces the inverse response the paper's zero predicts.
"""

import unittest
from math import sqrt
from random import Random

from ameba.benchmarks import memoryless_floor
from ameba.benchmarks.quadruple_tank import (
    GRAVITY,
    HOLD,
    MINIMUM_PHASE,
    NON_MINIMUM_PHASE,
    TIME_STEP,
    TRAJECTORIES,
    TankParameters,
    derivatives,
    equilibrium,
    excitation,
    identification_dataset,
    minimum_phase_trajectory,
    non_minimum_phase_trajectory,
    simulate,
)


class PlantPhysicsTests(unittest.TestCase):
    def test_the_solved_equilibrium_is_a_fixed_point(self) -> None:
        """Every rate of change is zero there, to floating-point noise."""
        for name, parameters in (("P-", MINIMUM_PHASE), ("P+", NON_MINIMUM_PHASE)):
            with self.subTest(operating_point=name):
                rates = derivatives(
                    equilibrium(parameters), parameters.voltage, parameters
                )
                for index, rate in enumerate(rates):
                    self.assertAlmostEqual(rate, 0.0, places=10, msg=f"tank {index + 1}")

    def test_the_plant_stays_at_rest_without_an_input_deviation(self) -> None:
        responses = simulate([(0.0, 0.0)] * 50, MINIMUM_PHASE)
        for y1, y2 in responses:
            self.assertAlmostEqual(y1, 0.0, places=9)
            self.assertAlmostEqual(y2, 0.0, places=9)

    def test_the_solved_levels_are_near_the_published_operating_point(self) -> None:
        """Agree with the paper's table, tightly on what the plant measures.

        The table is not perfectly self-consistent at printed precision, and the
        upper tanks are where that shows: their level goes as the square of the
        inflow, so rounding in a pump gain or a valve split is amplified. At P-
        the printed h3 of 1.8 cm implies 4.7% more flow than the printed g2 and
        k2 deliver, which lands as 9% in level.

        The measured levels -- tanks 1 and 2, the plant's actual outputs -- are
        insensitive to that and are held to 2%. The solved equilibrium is in any
        case self-consistent with the stated parameters, which is what the
        benchmark needs; that is what the fixed-point test above establishes.
        """
        published = {
            MINIMUM_PHASE: (12.4, 12.7, 1.8, 1.4),
            NON_MINIMUM_PHASE: (12.6, 13.0, 4.8, 4.9),
        }
        for parameters, expected in published.items():
            solved = equilibrium(parameters)
            for index, (value, paper) in enumerate(zip(solved, expected), 1):
                tolerance = 0.02 if index <= 2 else 0.10
                with self.subTest(minimum_phase=parameters.minimum_phase, tank=index):
                    self.assertLess(abs(value - paper) / paper, tolerance)

    def test_integration_has_converged_at_the_default_substep_count(self) -> None:
        controls = excitation(40, Random(1))
        coarse = simulate(controls, MINIMUM_PHASE, substeps=10)
        fine = simulate(controls, MINIMUM_PHASE, substeps=200)
        worst = max(
            abs(a - b) for rows in zip(coarse, fine) for a, b in zip(*rows)
        )
        self.assertLess(worst, 1e-6, "default integration is too coarse")

    def test_outflow_matches_torricelli_at_the_equilibrium(self) -> None:
        """Tank 3 drains exactly what pump 2 routes over the top into it."""
        parameters = MINIMUM_PHASE
        levels = equilibrium(parameters)
        outflow = parameters.outlet[2] * sqrt(2.0 * GRAVITY * levels[2])
        inflow = (1.0 - parameters.split[1]) * parameters.gain[1] * parameters.voltage[1]
        self.assertAlmostEqual(outflow, inflow, places=10)


class TransmissionZeroTests(unittest.TestCase):
    """The valve setting decides the sign of the zero, and so the response."""

    def test_the_configurations_are_classified_by_their_valve_split(self) -> None:
        self.assertTrue(MINIMUM_PHASE.minimum_phase)
        self.assertFalse(NON_MINIMUM_PHASE.minimum_phase)
        self.assertGreater(sum(MINIMUM_PHASE.split), 1.0)
        self.assertLess(sum(NON_MINIMUM_PHASE.split), 1.0)

    def test_minimum_phase_answers_a_step_in_one_direction_only(self) -> None:
        responses = simulate([(1.0, 0.0)] * 120, MINIMUM_PHASE)
        first = [row[0] for row in responses]
        self.assertGreater(first[-1], 0.0, "pump 1 must raise level 1")
        self.assertGreaterEqual(min(first), -1e-9, "no undershoot is expected")

    def test_non_minimum_phase_sends_the_far_output_the_wrong_way_first(self) -> None:
        """The signature of a right-half-plane zero: an inverse response.

        With most flow routed over the top, pump 1 reaches level 2 only through
        tank 4, while pump 2's own direct route has been throttled. Driving
        pump 1 alone therefore moves level 2 before tank 4 has filled, and the
        cross-channel response starts in the direction it does not end in.
        """
        responses = simulate([(1.0, -1.0)] * 200, NON_MINIMUM_PHASE)
        second = [row[1] for row in responses]
        settled = second[-1]
        early = min(second) if settled > 0 else max(second)
        self.assertNotAlmostEqual(settled, 0.0, places=3)
        self.assertLess(
            early * settled, 0.0,
            "expected the response to start opposite to where it settles",
        )

    def test_the_same_step_does_not_invert_in_minimum_phase(self) -> None:
        responses = simulate([(1.0, -1.0)] * 200, MINIMUM_PHASE)
        second = [row[1] for row in responses]
        settled = second[-1]
        early = min(second) if settled > 0 else max(second)
        self.assertGreaterEqual(early * settled, -1e-12)


class IdentificationDatasetTests(unittest.TestCase):
    def test_the_dataset_is_bounded_and_correctly_shaped(self) -> None:
        data = identification_dataset(120)
        self.assertEqual(len(data.inputs), 120)
        self.assertEqual(len(data.inputs[0]), 2)
        self.assertEqual(len(data.outputs[0]), 2)
        self.assertTrue(all(abs(v) < 50.0 for row in data.outputs for v in row))

    def test_a_held_excitation_actually_moves_the_plant(self) -> None:
        """A slow plant under white noise would barely respond at all."""
        held = identification_dataset(200, hold=10)
        noisy = identification_dataset(200, hold=1)
        spread = lambda data: max(  # noqa: E731
            abs(v) for row in data.outputs for v in row
        )
        self.assertGreater(spread(held), 3.0 * spread(noisy))

    def test_the_memoryless_floor_leaves_room_to_identify(self) -> None:
        """A static map must not already explain the data, or nothing is learned."""
        for parameters in (MINIMUM_PHASE, NON_MINIMUM_PHASE):
            with self.subTest(minimum_phase=parameters.minimum_phase):
                data = identification_dataset(200, parameters)
                energy = sum(v * v for row in data.outputs for v in row)
                self.assertGreater(memoryless_floor(data), 0.2 * energy)

    def test_configuration_is_validated(self) -> None:
        with self.assertRaises(ValueError):
            TankParameters((28.0, 32.0, 28.0, 32.0), (0.071, 0.057, 0.071, 0.057),
                           (3.33, 3.35), (1.4, 0.6), (3.0, 3.0))
        with self.assertRaises(ValueError):
            TankParameters((28.0, 32.0, 28.0, -1.0), (0.071, 0.057, 0.071, 0.057),
                           (3.33, 3.35), (0.7, 0.6), (3.0, 3.0))


class RegisteredTrajectoryTests(unittest.TestCase):
    """The benchmark-facing builders, which the report scripts select by name."""

    def test_both_variants_are_registered(self) -> None:
        self.assertEqual(
            sorted(TRAJECTORIES), ["tank-minphase", "tank-nonminphase"]
        )

    def test_a_trajectory_carries_the_same_response_as_the_plant(self) -> None:
        controls = excitation(40, Random(3))
        self.assertEqual(
            minimum_phase_trajectory(controls).responses,
            simulate(controls, MINIMUM_PHASE, TIME_STEP),
        )
        self.assertEqual(
            non_minimum_phase_trajectory(controls).responses,
            simulate(controls, NON_MINIMUM_PHASE, TIME_STEP),
        )

    def test_the_two_variants_are_not_the_same_plant(self) -> None:
        controls = excitation(40, Random(3))
        self.assertNotEqual(
            minimum_phase_trajectory(controls).responses,
            non_minimum_phase_trajectory(controls).responses,
        )

    def test_the_default_dataset_is_the_one_committed_to_examples(self) -> None:
        """The defaults are the settings ``examples/tank-*.dataset.json`` holds.

        Regenerating those files is only meaningful while these agree, so the
        hold, the amplitude, and the sample time are all pinned through them.
        """
        data = identification_dataset()
        expected = excitation(300, Random(7), 0.5, HOLD)
        self.assertEqual(len(data.inputs), 300)
        self.assertEqual(data.inputs, expected)
        # One step is independent of later ones, so replaying the first input
        # from rest must reproduce the first recorded response exactly.
        self.assertEqual(data.outputs[0], simulate(data.inputs[:1], MINIMUM_PHASE)[0])
        self.assertEqual((TIME_STEP, HOLD), (3.0, 10))


if __name__ == "__main__":
    unittest.main()
