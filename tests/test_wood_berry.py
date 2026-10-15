import unittest
from math import exp
from random import Random

from ameba.benchmarks import BENCHMARKS, channel_training_controls, multi_channel_dataset, multi_channel_floor
from ameba.benchmarks.wood_berry import (
    DEAD_TIMES,
    GAINS,
    OUTPUT_SCALE,
    TIME_CONSTANTS,
    reference_graph,
    wood_berry_trajectory,
)
from ameba_signal import SignalEvaluator, SignalGraphPolicy, SignalSimulator


def controls(steps=200, seed=7):
    return channel_training_controls(steps, 2, Random(seed), 1.0, 10)


class PlantTests(unittest.TestCase):
    def test_each_path_settles_at_its_published_gain(self) -> None:
        for j in range(2):
            step = [tuple(1.0 if c == j else 0.0 for c in range(2))] * 400
            final = wood_berry_trajectory(step).responses[-1]
            for i in range(2):
                with self.subTest(output=i, control=j):
                    self.assertAlmostEqual(GAINS[i][j] * OUTPUT_SCALE, final[i], places=6)

    def test_each_path_waits_out_its_dead_time(self) -> None:
        """A step at k=0 first shows in y(d+1), which is responses[d]."""
        for j in range(2):
            step = [tuple(1.0 if c == j else 0.0 for c in range(2))] * 12
            responses = wood_berry_trajectory(step).responses
            for i in range(2):
                d = DEAD_TIMES[i][j]
                with self.subTest(output=i, control=j):
                    self.assertTrue(all(row[i] == 0.0 for row in responses[:d]))
                    self.assertNotEqual(0.0, responses[d][i])

    def test_first_moved_sample_matches_the_lag(self) -> None:
        responses = wood_berry_trajectory([(1.0, 0.0)] * 3).responses
        expected = GAINS[0][0] * OUTPUT_SCALE * (1.0 - exp(-1.0 / TIME_CONSTANTS[0][0]))
        self.assertAlmostEqual(expected, responses[1][0], places=12)

    def test_steam_drives_both_outputs_down_and_reflux_both_up(self) -> None:
        reflux = wood_berry_trajectory([(1.0, 0.0)] * 100).responses[-1]
        steam = wood_berry_trajectory([(0.0, 1.0)] * 100).responses[-1]
        self.assertTrue(all(value > 0 for value in reflux))
        self.assertTrue(all(value < 0 for value in steam))

    def test_rejects_the_wrong_number_of_inputs(self) -> None:
        with self.assertRaises(ValueError):
            wood_berry_trajectory([(1.0,)])


class ReferenceGraphTests(unittest.TestCase):
    def test_reference_graph_is_a_valid_signal_graph(self) -> None:
        SignalGraphPolicy().validate(reference_graph())

    def test_reference_graph_reproduces_the_plant(self) -> None:
        rows = controls()
        trajectory = wood_berry_trajectory(rows)
        modelled = SignalSimulator().run_series(reference_graph(), rows)
        worst = max(abs(a - b) for got, want in zip(modelled, trajectory.responses) for a, b in zip(got, want))
        self.assertLess(worst, 1e-12)

    def test_reference_scores_far_below_the_memoryless_floor(self) -> None:
        trajectory = wood_berry_trajectory(controls())
        score = SignalEvaluator(multi_channel_dataset(trajectory)).evaluate(reference_graph())
        self.assertLess(score, 1e-20 * multi_channel_floor(trajectory))


class RegistrationTests(unittest.TestCase):
    def test_registered_as_a_two_channel_rung_with_a_reference(self) -> None:
        benchmark = BENCHMARKS["wood-berry"]
        self.assertEqual(2, benchmark.channels)
        self.assertIsNotNone(benchmark.reference_of)
        self.assertTrue(benchmark.linear_floor_is_strict)
        trajectory = benchmark.build(controls(50))
        self.assertEqual((2, 2), (trajectory.inputs, trajectory.outputs))


if __name__ == "__main__":
    unittest.main()
