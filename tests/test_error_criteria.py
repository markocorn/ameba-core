import unittest

from ameba_graph import Edge, Graph, Node
from ameba_signal import CRITERIA, Dataset, SignalEvaluator


def constant_model(value: float) -> Graph:
    """A model that ignores its input and emits ``value`` every step."""
    return Graph(
        nodes=[
            Node("u", "input", {"index": 0}),
            Node("c", "constant", {"value": value}),
            Node("y", "output", {"index": 0}),
        ],
        edges=[Edge("e1", "c", "y")],
    )


class ErrorCriterionTests(unittest.TestCase):
    def setUp(self) -> None:
        # Four steps, and a model that is wrong by exactly 1.0 at every step.
        self.dataset = Dataset(
            inputs=((0.0,), (0.0,), (0.0,), (0.0,)),
            outputs=((1.0,), (1.0,), (1.0,), (1.0,)),
        )
        self.model = constant_model(0.0)

    def test_ise_is_the_plain_sum_of_squared_error(self) -> None:
        self.assertEqual(4.0, SignalEvaluator(self.dataset).evaluate(self.model))

    def test_ise_is_the_default_criterion(self) -> None:
        default = SignalEvaluator(self.dataset)
        self.assertEqual("ise", default.criterion)
        self.assertEqual(
            default.evaluate(self.model),
            SignalEvaluator(self.dataset, criterion="ise").evaluate(self.model),
        )

    def test_iae_sums_absolute_error(self) -> None:
        score = SignalEvaluator(self.dataset, criterion="iae").evaluate(self.model)
        self.assertEqual(4.0, score)

    def test_time_weighted_criteria_discount_the_first_sample(self) -> None:
        # Weights are t = 0, 1, 2, 3, so a unit error every step sums to 6.
        for criterion in ("itse", "itae"):
            score = SignalEvaluator(self.dataset, criterion=criterion).evaluate(self.model)
            self.assertEqual(6.0, score, msg=criterion)

    def test_squared_criteria_punish_one_large_error_more_than_many_small_ones(self) -> None:
        """The defining difference between ISE and IAE."""
        spread = Dataset(inputs=((0.0,),) * 4, outputs=((1.0,), (1.0,), (1.0,), (1.0,)))
        concentrated = Dataset(inputs=((0.0,),) * 4, outputs=((4.0,), (0.0,), (0.0,), (0.0,)))
        model = constant_model(0.0)

        # Same total absolute error, so IAE cannot tell them apart.
        self.assertEqual(
            SignalEvaluator(spread, criterion="iae").evaluate(model),
            SignalEvaluator(concentrated, criterion="iae").evaluate(model),
        )
        # ISE does: 4 * 1^2 = 4 against 4^2 = 16.
        self.assertLess(
            SignalEvaluator(spread, criterion="ise").evaluate(model),
            SignalEvaluator(concentrated, criterion="ise").evaluate(model),
        )

    def test_time_step_scales_the_integral(self) -> None:
        """ISE integrates e^2 dt, so halving the sample time halves the area."""
        unit = SignalEvaluator(self.dataset, time_step=1.0).evaluate(self.model)
        half = SignalEvaluator(self.dataset, time_step=0.5).evaluate(self.model)
        self.assertAlmostEqual(unit / 2.0, half, places=12)

    def test_an_invalid_candidate_scores_infinity_under_every_criterion(self) -> None:
        broken = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("inverse", "reciprocal"),
                Node("y", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "inverse"), Edge("e2", "inverse", "y")],
        )
        for criterion in sorted(CRITERIA):
            evaluator = SignalEvaluator(self.dataset, criterion=criterion)
            self.assertEqual(float("inf"), evaluator.evaluate(broken), msg=criterion)

    def test_an_unknown_criterion_is_rejected(self) -> None:
        with self.assertRaisesRegex(ValueError, "Unknown error criterion"):
            SignalEvaluator(self.dataset, criterion="nonsense")

    def test_a_non_positive_time_step_is_rejected(self) -> None:
        for value in (0.0, -1.0, float("inf")):
            with self.assertRaisesRegex(ValueError, "time_step must be positive"):
                SignalEvaluator(self.dataset, time_step=value)


if __name__ == "__main__":
    unittest.main()
