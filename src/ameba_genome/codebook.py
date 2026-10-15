"""Codebooks: which integer stands for which gene.

The dictionary fixes what genes *exist*; a codebook fixes the numbers written
in a genome. Under context-aware variation the choice is cosmetic, because
mutation picks genes as symbols. Under numeric variation -- a gene mutated
from 150 to 151 with no idea what either means -- it decides what a small
change *is*, so the layout becomes part of the experiment:

``ordered``
    The dictionary's own IDs. Families sit in contiguous blocks and parameter
    values in value order, so ``+1`` is usually a meaningful neighbour
    (``w=0.5 -> w=0.75``, ``src:+1 -> src:+2``), and occasionally falls into
    a gap and becomes an intron.

``hashed``
    A 4-digit number derived from each gene's label by hash, so nothing is in
    sequence and ``+1`` almost always lands on an intron. Permanent without a
    stored table: the same label always hashes to the same number.

``dense``
    Consecutive numbers from 1000 with no gaps, in dictionary order: the
    families stay in blocks, and every number is a gene.

Genomes are stored and varied in codebook numbers and translated to dictionary
IDs only for decoding. A number the codebook does not know translates to
nothing, and the decoder treats it as an intron.
"""

from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
from typing import Sequence

from .dictionary import CORE_GENES as GENES

#: Translates to no dictionary gene, so the decoder skips it.
INTRON = 0


@dataclass(frozen=True)
class Codebook:
    name: str
    #: codebook number -> dictionary ID
    to_gene: dict[int, int]
    #: The integer range numeric variation draws from, inclusive.
    low: int
    high: int

    @property
    def to_number(self) -> dict[int, int]:
        """A gene's first number; a redundant codebook has others as well."""
        numbers: dict[int, int] = {}
        for number in sorted(self.to_gene):
            numbers.setdefault(self.to_gene[number], number)
        return numbers

    def translate(self, genome: Sequence[int]) -> tuple[int, ...]:
        """Codebook numbers to dictionary IDs, for the decoder."""
        return tuple(self.to_gene.get(number, INTRON) for number in genome)

    def encode(self, genes: Sequence[int]) -> tuple[int, ...]:
        """Dictionary IDs to codebook numbers."""
        numbers = self.to_number
        return tuple(numbers[gene] for gene in genes)

    @property
    def density(self) -> float:
        """Fraction of the drawable range that means something."""
        return len(self.to_gene) / (self.high - self.low + 1)


def ordered() -> Codebook:
    ids = sorted(GENES)
    return Codebook("ordered", {gene: gene for gene in ids}, ids[0], ids[-1])


def hashed(low: int = 1000, high: int = 9999) -> Codebook:
    """Each gene at ``low + sha256(label) mod span``, salted past collisions.

    Genes are placed in dictionary-ID order, and a colliding label is rehashed
    with an increasing salt. A gene added later can therefore displace an
    existing one only if it takes that gene's number first, and the whole table
    is pinned by a test, so such a move fails loudly instead of silently
    rewriting saved genomes.
    """
    span = high - low + 1
    table: dict[int, int] = {}
    for gene in sorted(GENES.values(), key=lambda item: item.id):
        salt = 0
        while True:
            digest = sha256(f"{gene.label}#{salt}".encode()).digest()
            number = low + int.from_bytes(digest[:8], "big") % span
            if number not in table:
                table[number] = gene.id
                break
            salt += 1
    return Codebook("hashed", table, low, high)


def dense(start: int = 1000) -> Codebook:
    """Consecutive numbers from ``start`` with no gaps: every number is a gene.

    Genes are numbered in dictionary-ID order, so the families stay in blocks
    and parameter values in value order, as in ``ordered``, but ``+1`` never
    falls into a gap. A gene added to the dictionary must take the next number
    *after* the current last one, never a place in its family's block, or
    every number behind it would shift; the whole table is pinned by a test.
    """
    ids = sorted(GENES)
    return Codebook("dense", {start + rank: gene for rank, gene in enumerate(ids)},
                    start, start + len(ids) - 1)


def gene_groups() -> dict[str, tuple[int, ...]]:
    """Dictionary IDs by the role a gene plays, in dictionary order.

    Six roles rather than the four categories, because weights and node
    parameters, and node and output records, behave differently enough that
    sharing a role would let one crowd out the other.
    """
    groups: dict[str, list[int]] = {
        "node": [], "output": [], "connection": [], "weight": [], "parameter": [], "temporal": [],
    }
    for gene_id in sorted(GENES):
        gene = GENES[gene_id]
        if gene.family in ("node", "output", "weight"):
            groups[gene.family].append(gene_id)
        elif gene.family in ("input", "back", "ahead"):
            groups["connection"].append(gene_id)
        else:
            groups[gene.category].append(gene_id)
    return {name: tuple(ids) for name, ids in groups.items()}


def redundant(start: int = 1000, block: int = 240) -> Codebook:
    """Every role an equal block of numbers, each gene a run of synonyms.

    The genetic code's arrangement: 64 codons for 20 amino acids. Here each of
    the six roles gets ``block`` consecutive numbers, so a uniform draw lands
    on a node kind as often as on a connection, however many of each exist.
    Within a block every gene holds a contiguous run of equal length, any
    remainder going to the last, so ``+1`` is usually a synonym -- a neutral
    mutation -- and otherwise the next gene of the same role.
    """
    table: dict[int, int] = {}
    number = start
    for ids in gene_groups().values():
        run = block // len(ids)
        if run < 1:
            raise ValueError("block is smaller than a role's gene count")
        for index, gene_id in enumerate(ids):
            length = run if index < len(ids) - 1 else block - run * (len(ids) - 1)
            for _ in range(length):
                table[number] = gene_id
                number += 1
    return Codebook("redundant", table, start, number - 1)


CODEBOOKS = {"ordered": ordered, "hashed": hashed, "dense": dense, "redundant": redundant}
