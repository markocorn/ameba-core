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
from typing import Sequence

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
    """

    capacity: int = 10
    probation: int = 5
    threshold: float | None = None
    threshold_quantile: float = 0.25
    novelty_admits: bool = True

    def __post_init__(self) -> None:
        if self.capacity < 1:
            raise ValueError("capacity must be positive")
        if self.probation < 0:
            raise ValueError("probation cannot be negative")
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
        if not 0.0 <= self.threshold_quantile <= 1.0:
            raise ValueError("threshold_quantile must be between zero and one")


@dataclass(slots=True)
class ArchiveMember:
    graph: Graph
    score: float
    descriptor: tuple[float, ...]
    admitted: int
    improvements: int = 0


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
            correlation_distance(left.descriptor, right.descriptor)
            for index, left in enumerate(self.members)
            for right in self.members[index + 1 :]
        )
        position = int(self.config.threshold_quantile * (len(distances) - 1))
        return distances[position]

    def insert(
        self, graph: Graph, score: float, descriptor: Sequence[float] | None
    ) -> Insertion:
        """Offer one evaluated candidate to the archive."""
        self.insertions += 1
        if descriptor is None or not descriptor or not isfinite(score):
            return Insertion(False, "invalid")

        vector = tuple(float(value) for value in descriptor)
        if len(self.members) < self.config.capacity:
            self._admit(graph, score, vector)
            return Insertion(True, "filled")

        distance, nearest = self._nearest(vector)
        if distance <= self.threshold():
            if score < nearest.score:
                # Same idea, tuned better: the variant takes its parent's slot
                # and inherits its standing, so improving a member never
                # renews the protection it has already used up.
                nearest.graph = graph
                nearest.score = score
                nearest.descriptor = vector
                nearest.improvements += 1
                return Insertion(True, "improved", distance)
            return Insertion(False, "dominated", distance)

        return self._admit_novel(graph, score, vector, distance)

    def _admit_novel(
        self,
        graph: Graph,
        score: float,
        descriptor: tuple[float, ...],
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
        self._admit(graph, score, descriptor)
        return Insertion(True, "novel", distance, victim)

    def _admit(
        self, graph: Graph, score: float, descriptor: tuple[float, ...]
    ) -> None:
        self.members.append(
            ArchiveMember(graph, score, descriptor, self.insertions)
        )

    def _nearest(self, descriptor: tuple[float, ...]) -> tuple[float, ArchiveMember]:
        return min(
            (
                (correlation_distance(descriptor, member.descriptor), member)
                for member in self.members
            ),
            key=lambda pair: pair[0],
        )
