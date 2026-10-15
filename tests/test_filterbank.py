import cmath
import math
import unittest

from ameba.benchmarks import BENCHMARKS, narendra
from ameba.benchmarks.filterbank import FilterBank, band_pass, bank_from_spec, laguerre
from ameba.benchmarks.splits import BANDS, suite


def response(biquad, frequency):
    z = cmath.exp(-2j * math.pi * frequency)
    return abs((biquad.b0 + biquad.b1 * z + biquad.b2 * z * z) / (1 + biquad.a1 * z + biquad.a2 * z * z))


class FilterBankTests(unittest.TestCase):
    def test_each_band_pass_has_unit_gain_at_its_centre_and_less_elsewhere(self) -> None:
        centres = [math.sqrt(low * high) for low, high in BANDS]
        for index, (low, high) in enumerate(BANDS):
            gains = [response(band_pass(low, high), f) for f in centres]
            self.assertAlmostEqual(1.0, gains[index], places=9)
            self.assertTrue(all(g < 0.6 for i, g in enumerate(gains) if i != index))

    def test_laguerre_filters_are_orthonormal(self) -> None:
        impulse = [1.0] + [0.0] * 500
        outputs = laguerre(impulse, 5, 0.6)
        for i in range(5):
            for j in range(5):
                product = sum(a * b for a, b in zip(outputs[i], outputs[j]))
                self.assertAlmostEqual(1.0 if i == j else 0.0, product, places=6)

    def test_laguerre_outputs_are_strictly_causal(self) -> None:
        outputs = laguerre([1.0, 0.0, 0.0], 3, 0.5)
        self.assertTrue(all(output[0] == 0.0 for output in outputs))

    def test_augment_keeps_the_raw_input_first(self) -> None:
        rows = [(0.1,), (0.2,), (-0.3,)]
        augmented = FilterBank("octaves").augment(rows)
        self.assertEqual([0.1, 0.2, -0.3], [row[0] for row in augmented])
        self.assertEqual(7, len(augmented[0]))

    def test_multi_channel_inputs_get_a_bank_each(self) -> None:
        rows = [(0.1, -0.2)] * 5
        self.assertEqual(2 + 2 * 6, len(FilterBank("laguerre").augment(rows)[0]))

    def test_the_plant_and_its_floor_are_unchanged_by_a_bank(self) -> None:
        plain = suite(BENCHMARKS["narendra"], "test", family="octaves")
        banked = suite(BENCHMARKS["narendra"], "test", family="octaves", bank=bank_from_spec("octaves"))
        for (_, a, floor_a), (_, b, floor_b) in zip(plain.members, banked.members):
            self.assertEqual(floor_a, floor_b)
            self.assertEqual(a.dataset.outputs, b.dataset.outputs)
        self.assertLess(banked.evaluate(narendra.reference_graph()), 1e-20)

    def test_normalised_bands_carry_white_noise_at_the_input_rms(self) -> None:
        import random

        rng = random.Random(4)
        noise = [rng.uniform(-1, 1) for _ in range(20000)]
        rms = lambda x: math.sqrt(sum(v * v for v in x) / len(x))
        for spec in ("bands:2:1,2,3,4,5,6:norm", "bands:4:3,4,5:norm"):
            for output in bank_from_spec(spec).filter(noise):
                self.assertAlmostEqual(1.0, rms(output[2000:]) / rms(noise), delta=0.05)

    def test_fourth_order_bands_overlap_less(self) -> None:
        low, high = BANDS[3]
        neighbour = math.sqrt(BANDS[4][0] * BANDS[4][1])
        second = response(band_pass(low, high), neighbour)
        self.assertLess(second ** 2, 0.5 * second)  # the cascade squares the response

    def test_band_subsets(self) -> None:
        bank = bank_from_spec("bands:4:4:norm")
        self.assertEqual(1, bank.width())
        self.assertEqual(2, len(bank.augment([(0.1,), (0.2,)])[0]))
        with self.assertRaises(ValueError):
            bank_from_spec("bands:3:4")
        with self.assertRaises(ValueError):
            bank_from_spec("bands:4:7")

    def test_dummy_channels_carry_nothing_about_the_input(self) -> None:
        zero = bank_from_spec("dummy:zero:2")
        self.assertEqual([[0.0] * 4, [0.0] * 4], zero.filter([1.0, -1.0, 0.5, 0.2]))
        noise = bank_from_spec("dummy:noise:1")
        self.assertEqual(noise.filter([1.0] * 50), noise.filter([-0.3] * 50))  # independent of u
        self.assertNotEqual(noise.filter([0.0] * 50)[0], [0.0] * 50)

    def test_specs(self) -> None:
        self.assertIsNone(bank_from_spec("none"))
        self.assertEqual(FilterBank("laguerre", 0.7, 4), bank_from_spec("laguerre:0.7:4"))
        with self.assertRaises(ValueError):
            bank_from_spec("wavelets")


if __name__ == "__main__":
    unittest.main()
