"""A fixed-size archive of models that fail differently from one another.

A population ranked by score alone converges: once one shape is ahead, every
slot fills with variants of it, and a structurally new candidate is judged
against tuned incumbents while its own parameters are still whatever the
mutation happened to produce. It loses, and the shape is never seen again.

This archive replaces that ranking with local competition. Each member owns a
neighbourhood, described by a domain-supplied descriptor vector. A candidate is
compared only against its nearest member: close to it, they are the same idea
and the better one keeps the slot; far from it, the candidate is a new idea and
takes a slot from the worst member instead. Parameter mutation therefore stops
being a separate refinement phase -- a tuned variant of a member is near that
member, so it competes only with its own parent and replaces it on improvement.
Local search is what the archive does to itself over time.

The layer stays domain-neutral: it never inspects a graph. Whoever supplies the
descriptor decides what "different" means.
"""

from __future__ import annotations

from dataclasses import dataclass
from math import isfinite, sqrt
from typing import Mapping, Sequence

from .model import Graph


def rank_transform(values: Sequence[float]) -> tuple[float, ...]:
    """Replace values by their average ranks, so scale stops mattering.

    Correlating ranks rather than the values themselves is what makes two
    models that differ only by a gain read as the same idea, and it stops one
    enormous residual from deciding the whole comparison.
    """
    if any(value != value for value in values):
        raise ValueError("Cannot rank a sequence containing NaN")
    order = sorted(range(len(values)), key=lambda index: values[index])
    ranks = [0.0] * len(values)
    start = 0
    while start < len(order):
        stop = start
        while (
            stop + 1 < len(order)
            and values[order[stop + 1]] == values[order[start]]
        ):
            stop += 1
        average = (start + stop) / 2.0 + 1.0
        for position in range(start, stop + 1):
            ranks[order[position]] = average
        start = stop + 1
    return tuple(ranks)


def correlation_distance(left: Sequence[float], right: Sequence[float]) -> float:
    """``1 - r`` over two equal-length vectors, so the range is 0 (same) to 2.

    Anti-correlation is kept as maximal distance rather than folded onto zero:
    two models whose errors move in opposite directions are the most
    complementary pair available, not a duplicate.
    """
    if len(left) != len(right):
        raise ValueError("Descriptors must have equal length")
    if not left:
        raise ValueError("Descriptors cannot be empty")
    mean_left = sum(left) / len(left)
    mean_right = sum(right) / len(right)
    covariance = sum(
        (a - mean_left) * (b - mean_right) for a, b in zip(left, right)
    )
    spread = sqrt(
        sum((a - mean_left) ** 2 for a in left)
        * sum((b - mean_right) ** 2 for b in right)
    )
    if spread == 0.0:
        # A constant vector has no shape to compare; treat it as carrying no
        # evidence of similarity rather than as a perfect match.
        return 1.0
    return 1.0 - covariance / spread


def composition(graph: Graph, kinds: Sequence[str]) -> tuple[float, ...]:
    """How much of each kind a graph is made of, plus how wired it is.

    Deliberately counts rather than proportions. A rank or proportion profile
    would call a four-node graph and a forty-node graph of the same mixture
    identical, and size is the strongest single predictor of success measured
    on these plants -- so the descriptor has to be able to see it.
    """
    counts = dict.fromkeys(kinds, 0.0)
    for node in graph.nodes.values():
        if node.kind in counts:
            counts[node.kind] += 1.0
    return tuple(counts[kind] for kind in kinds) + (float(len(graph.edges)),)


def composition_distance(left: Sequence[float], right: Sequence[float]) -> float:
    """Bray-Curtis dissimilarity: 0 for identical make-up, 1 for disjoint.

    Note the range is half that of ``correlation_distance``, which reaches 2
    on perfect anti-correlation. Both are 0 when identical, which is what the
    same-idea test actually turns on.
    """
    if len(left) != len(right):
        raise ValueError("Descriptors must have equal length")
    total = sum(left) + sum(right)
    if total == 0.0:
        return 0.0
    return sum(abs(a - b) for a, b in zip(left, right)) / total


def neighbourhood_profile(graph: Graph, depth: int = 1) -> dict[str, float]:
    """Weisfeiler-Lehman labels: what each node is, and what surrounds it.

    Each node starts labelled by its kind, and each round replaces that label
    with the kind plus the sorted labels of what feeds it and what it feeds.
    Counting the resulting labels compares two graphs by their local wiring
    without ever matching node against node -- the histogram does the work an
    all-pairs assignment would, at a fraction of the cost, which matters when
    an archive performs hundreds of thousands of comparisons per run.

    Depth is a resolution dial. Depth 0 is a bag of kinds, blind to wiring.
    Depth 2 makes almost every node unique on graphs this size, so every
    candidate reads as new and local competition stops happening. Depth 1 is
    the setting that distinguishes wiring while still merging near-relatives.

    Predecessors and successors are kept apart, because a delay feeding a sum
    is not a sum feeding a delay. Edge weights are deliberately excluded: a
    retuned copy must stay structurally identical to its parent, or the
    in-place tuning the archive runs on would break.
    """
    if depth < 0:
        raise ValueError("depth cannot be negative")
    labels = {node_id: node.kind for node_id, node in graph.nodes.items()}
    counts: dict[str, float] = {}
    for label in labels.values():
        counts[label] = counts.get(label, 0.0) + 1.0
    predecessors: dict[str, list[str]] = {node_id: [] for node_id in graph.nodes}
    successors: dict[str, list[str]] = {node_id: [] for node_id in graph.nodes}
    for edge in graph.edges.values():
        if edge.source in graph.nodes and edge.target in graph.nodes:
            predecessors[edge.target].append(edge.source)
            successors[edge.source].append(edge.target)

    for _ in range(depth):
        relabelled = {}
        for node_id in graph.nodes:
            before = ",".join(sorted(labels[item] for item in predecessors[node_id]))
            after = ",".join(sorted(labels[item] for item in successors[node_id]))
            relabelled[node_id] = f"{labels[node_id]}<({before})>({after})"
        labels = relabelled
        for label in labels.values():
            counts[label] = counts.get(label, 0.0) + 1.0
    return counts


def profile_distance(left: Mapping[str, float], right: Mapping[str, float]) -> float:
    """Bray-Curtis over two label histograms that need not share a vocabulary."""
    total = sum(left.values()) + sum(right.values())
    if total == 0.0:
        return 0.0
    return sum(
        abs(left.get(key, 0.0) - right.get(key, 0.0))
        for key in set(left) | set(right)
    ) / total


@dataclass(frozen=True, slots=True)
class ArchiveConfig:
    """Shape of the archive and the terms of its competition.

    ``probation`` is the number of later insertions during which a freshly
    admitted member cannot be evicted. It is the whole reason a new structure
    is worth admitting: born untuned it will score badly, and it needs to
    survive long enough for its own parameter variants to tune it in place.

    ``threshold`` is the distance below which two candidates count as the same
    idea. Left at ``None`` it is recomputed from the archive's own pairwise
    distances, because a fixed constant does not mean the same thing early --
    when every model is bad and their errors all track the target -- as it does
    once the members are good and differ only in the detail.

    ``novelty_admits`` decides whether a distant candidate must also beat the
    member it displaces. Leaving it on is what keeps the door open for
    structures that are not yet tuned; turning it off makes the archive a
    stricter elite set and is the ablation worth measuring against.

    ``size_tiebreak`` prefers, within a niche, a smaller graph scoring inside
    this relative tolerance of its neighbour. It was built on the theory that
    niche-scoped parsimony is safe where a flat node charge is not, and the
    measurement refuted that: at a tolerance of 0.02 it collapsed six of eight
    Narendra runs to a two-node graph sitting exactly on the memoryless floor,
    against seven of eight below the floor with it off. The theory missed that
    early on *every* poor model resembles the static one -- their residuals all
    track the target -- so they share its niche and the tiebreak shrinks the
    whole archive into it before any structure can grow. Left in place because
    one seed did improve, but it is off by default and should stay off absent a
    reason.

    ``min_nodes`` refuses candidates below a size. Measured on the Narendra
    plant, final graph size predicted the score with a rank correlation of
    -0.86: every run that grew found dynamics, every run that shrank stalled at
    the memoryless floor, and a run that had collapsed did not recover in a
    further nine thousand batches. Size is the search's working material rather
    than waste, so the useful guard is a lower bound, not a charge.

    ``structure_rule`` adds a second axis to the same-idea test. Behaviour
    alone has a measured blind spot: a graph generated from scratch, sharing no
    structure with anything held, sat a median behavioural distance of 0.009
    from its nearest member, because models that are bad are all bad the same
    way. ``"max"`` requires two candidates to be close on *both* axes to count
    as one idea. ``"mean"`` blends the two more softly.

    ``"only"`` ignores behaviour for the same-idea test, which the measurements
    favour: a retuned copy is structurally identical and so always meets its own
    parent, while a structural mutant sits far enough away to earn its own slot
    and the protection that comes with it. Under ``"max"`` a retuned copy whose
    weights moved its residuals is thrown out of its own niche instead, and the
    tuning the archive runs on stops happening. The cost of ``"only"`` is that
    two different structures computing the same function each hold a slot.

    ``local_competition`` exists to be turned off. With it disabled a candidate
    that lands in an occupied niche is simply dropped, so members never improve
    in place and the archive advances only by admitting novel structures. That
    isolates how much of the result comes from in-place tuning.
    """

    capacity: int = 10
    probation: int = 5
    threshold: float | None = None
    threshold_quantile: float = 0.25
    threshold_min: float = 0.0
    threshold_max: float = 2.0
    novelty_admits: bool = True
    size_tiebreak: float = 0.0
    local_competition: bool = True
    min_nodes: int = 0
    structure_rule: str = "off"

    def __post_init__(self) -> None:
        if self.capacity < 1:
            raise ValueError("capacity must be positive")
        if self.probation < 0:
            raise ValueError("probation cannot be negative")
        if self.min_nodes < 0:
            raise ValueError("min_nodes cannot be negative")
        if self.probation > self.capacity - 2:
            # At most one member is admitted per insertion, so at most
            # ``probation`` of them can be under protection at once. Holding
            # that below the capacity minus the never-evicted best member
            # guarantees a novel candidate always has somewhere to go --
            # otherwise a freshly filled archive protects every slot at once
            # and shuts the door precisely when it should be widest open.
            raise ValueError("probation must leave an evictable slot: at most capacity - 2")
        if self.threshold is not None and self.threshold < 0.0:
            raise ValueError("threshold cannot be negative")
        if self.structure_rule not in {"off", "max", "mean", "only"}:
            raise ValueError("structure_rule must be 'off', 'max', 'mean' or 'only'")
        if not 0.0 <= self.threshold_quantile <= 1.0:
            raise ValueError("threshold_quantile must be between zero and one")
        if self.threshold_min < 0.0 or self.threshold_max > 2.0:
            raise ValueError("threshold bounds must lie within the distance range")
        if self.threshold_min > self.threshold_max:
            raise ValueError("threshold_min cannot exceed threshold_max")


@dataclass(slots=True)
class ArchiveMember:
    graph: Graph
    score: float
    descriptor: tuple[float, ...]
    admitted: int
    improvements: int = 0
    structure: Mapping[str, float] | None = None


@dataclass(frozen=True, slots=True)
class Insertion:
    """What the archive did with one candidate, and why."""

    accepted: bool
    reason: str
    distance: float | None = None
    displaced: ArchiveMember | None = None


class Archive:
    """Fixed-size local-competition archive.

    Insertion is deterministic given the order candidates arrive in, so a
    driver may evaluate a whole batch in parallel and then insert the batch in
    a fixed order without changing the outcome.
    """

    def __init__(self, config: ArchiveConfig | None = None) -> None:
        self.config = config or ArchiveConfig()
        self.members: list[ArchiveMember] = []
        self.insertions = 0

    def __len__(self) -> int:
        return len(self.members)

    @property
    def best(self) -> ArchiveMember:
        if not self.members:
            raise ValueError("Archive is empty")
        return min(self.members, key=lambda member: member.score)

    def ranked(self) -> list[ArchiveMember]:
        return sorted(self.members, key=lambda member: member.score)

    def threshold(self) -> float:
        """The current same-idea distance, adaptive unless one was configured."""
        if self.config.threshold is not None:
            return self.config.threshold
        if len(self.members) < 2:
            return 0.0
        distances = sorted(
            self._distance(left.descriptor, left.structure, right)
            for index, left in enumerate(self.members)
            for right in self.members[index + 1 :]
        )
        position = int(self.config.threshold_quantile * (len(distances) - 1))
        # An unbounded quantile drifts to both useless extremes: pinned high it
        # calls every candidate a variant of something and admits no new
        # structure at all, collapsed to nearly zero it calls everything novel
        # and never lets a niche consolidate. Both were observed stalling runs.
        return min(
            max(distances[position], self.config.threshold_min),
            self.config.threshold_max,
        )

    def insert(
        self,
        graph: Graph,
        score: float,
        descriptor: Sequence[float] | None,
        structure: Mapping[str, float] | None = None,
    ) -> Insertion:
        """Offer one evaluated candidate to the archive."""
        self.insertions += 1
        if descriptor is None or not descriptor or not isfinite(score):
            return Insertion(False, "invalid")
        if len(graph.nodes) < self.config.min_nodes:
            return Insertion(False, "too small")

        vector = tuple(float(value) for value in descriptor)
        shape = dict(structure) if structure else None
        if len(self.members) < self.config.capacity:
            self._admit(graph, score, vector, shape)
            return Insertion(True, "filled")

        distance, nearest = self._nearest(vector, shape)
        if distance <= self.threshold():
            if not self.config.local_competition:
                return Insertion(False, "no local competition", distance)
            if self._simplifies(graph, score, nearest):
                nearest.graph = graph
                nearest.score = score
                nearest.descriptor = vector
                nearest.structure = shape
                nearest.improvements += 1
                return Insertion(True, "simplified", distance)
            if score < nearest.score:
                # Same idea, tuned better: the variant takes its parent's slot
                # and inherits its standing, so improving a member never
                # renews the protection it has already used up.
                nearest.graph = graph
                nearest.score = score
                nearest.descriptor = vector
                nearest.structure = shape
                nearest.improvements += 1
                return Insertion(True, "improved", distance)
            return Insertion(False, "dominated", distance)

        return self._admit_novel(graph, score, vector, shape, distance)

    def _simplifies(
        self, graph: Graph, score: float, nearest: ArchiveMember
    ) -> bool:
        """Is this the same idea said more briefly, at no meaningful cost?"""
        if self.config.size_tiebreak <= 0.0:
            return False
        if len(graph.nodes) >= len(nearest.graph.nodes):
            return False
        allowed = abs(nearest.score) * self.config.size_tiebreak
        return score <= nearest.score + allowed

    def _admit_novel(
        self,
        graph: Graph,
        score: float,
        descriptor: tuple[float, ...],
        structure: Mapping[str, float] | None,
        distance: float,
    ) -> Insertion:
        best = self.best
        evictable = [
            member
            for member in self.members
            if member is not best
            and self.insertions - member.admitted > self.config.probation
        ]
        if not evictable:
            return Insertion(False, "no evictable slot", distance)
        victim = max(evictable, key=lambda member: member.score)
        if not self.config.novelty_admits and score >= victim.score:
            return Insertion(False, "worse than worst", distance)
        self.members.remove(victim)
        self._admit(graph, score, descriptor, structure)
        return Insertion(True, "novel", distance, victim)

    def _admit(
        self,
        graph: Graph,
        score: float,
        descriptor: tuple[float, ...],
        structure: Mapping[str, float] | None = None,
    ) -> None:
        self.members.append(
            ArchiveMember(graph, score, descriptor, self.insertions, structure=structure)
        )

    def _distance(
        self,
        descriptor: tuple[float, ...],
        structure: Mapping[str, float] | None,
        member: ArchiveMember,
    ) -> float:
        behaviour = correlation_distance(descriptor, member.descriptor)
        if (
            self.config.structure_rule == "off"
            or structure is None
            or member.structure is None
        ):
            return behaviour
        shape = profile_distance(structure, member.structure)
        if self.config.structure_rule == "only":
            return shape
        if self.config.structure_rule == "max":
            return max(behaviour, shape)
        return (behaviour + shape) / 2.0

    def _nearest(
        self, descriptor: tuple[float, ...], structure: Mapping[str, float] | None = None
    ) -> tuple[float, ArchiveMember]:
        return min(
            (
                (self._distance(descriptor, structure, member), member)
                for member in self.members
            ),
            key=lambda pair: pair[0],
        )
