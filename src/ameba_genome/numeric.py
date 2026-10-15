"""Numeric variation: the GA edits integers without knowing what they mean.

The counterpart to :mod:`.variation`. Nothing here reads the dictionary, the
decoder, or the context: a mutation adds one to a number, or draws a fresh one
from the codebook's range, and crossover cuts anywhere. Whether a change is
small or drastic is decided entirely by how the codebook lays the genes out
(see :mod:`.codebook`), which is the point of the comparison.

Validity is unaffected, because the decoder is total and a number with no gene
behind it is an intron.

``creep``    one number moved by +-1 (rarely +-2 or more): 16 -> 17, 150 -> 151
``reset``    one number replaced by a uniform draw from the range
``insert``   a uniform draw inserted anywhere
``delete``   one number removed
"""

from __future__ import annotations

from random import Random
from typing import Callable

from .codebook import Codebook

Genome = tuple[int, ...]
NumericMutation = Callable[[Genome, Random, Codebook], "Genome | None"]


def creep(genome: Genome, rng: Random, codebook: Codebook) -> Genome | None:
    if not genome:
        return None
    position = rng.randrange(len(genome))
    # Mostly a unit step; a geometric tail occasionally jumps further.
    size = 1
    while rng.random() < 0.25:
        size += 1
    value = genome[position] + rng.choice((-size, size))
    value = min(max(value, codebook.low), codebook.high)
    return genome[:position] + (value,) + genome[position + 1:]


def reset(genome: Genome, rng: Random, codebook: Codebook) -> Genome | None:
    if not genome:
        return None
    position = rng.randrange(len(genome))
    value = rng.randint(codebook.low, codebook.high)
    return genome[:position] + (value,) + genome[position + 1:]


def insert(genome: Genome, rng: Random, codebook: Codebook) -> Genome | None:
    position = rng.randint(0, len(genome))
    return genome[:position] + (rng.randint(codebook.low, codebook.high),) + genome[position:]


def delete(genome: Genome, rng: Random, codebook: Codebook) -> Genome | None:
    if not genome:
        return None
    position = rng.randrange(len(genome))
    return genome[:position] + genome[position + 1:]


NUMERIC_MUTATIONS: dict[str, NumericMutation] = {
    "creep": creep,
    "reset": reset,
    "insert": insert,
    "delete": delete,
}

DEFAULT_NUMERIC_WEIGHTS: dict[str, float] = {
    "creep": 0.5,
    "reset": 0.2,
    "insert": 0.15,
    "delete": 0.15,
}


def numeric_mutate(
    genome: Genome,
    rng: Random,
    codebook: Codebook,
    weights: dict[str, float] | None = None,
    attempts: int = 8,
) -> Genome:
    weights = weights or DEFAULT_NUMERIC_WEIGHTS
    names = [name for name, weight in weights.items() if weight > 0.0]
    if not names:
        raise ValueError("At least one mutation needs a positive weight")
    for _ in range(attempts):
        name = rng.choices(names, [weights[name] for name in names])[0]
        child = NUMERIC_MUTATIONS[name](genome, rng, codebook)
        if child is not None and child != genome:
            return child
    return genome


class TwoLevelMutation:
    """Choose the role first, uniformly, then a gene within it.

    Works on dictionary IDs. The GA still knows nothing about context or
    position -- only which role each ID belongs to -- but the share of
    mutations each role gets is set by this choice instead of by how many IDs
    the role happens to own. ``creep`` stays inside the gene's own role,
    stepping to its neighbour there; ``reset`` and ``insert`` pick a role
    uniformly and then a gene in it, so they may change a locus's role.
    """

    WEIGHTS = {"creep": 0.5, "reset": 0.2, "insert": 0.15, "delete": 0.15}

    def __init__(self, groups: dict[str, tuple[int, ...]]) -> None:
        self.groups = [ids for ids in groups.values() if ids]
        self.role_of = {gene: ids for ids in self.groups for gene in ids}

    def _draw(self, rng: Random) -> int:
        return rng.choice(rng.choice(self.groups))

    def mutate(self, genome: Genome, rng: Random, attempts: int = 8) -> Genome:
        names = list(self.WEIGHTS)
        for _ in range(attempts):
            name = rng.choices(names, [self.WEIGHTS[n] for n in names])[0]
            child = getattr(self, "_" + name)(genome, rng)
            if child is not None and child != genome:
                return child
        return genome

    def _creep(self, genome: Genome, rng: Random) -> Genome | None:
        if not genome:
            return None
        position = rng.randrange(len(genome))
        ids = self.role_of.get(genome[position])
        if ids is None:
            return None
        index = min(max(ids.index(genome[position]) + rng.choice((-1, 1)), 0), len(ids) - 1)
        return genome[:position] + (ids[index],) + genome[position + 1:]

    def _reset(self, genome: Genome, rng: Random) -> Genome | None:
        if not genome:
            return None
        position = rng.randrange(len(genome))
        return genome[:position] + (self._draw(rng),) + genome[position + 1:]

    def _insert(self, genome: Genome, rng: Random) -> Genome | None:
        position = rng.randint(0, len(genome))
        return genome[:position] + (self._draw(rng),) + genome[position:]

    def _delete(self, genome: Genome, rng: Random) -> Genome | None:
        if not genome:
            return None
        position = rng.randrange(len(genome))
        return genome[:position] + genome[position + 1:]


def one_point(left: Genome, right: Genome, rng: Random) -> Genome:
    """Classical one-point crossover with an independent cut in each parent."""
    return left[: rng.randint(0, len(left))] + right[rng.randint(0, len(right)):]
