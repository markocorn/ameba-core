"""The gene dictionary: what every integer in a genome means.

A genome is a flat sequence of integers. None of them carries a value directly;
each is a key into this dictionary, which says what the gene does when the
decoder meets it. Four categories of gene exist:

``structural``
    Open a new node record (``node:<kind>``) or an output record
    (``out:y<c>``). Everything that follows, up to the next structural gene,
    describes that record.

``connection``
    Add an incoming edge to the open record. Sources are addressed
    *relatively* -- ``src:-2`` is the node two records back, ``src:+1`` the
    next one -- except the model inputs, which are absolute (``src:u0``).
    Relative addressing is what lets a block of genes mean the same thing
    wherever crossover puts it.

``parameter``
    An edge weight or a node parameter, as one value from a fixed alphabet:
    ``w=0.5``, ``alpha=0.25``, ``gain=-1``.

``temporal``
    Parameters that set a time scale rather than an amplitude: a delay's
    ``steps`` and a derivative's ``time_step``.

**IDs are permanent.** Each family owns a fixed block of the integer range and
assigns IDs by position within its own value list, so a family can only ever be
*appended* to: a new operator or a new allele takes the next free ID in its
block, and no existing ID changes meaning. That is what makes two genomes
comparable gene by gene, and it is pinned by ``tests/test_genome_encoding.py``.
Do not reorder any table below; add to the end of one instead.
"""

from __future__ import annotations

import math
from dataclasses import dataclass

#: Bumped only if an existing ID is ever reassigned, which the rule above
#: forbids; recorded in saved genomes so an old file is never silently misread.
DICTIONARY_VERSION = 1

STRUCTURAL = "structural"
CONNECTION = "connection"
PARAMETER = "parameter"
TEMPORAL = "temporal"


@dataclass(frozen=True, slots=True)
class Gene:
    """One dictionary entry.

    ``family`` is what the decoder dispatches on; the remaining fields are the
    payload for that family and are ``None`` where they do not apply.
    """

    id: int
    family: str
    category: str
    label: str
    kind: str | None = None
    channel: int | None = None
    offset: int | None = None
    name: str | None = None
    value: float | int | None = None


# --- Structural: 1..49 node kinds, 50..59 output records -------------------

#: Append-only. The position of a kind here is its permanent ID minus one.
NODE_KINDS: tuple[str, ...] = (
    "add",
    "multiply",
    "negate",
    "reciprocal",
    "sin",
    "tanh",
    "square",
    "sqrt",
    "abs",
    "exp",
    "log",
    "delay",
    "integral",
    "derivative",
    "filter_lp",
    "filter_hp",
    "constant",
)
NODE_BASE = 1
OUTPUT_BASE = 50
MAX_CHANNELS = 10

# --- Connection: 100..109 inputs, 110..141 back, 150..181 ahead -------------

INPUT_BASE = 100
BACK_BASE = 110
AHEAD_BASE = 150
#: How far a relative reference can reach. A model needing a longer edge has to
#: route it through intermediate nodes, which bounds the dictionary.
MAX_OFFSET = 32

# --- Parameter: 200..299 weights, 300.. node parameters ---------------------

WEIGHT_BASE = 200
#: Edge weights. Dense near zero, because small couplings are where a linear
#: model's poles are tuned, and inside the policy's default range of +-2.
WEIGHTS: tuple[float, ...] = (
    -2.0, -1.5, -1.0, -0.75, -0.5, -0.25, -0.1, -0.05,
    0.05, 0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0,
)

#: Node parameter alphabets, each with its own fixed block. Every value must lie
#: inside the limits :class:`ameba_signal.SignalGraphPolicy` enforces.
#: ``initial`` and ``initial_input`` have no genes: they are starting offsets
#: rather than dynamics, so they stay at their default of zero.
PARAMETER_FAMILIES: tuple[tuple[str, int, str, tuple[float | int, ...]], ...] = (
    ("gain", 300, PARAMETER, (-2.0, -1.0, -0.5, -0.1, 0.1, 0.5, 1.0, 2.0)),
    ("alpha", 320, PARAMETER, (0.05, 0.1, 0.25, 0.5, 0.75, 0.9, 0.95)),
    ("value", 340, PARAMETER, (-2.0, -1.0, -0.5, 0.0, 0.5, 1.0, 2.0)),
    ("phase", 360, PARAMETER, (0.0, math.pi / 2.0, math.pi, -math.pi / 2.0)),
    ("steps", 600, TEMPORAL, (1, 2, 3, 4, 5, 6, 8, 10)),
    ("time_step", 620, TEMPORAL, (0.1, 1.0, 3.0, 10.0)),
)
#: How many IDs each parameter family may ever grow to.
FAMILY_CAPACITY = 20

#: The parameters each node kind owns, and the value used when no gene sets
#: one. Mirrors the defaults of ``ameba_signal.policy``.
KIND_PARAMETERS: dict[str, dict[str, float | int]] = {
    "constant": {"value": 0.0},
    "sin": {"phase": 0.0},
    "delay": {"steps": 1, "initial": 0.0},
    "derivative": {"time_step": 1.0, "initial": 0.0},
    "filter_hp": {"alpha": 0.5, "initial": 0.0, "initial_input": 0.0},
    "filter_lp": {"alpha": 0.5, "initial": 0.0},
    "integral": {"gain": 1.0, "initial": 0.0},
}

DEFAULT_WEIGHT = 1.0


def _format(value: float | int) -> str:
    if isinstance(value, int):
        return str(value)
    if value == math.pi / 2.0:
        return "pi/2"
    if value == -math.pi / 2.0:
        return "-pi/2"
    if value == math.pi:
        return "pi"
    return f"{value:g}"


def _build() -> dict[int, Gene]:
    genes: list[Gene] = []
    for index, kind in enumerate(NODE_KINDS):
        genes.append(Gene(NODE_BASE + index, "node", STRUCTURAL, f"node:{kind}", kind=kind))
    for channel in range(MAX_CHANNELS):
        genes.append(
            Gene(OUTPUT_BASE + channel, "output", STRUCTURAL, f"out:y{channel}", channel=channel)
        )
    for channel in range(MAX_CHANNELS):
        genes.append(
            Gene(INPUT_BASE + channel, "input", CONNECTION, f"src:u{channel}", channel=channel)
        )
    for offset in range(1, MAX_OFFSET + 1):
        genes.append(Gene(BACK_BASE + offset - 1, "back", CONNECTION, f"src:-{offset}", offset=offset))
        genes.append(Gene(AHEAD_BASE + offset - 1, "ahead", CONNECTION, f"src:+{offset}", offset=offset))
    for index, weight in enumerate(WEIGHTS):
        genes.append(
            Gene(WEIGHT_BASE + index, "weight", PARAMETER, f"w={_format(weight)}", name="weight", value=weight)
        )
    for name, base, category, values in PARAMETER_FAMILIES:
        if len(values) > FAMILY_CAPACITY:
            raise ValueError(f"Parameter family {name!r} outgrew its ID block")
        for index, value in enumerate(values):
            genes.append(
                Gene(base + index, "parameter", category, f"{name}={_format(value)}", name=name, value=value)
            )
    table: dict[int, Gene] = {}
    for gene in genes:
        if gene.id in table:
            raise ValueError(f"Gene ID {gene.id} is assigned twice")
        table[gene.id] = gene
    return table


#: The genes the free-sequence encodings and every codebook are built from.
CORE_GENES: dict[int, Gene] = _build()

# --- Fine weights: 1000..1998 ----------------------------------------------

#: A fine, ordered grid of edge weights over the policy's +-2, for encodings
#: whose weight locus is a number from 1 to 999: level ``k`` is
#: ``-2 + 4 * k / 998``, so a step of one level moves a weight by about 0.004.
#: Kept out of :data:`CORE_GENES` so the codebooks, which are pinned, stay as
#: they were.
FINE_WEIGHT_BASE = 1000
FINE_WEIGHT_LEVELS = 999


def fine_weight(level: int) -> float:
    return -2.0 + 4.0 * level / (FINE_WEIGHT_LEVELS - 1)


def fine_weight_gene(level: int) -> int:
    if not 0 <= level < FINE_WEIGHT_LEVELS:
        raise ValueError(f"Fine weight level {level} is outside 0..{FINE_WEIGHT_LEVELS - 1}")
    return FINE_WEIGHT_BASE + level


# --- Log weights: 2000..2199 -------------------------------------------------

#: Edge weights on a logarithmic scale: 100 magnitudes from 0.001 to 2, evenly
#: spaced in log, negative then positive, in value order. A small coupling --
#: the 0.12 of the linear plant, say -- gets as fine a grid as a large one gets
#: coarse, which a linear grid cannot offer at any size.
LOG_WEIGHT_BASE = 2000
LOG_WEIGHT_MAGNITUDES = 100
LOG_WEIGHT_LEVELS = 2 * LOG_WEIGHT_MAGNITUDES


def log_weight(level: int) -> float:
    if not 0 <= level < LOG_WEIGHT_LEVELS:
        raise ValueError(f"Log weight level {level} is outside 0..{LOG_WEIGHT_LEVELS - 1}")
    low, high = math.log10(0.001), math.log10(2.0)
    if level < LOG_WEIGHT_MAGNITUDES:  # negative, from -2 up towards -0.001
        step = LOG_WEIGHT_MAGNITUDES - 1 - level
        return -min(2.0, 10 ** (low + (high - low) * step / (LOG_WEIGHT_MAGNITUDES - 1)))
    step = level - LOG_WEIGHT_MAGNITUDES
    # Clamped because 10 ** log10(2) rounds a hair above 2, outside the range.
    return min(2.0, 10 ** (low + (high - low) * step / (LOG_WEIGHT_MAGNITUDES - 1)))


def log_weight_gene(level: int) -> int:
    log_weight(level)  # validates the level
    return LOG_WEIGHT_BASE + level


# --- Soft nodes: 700..749, biases: 760..779 -------------------------------------

#: The soft node kinds, append-only like every family. Kept out of
#: :data:`CORE_GENES` with the other later additions, so the pinned codebooks
#: are unchanged.
SOFT_KINDS: tuple[str, ...] = (
    "soft_lin", "soft_mul", "soft_sq", "soft_bump", "soft_exp", "soft_log",
    "soft_alog", "soft_sqrt", "soft_asqrt", "soft_sin", "soft_abs",
)
SOFT_NODE_BASE = 700
BIAS_BASE = 760
#: The bias a soft node adds to its input sum, shifting its shape left or right.
BIASES: tuple[float, ...] = (-1.5, -1.0, -0.5, 0.0, 0.5, 1.0, 1.5)

for _kind in SOFT_KINDS:
    KIND_PARAMETERS[_kind] = {"bias": 0.0}

#: Every gene, keyed by its permanent ID.
GENES: dict[int, Gene] = {
    **CORE_GENES,
    **{
        FINE_WEIGHT_BASE + level: Gene(
            FINE_WEIGHT_BASE + level, "weight", PARAMETER,
            f"w={fine_weight(level):.3f}", name="weight", value=fine_weight(level),
        )
        for level in range(FINE_WEIGHT_LEVELS)
    },
    **{
        SOFT_NODE_BASE + index: Gene(SOFT_NODE_BASE + index, "node", STRUCTURAL, f"node:{kind}", kind=kind)
        for index, kind in enumerate(SOFT_KINDS)
    },
    **{
        BIAS_BASE + index: Gene(BIAS_BASE + index, "parameter", PARAMETER, f"bias={_format(value)}",
                                name="bias", value=value)
        for index, value in enumerate(BIASES)
    },
    **{
        LOG_WEIGHT_BASE + level: Gene(
            LOG_WEIGHT_BASE + level, "weight", PARAMETER,
            f"w={log_weight(level):.4g}", name="weight", value=log_weight(level),
        )
        for level in range(LOG_WEIGHT_LEVELS)
    },
}

#: Gene IDs per parameter name, in alphabet order, so a parameter mutation can
#: step to a neighbouring value.
PARAMETER_LADDERS: dict[str, tuple[int, ...]] = {
    "weight": tuple(WEIGHT_BASE + index for index in range(len(WEIGHTS))),
    **{
        name: tuple(base + index for index in range(len(values)))
        for name, base, _, values in PARAMETER_FAMILIES
    },
}


PARAMETER_LADDERS["bias"] = tuple(BIAS_BASE + index for index in range(len(BIASES)))


def node_gene(kind: str) -> int:
    if kind in SOFT_KINDS:
        return SOFT_NODE_BASE + SOFT_KINDS.index(kind)
    return NODE_BASE + NODE_KINDS.index(kind)


def output_gene(channel: int) -> int:
    if not 0 <= channel < MAX_CHANNELS:
        raise ValueError(f"Output channel {channel} is outside the dictionary")
    return OUTPUT_BASE + channel


def input_gene(channel: int) -> int:
    if not 0 <= channel < MAX_CHANNELS:
        raise ValueError(f"Input channel {channel} is outside the dictionary")
    return INPUT_BASE + channel


def source_gene(offset: int) -> int:
    """A relative source: negative reaches back, positive reaches ahead."""
    if offset == 0 or abs(offset) > MAX_OFFSET:
        raise ValueError(f"Relative offset {offset} is outside 1..{MAX_OFFSET}")
    return (BACK_BASE if offset < 0 else AHEAD_BASE) + abs(offset) - 1


def weight_gene(value: float) -> int:
    return WEIGHT_BASE + WEIGHTS.index(value)


def parameter_gene(name: str, value: float | int) -> int:
    if name == "weight":
        return weight_gene(float(value))
    for family, base, _, values in PARAMETER_FAMILIES:
        if family == name:
            return base + values.index(value)
    raise ValueError(f"No parameter genes for {name!r}")


def label(gene_id: int) -> str:
    gene = GENES.get(gene_id)
    return gene.label if gene is not None else f"?{gene_id}"


def describe(genome: tuple[int, ...] | list[int]) -> str:
    """A genome as readable labels, one record per line."""
    lines: list[str] = []
    current: list[str] = []
    for gene_id in genome:
        gene = GENES.get(gene_id)
        if gene is not None and gene.category == STRUCTURAL and current:
            lines.append(" ".join(current))
            current = []
        current.append(f"{gene_id}:{label(gene_id)}")
    if current:
        lines.append(" ".join(current))
    return "\n".join(lines)
