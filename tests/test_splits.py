import unittest
from random import Random

from ameba.benchmarks import BENCHMARKS, narendra, training_controls
from ameba.benchmarks.splits import SPLITS, suite


class SplitTests(unittest.TestCase):
    def test_the_three_splits_never_share_a_signal(self) -> None:
        signatures = {}
        for split, members in SPLITS.items():
            for _, kind, steps, seed, hold, period in members:
                signatures.setdefault((kind, steps, seed, hold, period), set()).add(split)
        self.assertTrue(all(len(splits) == 1 for splits in signatures.values()))

    def test_training_starts_with_the_old_single_trajectory(self) -> None:
        train = suite(BENCHMARKS["narendra"], "train")
        label, evaluator, _ = train.members[0]
        old = narendra.narendra_trajectory(training_controls(150, Random(7)))
        self.assertEqual("noise", label)
        self.assertEqual(old.responses, tuple(row[0] for row in evaluator.dataset.outputs))

    def test_the_true_model_scores_near_zero_on_every_split(self) -> None:
        reference = narendra.reference_graph()
        for split in SPLITS:
            score = suite(BENCHMARKS["narendra"], split).evaluate(reference)
            self.assertLess(score, 1e-20, split)

    def test_a_static_gain_sits_near_one(self) -> None:
        """y = 0.75 u is about the memoryless floor everywhere, by construction."""
        from ameba_genome import Grammar, decode
        from ameba_genome.dictionary import input_gene, output_gene, weight_gene

        graph = decode((output_gene(0), input_gene(0), weight_gene(0.75)), Grammar())
        for split in SPLITS:
            score = suite(BENCHMARKS["narendra"], split).evaluate(graph)
            self.assertTrue(0.9 < score < 1.6, (split, score))

    def test_test_suite_reports_every_signal(self) -> None:
        test = suite(BENCHMARKS["narendra"], "test")
        ratios = test.ratios(narendra.reference_graph())
        self.assertEqual({"noise-11", "noise-23", "sine", "steps"}, set(ratios))

    def test_multi_channel_rungs_get_splits_too(self) -> None:
        reference = BENCHMARKS["wood-berry"].reference_of()
        for split in SPLITS:
            self.assertLess(suite(BENCHMARKS["wood-berry"], split).evaluate(reference), 1e-20)


class SpectralTests(unittest.TestCase):
    def test_a_multisine_stays_inside_its_band(self) -> None:
        import cmath
        import math

        from ameba.benchmarks import multisine_controls
        from ameba.benchmarks.splits import BANDS

        for low, high in BANDS:
            u = multisine_controls(600, low, high, Random(3))
            n = len(u)
            spectrum = [abs(sum(u[t] * cmath.exp(-2j * math.pi * k * t / n) for t in range(n))) ** 2
                        for k in range(n // 2 + 1)]
            inside = sum(p for k, p in enumerate(spectrum) if low <= k / n <= high)
            self.assertGreater(inside / sum(spectrum), 0.999, (low, high))
            self.assertAlmostEqual(1.0, max(abs(x) for x in u), places=12)

    def test_spectral_splits_never_share_a_signal(self) -> None:
        from ameba.benchmarks.splits import SPECTRAL

        seen = {}
        for split, members in SPECTRAL.items():
            for _, kind, steps, seed, _, band in members:
                seen.setdefault((kind, steps, seed, band), set()).add(split)
        self.assertTrue(all(len(splits) == 1 for splits in seen.values()))

    def test_the_true_model_is_exact_on_every_spectral_split(self) -> None:
        reference = narendra.reference_graph()
        for split in SPLITS:
            score = suite(BENCHMARKS["narendra"], split, family="spectral").evaluate(reference)
            self.assertLess(score, 1e-20, split)

    def test_octave_training_covers_every_test_band_equally(self) -> None:
        from ameba.benchmarks.splits import OCTAVES, SPECTRAL

        for split in ("train", "validation"):
            labels = [member[0] for member in OCTAVES[split]]
            self.assertEqual(["noise"] + [f"band-{i}" for i in range(1, 7)], labels)
        self.assertEqual(SPECTRAL["test"], OCTAVES["test"])
        seeds = [m[3] for split in OCTAVES.values() for m in split]
        self.assertEqual(len(seeds), len(set(seeds)))

    def test_spectral_test_suite_has_six_octaves(self) -> None:
        test = suite(BENCHMARKS["narendra"], "test", family="spectral")
        self.assertEqual([f"band-{i}" for i in range(1, 7)], [label for label, _, _ in test.members])


if __name__ == "__main__":
    unittest.main()
