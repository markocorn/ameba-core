"""Timing and information-boundary checks for the ideal-state diagnostic."""

import unittest
from random import Random
from types import SimpleNamespace

from scripts.oracle_probe import OracleStates, input_provider, positive_control, readout_reference, run_arm
from ameba.benchmarks import BENCHMARKS, narendra, training_controls
from ameba.benchmarks.splits import suite


class OracleProbeTests(unittest.TestCase):
    def test_first_rows_have_pre_output_state_not_target(self):
        controls = (0.4, -0.3, 0.8, 0.1)
        y = narendra.narendra_trajectory(controls).responses
        rows = OracleStates().augment([(u,) for u in controls])
        self.assertEqual(rows[0], (controls[0], 0.0, 0.0, 0.0, 0.0))
        self.assertEqual(rows[1], (controls[1], y[0], 0.0, 0.0, controls[0]))
        self.assertEqual(rows[3], (controls[3], y[2], y[1], y[0], controls[2]))

    def test_future_and_current_input_do_not_change_current_state(self):
        rng = Random(11)
        rows = [(rng.uniform(-1, 1),) for _ in range(20)]
        reference = OracleStates().augment(rows)
        for k in range(len(rows)):
            altered = rows[:k] + [(-0.9,)] * (len(rows) - k)
            self.assertEqual(OracleStates().augment(altered)[k][1:], reference[k][1:])
            self.assertEqual(OracleStates().augment(rows[:k + 1]), reference[:k + 1])

    def test_extra_delay_shifts_only_state_channels(self):
        rows = [(u,) for u in training_controls(20, Random(9))]
        current = OracleStates().augment(rows)
        for delay in (1, 3, 30):
            shifted = OracleStates(delay).augment(rows)
            self.assertEqual([row[0] for row in shifted], [row[0] for row in rows])
            for k, row in enumerate(shifted):
                self.assertEqual(row[1:], current[k - delay][1:] if k >= delay else (0.0,) * 4)

    def test_every_trajectory_starts_from_rest(self):
        oracle = OracleStates()
        oracle.augment([(0.9,)] * 100)
        self.assertEqual(oracle.augment([(0.2,)])[0], (0.2, 0.0, 0.0, 0.0, 0.0))
        self.assertEqual(oracle.augment([]), ())

    def test_no_stateful_nodes_in_positive_control(self):
        graph = readout_reference()
        self.assertEqual(sum(n.kind == "input" for n in graph.nodes.values()), 5)
        self.assertFalse(any(n.kind == "delay" for n in graph.nodes.values()))

    def test_exact_readout_on_every_split_and_family(self):
        for family in ("diverse", "spectral", "octaves"):
            with self.subTest(family=family):
                scores = positive_control(family)
                self.assertEqual(set(scores), {"train", "validation", "test"})

    def test_all_arms_keep_targets_and_raw_input_baseline(self):
        base = suite(BENCHMARKS["narendra"], "train", family="octaves")
        for arm in ("zeros", "bands", "oracle", "oracle-delay"):
            augmented = suite(BENCHMARKS["narendra"], "train", family="octaves",
                              bank=input_provider(arm, 1))
            for (name, evaluator, floor), (other_name, other, other_floor) in zip(base.members, augmented.members):
                self.assertEqual((name, floor), (other_name, other_floor))
                self.assertEqual(evaluator.dataset.outputs, other.dataset.outputs)
                self.assertEqual([r[0] for r in evaluator.dataset.inputs], [r[0] for r in other.dataset.inputs])
                self.assertTrue(all(len(row) == 5 for row in other.dataset.inputs))

    def test_rejects_wrong_interface_and_negative_delay(self):
        with self.assertRaises(ValueError):
            OracleStates(-1)
        with self.assertRaises(ValueError):
            OracleStates().augment([(0.2, 0.3)])

    def test_search_selects_and_scores_an_oracle_model(self):
        args = SimpleNamespace(delay=1, data="diverse", population=6, generations=1, progress=0)
        result = run_arm("oracle", 7, args)
        self.assertTrue(result["diagnostic_only"])
        self.assertEqual(len(result["inputs"]), 5)
        self.assertEqual(set(result["test"]), {"noise-11", "noise-23", "sine", "steps"})
        self.assertEqual(len(result["train_best_history"]), 2)
        self.assertGreater(result["simulations"], 0)


if __name__ == "__main__":
    unittest.main()
