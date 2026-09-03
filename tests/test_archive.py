import unittest
from math import inf, nan

from ameba_graph import (
    Archive,
    ArchiveConfig,
    Graph,
    Node,
    correlation_distance,
    rank_transform,
)


def graph_of(size: int) -> Graph:
    return Graph(nodes=[Node(f"n{index}", "red") for index in range(size)])


class RankTransformTests(unittest.TestCase):
    def test_ranks_are_one_based_positions(self) -> None:
        self.assertEqual(rank_transform([5.0, 1.0, 3.0]), (3.0, 1.0, 2.0))

    def test_ties_share_their_average_rank(self) -> None:
        self.assertEqual(rank_transform([2.0, 2.0, 9.0]), (1.5, 1.5, 3.0))

    def test_a_constant_sequence_gives_every_value_the_same_rank(self) -> None:
        self.assertEqual(rank_transform([4.0] * 4), (2.5,) * 4)

    def test_nan_is_rejected_rather_than_ordered_arbitrarily(self) -> None:
        with self.assertRaises(ValueError):
            rank_transform([1.0, nan])


class CorrelationDistanceTests(unittest.TestCase):
    def test_identical_vectors_are_at_zero_distance(self) -> None:
        self.assertAlmostEqual(correlation_distance([1.0, 2.0, 3.0], [1.0, 2.0, 3.0]), 0.0)

    def test_a_monotone_rescaling_is_the_same_idea(self) -> None:
        """The whole reason for ranking: gain differences must not read as new."""
        left = rank_transform([0.5, 2.0, 8.0])
        right = rank_transform([5.0, 20.0, 80.0])
        self.assertAlmostEqual(correlation_distance(left, right), 0.0)

    def test_reversed_behaviour_is_maximally_distant(self) -> None:
        self.assertAlmostEqual(correlation_distance([1.0, 2.0, 3.0], [3.0, 2.0, 1.0]), 2.0)

    def test_one_huge_residual_cannot_dominate_a_rank_comparison(self) -> None:
        modest = rank_transform([1.0, 2.0, 3.0, 4.0])
        spiked = rank_transform([1.0, 2.0, 3.0, 1e9])
        self.assertAlmostEqual(correlation_distance(modest, spiked), 0.0)

    def test_a_constant_descriptor_carries_no_similarity(self) -> None:
        self.assertEqual(correlation_distance([1.0, 1.0, 1.0], [1.0, 2.0, 3.0]), 1.0)


class ArchiveTests(unittest.TestCase):
    def setUp(self) -> None:
        self.config = ArchiveConfig(capacity=3, probation=0, threshold=0.5)

    def test_it_fills_to_capacity_before_competing(self) -> None:
        """Everything correlates early, so a cold archive must admit freely."""
        archive = Archive(self.config)
        for index in range(3):
            outcome = archive.insert(graph_of(index + 1), 10.0 + index, (1.0, 2.0, 3.0))
            self.assertEqual(outcome.reason, "filled")
        self.assertEqual(len(archive), 3)

    def test_a_retuned_variant_replaces_its_own_parent(self) -> None:
        """Local search: near in behaviour means it competes only with itself."""
        archive = Archive(self.config)
        for score in (10.0, 20.0, 30.0):
            archive.insert(graph_of(1), score, (1.0, 2.0, 3.0, 4.0))
        outcome = archive.insert(graph_of(9), 5.0, (1.0, 2.0, 3.0, 4.0))
        self.assertTrue(outcome.accepted)
        self.assertEqual(outcome.reason, "improved")
        self.assertEqual(len(archive), 3)
        self.assertEqual(archive.best.score, 5.0)
        self.assertEqual(archive.best.improvements, 1)

    def test_a_worse_variant_of_the_same_idea_is_dominated(self) -> None:
        archive = Archive(self.config)
        for score in (10.0, 20.0, 30.0):
            archive.insert(graph_of(1), score, (1.0, 2.0, 3.0, 4.0))
        outcome = archive.insert(graph_of(9), 25.0, (1.0, 2.0, 3.0, 4.0))
        self.assertFalse(outcome.accepted)
        self.assertEqual(outcome.reason, "dominated")

    def test_a_novel_candidate_enters_without_beating_the_incumbents(self) -> None:
        """The door: an untuned new structure scores badly and still gets in."""
        archive = Archive(self.config)
        for score in (1.0, 2.0, 3.0):
            archive.insert(graph_of(1), score, (1.0, 2.0, 3.0, 4.0))
        outcome = archive.insert(graph_of(7), 900.0, (4.0, 3.0, 2.0, 1.0))
        self.assertTrue(outcome.accepted)
        self.assertEqual(outcome.reason, "novel")
        self.assertEqual(outcome.displaced.score, 3.0)
        self.assertEqual(len(archive), 3)

    def test_requiring_improvement_closes_that_door(self) -> None:
        archive = Archive(ArchiveConfig(capacity=3, probation=0, threshold=0.5,
                                        novelty_admits=False))
        for score in (1.0, 2.0, 3.0):
            archive.insert(graph_of(1), score, (1.0, 2.0, 3.0, 4.0))
        outcome = archive.insert(graph_of(7), 900.0, (4.0, 3.0, 2.0, 1.0))
        self.assertFalse(outcome.accepted)
        self.assertEqual(outcome.reason, "worse than worst")

    def test_the_best_member_is_never_evicted(self) -> None:
        archive = Archive(self.config)
        for score in (1.0, 2.0, 3.0):
            archive.insert(graph_of(1), score, (1.0, 2.0, 3.0, 4.0))
        for _ in range(20):
            archive.insert(graph_of(7), 500.0, (4.0, 3.0, 2.0, 1.0))
        self.assertEqual(archive.best.score, 1.0)

    def test_probation_protects_a_newcomer_from_immediate_eviction(self) -> None:
        """A structure admitted untuned must outlive the next arrival."""
        archive = Archive(ArchiveConfig(capacity=5, probation=2, threshold=0.5))
        for score in (1.0, 2.0, 3.0, 4.0, 5.0):
            archive.insert(graph_of(1), score, (1.0, 2.0, 3.0, 4.0))
        admitted = archive.insert(graph_of(7), 900.0, (4.0, 3.0, 2.0, 1.0))
        self.assertEqual(admitted.reason, "novel")
        following = archive.insert(graph_of(8), 800.0, (2.0, 4.0, 1.0, 3.0))
        self.assertEqual(following.reason, "novel")
        self.assertIn(900.0, [member.score for member in archive.members])

    def test_probation_cannot_be_set_high_enough_to_shut_the_door(self) -> None:
        with self.assertRaises(ValueError):
            ArchiveConfig(capacity=3, probation=5)

    def test_a_smaller_graph_wins_a_niche_at_equal_score(self) -> None:
        """Parsimony scoped to a niche, so it cannot pull toward the plateau."""
        archive = Archive(ArchiveConfig(capacity=3, probation=0, threshold=0.5,
                                        size_tiebreak=0.05))
        for score in (10.0, 20.0, 30.0):
            archive.insert(graph_of(8), score, (1.0, 2.0, 3.0, 4.0))
        outcome = archive.insert(graph_of(3), 10.2, (1.0, 2.0, 3.0, 4.0))
        self.assertEqual(outcome.reason, "simplified")
        self.assertEqual(len(archive.best.graph.nodes), 3)

    def test_a_smaller_graph_that_scores_much_worse_is_still_rejected(self) -> None:
        archive = Archive(ArchiveConfig(capacity=3, probation=0, threshold=0.5,
                                        size_tiebreak=0.05))
        for score in (10.0, 20.0, 30.0):
            archive.insert(graph_of(8), score, (1.0, 2.0, 3.0, 4.0))
        outcome = archive.insert(graph_of(3), 40.0, (1.0, 2.0, 3.0, 4.0))
        self.assertFalse(outcome.accepted)

    def test_size_tiebreak_is_off_unless_asked_for(self) -> None:
        archive = Archive(self.config)
        for score in (10.0, 20.0, 30.0):
            archive.insert(graph_of(8), score, (1.0, 2.0, 3.0, 4.0))
        self.assertFalse(
            archive.insert(graph_of(3), 10.2, (1.0, 2.0, 3.0, 4.0)).accepted
        )

    def test_disabling_local_competition_stops_in_place_improvement(self) -> None:
        """The ablation: members can no longer be tuned by their own variants."""
        archive = Archive(ArchiveConfig(capacity=3, probation=0, threshold=0.5,
                                        local_competition=False))
        for score in (10.0, 20.0, 30.0):
            archive.insert(graph_of(1), score, (1.0, 2.0, 3.0, 4.0))
        outcome = archive.insert(graph_of(9), 0.5, (1.0, 2.0, 3.0, 4.0))
        self.assertFalse(outcome.accepted)
        self.assertEqual(outcome.reason, "no local competition")
        self.assertEqual(archive.best.score, 10.0)

    def test_disabling_local_competition_still_admits_novelty(self) -> None:
        archive = Archive(ArchiveConfig(capacity=3, probation=0, threshold=0.5,
                                        local_competition=False))
        for score in (10.0, 20.0, 30.0):
            archive.insert(graph_of(1), score, (1.0, 2.0, 3.0, 4.0))
        outcome = archive.insert(graph_of(9), 0.5, (4.0, 3.0, 2.0, 1.0))
        self.assertEqual(outcome.reason, "novel")
        self.assertEqual(archive.best.score, 0.5)

    def test_an_unscorable_candidate_is_rejected_outright(self) -> None:
        archive = Archive(self.config)
        self.assertFalse(archive.insert(graph_of(1), inf, (1.0, 2.0)).accepted)
        self.assertFalse(archive.insert(graph_of(1), 1.0, None).accepted)
        self.assertEqual(len(archive), 0)

    def test_the_adaptive_threshold_follows_the_members_it_holds(self) -> None:
        archive = Archive(ArchiveConfig(capacity=3, probation=0))
        self.assertEqual(archive.threshold(), 0.0)
        archive.insert(graph_of(1), 1.0, (1.0, 2.0, 3.0))
        self.assertEqual(archive.threshold(), 0.0)
        archive.insert(graph_of(1), 2.0, (3.0, 2.0, 1.0))
        self.assertAlmostEqual(archive.threshold(), 2.0)

    def test_the_adaptive_threshold_is_held_inside_its_bounds(self) -> None:
        """Unbounded, it drifts to extremes that stall a run either way."""
        bounds = dict(probation=0, threshold_min=0.2, threshold_max=0.8)
        collapsed = Archive(ArchiveConfig(capacity=2, **bounds))
        collapsed.insert(graph_of(1), 1.0, (1.0, 2.0, 3.0))
        collapsed.insert(graph_of(1), 2.0, (1.0, 2.0, 3.0))
        self.assertEqual(collapsed.threshold(), 0.2)

        spread = Archive(ArchiveConfig(capacity=2, **bounds))
        spread.insert(graph_of(1), 1.0, (1.0, 2.0, 3.0))
        spread.insert(graph_of(1), 2.0, (3.0, 2.0, 1.0))
        self.assertEqual(spread.threshold(), 0.8)

    def test_a_candidate_below_the_size_floor_is_refused(self) -> None:
        """Runs that shrink stall; the guard is a lower bound, not a charge."""
        archive = Archive(ArchiveConfig(capacity=3, probation=0, min_nodes=5))
        outcome = archive.insert(graph_of(2), 0.001, (1.0, 2.0, 3.0))
        self.assertFalse(outcome.accepted)
        self.assertEqual(outcome.reason, "too small")
        self.assertEqual(len(archive), 0)
        self.assertTrue(archive.insert(graph_of(5), 9.0, (1.0, 2.0, 3.0)).accepted)

    def test_the_size_floor_is_off_by_default(self) -> None:
        archive = Archive(ArchiveConfig(capacity=3, probation=0))
        self.assertTrue(archive.insert(graph_of(1), 1.0, (1.0, 2.0, 3.0)).accepted)

    def test_threshold_bounds_are_validated(self) -> None:
        with self.assertRaises(ValueError):
            ArchiveConfig(threshold_min=0.9, threshold_max=0.4)
        with self.assertRaises(ValueError):
            ArchiveConfig(threshold_max=3.0)

    def test_insertion_order_alone_determines_the_result(self) -> None:
        """A batch may be scored in parallel as long as it is inserted in order."""
        batch = [
            (graph_of(index + 1), float(20 - index), (float(index), 2.0, 5.0, 1.0))
            for index in range(12)
        ]
        outcomes = []
        for _ in range(2):
            archive = Archive(ArchiveConfig(capacity=4, probation=2))
            outcomes.append([
                archive.insert(graph, score, descriptor).reason
                for graph, score, descriptor in batch
            ])
        self.assertEqual(outcomes[0], outcomes[1])


if __name__ == "__main__":
    unittest.main()
