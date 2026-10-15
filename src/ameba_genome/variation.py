"""Gene-level variation: context-aware mutation, crossover, and random genomes.

Validity is already guaranteed by the decoder, which expresses any sequence of
integers as a valid graph. What the *context* adds is relevance: a mutation
draws its new gene only from the genes that would be expressed where it lands.
A ``gain`` gene is offered inside an ``integral`` record and nowhere else, a
``src:+2`` only where it would close a loop through a delay, a weight only
after a source. A mutation can still turn a later gene into an intron -- that
is how insertions and deletions work -- but it never spends itself writing one.

Four mutations are defined, matching the encoding rules:

``replace_gene``     swap one gene for another allowed at that position
``insert_gene``      add one allowed gene
``remove_gene``      delete one gene
``step_parameter``   move a parameter or temporal gene to a neighbouring value

``insert_record`` is a fifth, composite one: several gene insertions at once,
forming a complete node record. A lone node gene has no sources yet, so it is
expressed only after further insertions find it; this does all of that in one
step. It is an extension rather than a rule, and can be switched off.
"""

from __future__ import annotations

from random import Random
from typing import Callable, Sequence

from ameba_signal.operators import OPERATOR_ARITY

from .decoding import Genome, Grammar, Record, is_structural, parse
from .dictionary import (
    GENES,
    KIND_PARAMETERS,
    MAX_OFFSET,
    PARAMETER_LADDERS,
    input_gene,
    node_gene,
    output_gene,
    source_gene,
)

Mutation = Callable[[Genome, Random, Grammar], "Genome | None"]


def allowed_at(
    genome: Sequence[int], position: int, grammar: Grammar, replacing: bool = False
) -> tuple[int, ...]:
    """Gene IDs that would be expressed at ``position`` in this context.

    With ``replacing`` the gene currently at ``position`` is treated as gone,
    so the answer is what may stand there *instead* (the current gene can be
    among them; the caller excludes it).
    """
    remainder = list(genome[position + 1:] if replacing else genome[position:])
    prefix = list(genome[:position])
    record = parse(prefix, grammar)[-1]
    kinds = [GENES[g].kind for g in prefix + remainder if g in GENES and GENES[g].family == "node"]
    count = len(kinds)

    allowed: list[int] = [node_gene(kind) for kind in grammar.kinds]
    allowed += [output_gene(channel) for channel in range(grammar.outputs)]
    if not record.expressible:
        return tuple(allowed)

    if _has_room(record):
        allowed += [input_gene(channel) for channel in range(grammar.inputs)]
        allowed += [source_gene(-k) for k in range(1, min(MAX_OFFSET, record.position) + 1)]
        for k in range(1, MAX_OFFSET + 1):
            if record.role == "output":
                index = record.position + k - 1
                if index >= count:
                    break
                allowed.append(source_gene(k))
                continue
            index = record.position + k
            if index >= count:
                break
            if record.kind == "delay" or kinds[index] == "delay":
                allowed.append(source_gene(k))
    if record.sources:
        allowed += PARAMETER_LADDERS["weight"]
    if record.role == "node":
        for name in KIND_PARAMETERS.get(record.kind, {}):
            allowed += PARAMETER_LADDERS.get(name, ())
    return tuple(allowed)


def _has_room(record: Record) -> bool:
    if record.role == "output":
        return not record.sources
    maximum = OPERATOR_ARITY[record.kind][1]
    return maximum is None or len(record.sources) < maximum


def boundaries(genome: Sequence[int]) -> list[int]:
    """Positions where a record starts, plus the end: safe places to cut."""
    cuts = [index for index, gene in enumerate(genome) if is_structural(gene)]
    if not cuts or cuts[0] != 0:
        cuts.insert(0, 0)
    cuts.append(len(genome))
    return cuts


# --- Mutations ---------------------------------------------------------------


def replace_gene(genome: Genome, rng: Random, grammar: Grammar) -> Genome | None:
    if not genome:
        return None
    for _ in range(8):
        position = rng.randrange(len(genome))
        options = [g for g in allowed_at(genome, position, grammar, replacing=True) if g != genome[position]]
        if options:
            return genome[:position] + (rng.choice(options),) + genome[position + 1:]
    return None


def insert_gene(genome: Genome, rng: Random, grammar: Grammar) -> Genome | None:
    position = rng.randint(0, len(genome))
    gene = rng.choice(allowed_at(genome, position, grammar))
    if is_structural(gene):
        # A record opened mid-record would capture the rest of its neighbour's
        # genes, so a new record starts at the next boundary instead.
        position = next(cut for cut in boundaries(genome) if cut >= position)
    return genome[:position] + (gene,) + genome[position:]


def remove_gene(genome: Genome, rng: Random, grammar: Grammar) -> Genome | None:
    if not genome:
        return None
    position = rng.randrange(len(genome))
    return genome[:position] + genome[position + 1:]


def step_parameter(genome: Genome, rng: Random, grammar: Grammar) -> Genome | None:
    ladders = {gene: ladder for ladder in PARAMETER_LADDERS.values() for gene in ladder}
    positions = [index for index, gene in enumerate(genome) if gene in ladders]
    if not positions:
        return None
    position = rng.choice(positions)
    ladder = ladders[genome[position]]
    if len(ladder) < 2:
        return None
    index = ladder.index(genome[position])
    step = rng.choice((-1, 1))
    if not 0 <= index + step < len(ladder):
        step = -step
    return genome[:position] + (ladder[index + step],) + genome[position + 1:]


def insert_record(genome: Genome, rng: Random, grammar: Grammar) -> Genome | None:
    cut = rng.choice(boundaries(genome))
    before = sum(1 for gene in genome[:cut] if GENES.get(gene) is not None and GENES[gene].family == "node")
    after = sum(1 for gene in genome[cut:] if GENES.get(gene) is not None and GENES[gene].family == "node")
    record = random_record(rng, grammar, before, after)
    return genome[:cut] + tuple(record) + genome[cut:]


MUTATIONS: dict[str, Mutation] = {
    "replace_gene": replace_gene,
    "insert_gene": insert_gene,
    "remove_gene": remove_gene,
    "step_parameter": step_parameter,
    "insert_record": insert_record,
}

#: Relative frequency of each mutation. Parameter steps are the fine search,
#: replacements the coarse one; insertions and removals change size.
DEFAULT_MUTATION_WEIGHTS: dict[str, float] = {
    "replace_gene": 0.3,
    "insert_gene": 0.15,
    "remove_gene": 0.15,
    "step_parameter": 0.3,
    "insert_record": 0.1,
}


def mutate(
    genome: Genome,
    rng: Random,
    grammar: Grammar,
    weights: dict[str, float] | None = None,
    attempts: int = 8,
) -> Genome:
    """Apply one mutation drawn by weight; the genome back unchanged if none applies."""
    weights = weights or DEFAULT_MUTATION_WEIGHTS
    names = [name for name, weight in weights.items() if weight > 0.0]
    if not names:
        raise ValueError("At least one mutation needs a positive weight")
    for _ in range(attempts):
        name = rng.choices(names, [weights[name] for name in names])[0]
        child = MUTATIONS[name](genome, rng, grammar)
        if child is not None and child != genome:
            return child
    return genome


# --- Crossover ---------------------------------------------------------------


def splice(left: Genome, right: Genome, rng: Random) -> Genome:
    """One-point cut-and-splice at record boundaries.

    The head of one parent joins the tail of the other, each cut where a record
    starts, so no record is split. The two cuts are independent, so the child's
    length varies. Sources are relative, so a record keeps its local wiring in
    its new place; a reference that reached across the cut now reaches whatever
    the other parent put there, which is where crossover does its exploring.
    """
    head = rng.choice(boundaries(left))
    tail = rng.choice(boundaries(right))
    return left[:head] + right[tail:]


# --- Random genomes ----------------------------------------------------------


def random_record(rng: Random, grammar: Grammar, before: int, after: int) -> list[int]:
    """A complete node record whose sources resolve in its surroundings.

    ``before`` and ``after`` are how many node records precede and follow it.
    Only a delay reaches ahead, because only through a delay can it be legal
    without knowing what kind of node it reaches.
    """
    kind = rng.choice(grammar.kinds)
    genes = [node_gene(kind)]
    for name in KIND_PARAMETERS.get(kind, {}):
        ladder = PARAMETER_LADDERS.get(name)
        if ladder and rng.random() < 0.8:
            genes.append(rng.choice(ladder))
    minimum, maximum = OPERATOR_ARITY[kind]
    wanted = minimum if maximum is not None and maximum <= minimum else rng.randint(max(minimum, 1), 3)
    for _ in range(wanted):
        # Mostly reach the nearest earlier nodes, so random records chain into
        # something deep enough to reach an output instead of each reading the
        # input directly and all but the last being pruned.
        draw = rng.random()
        if kind == "delay" and after and draw < 0.35:
            genes.append(source_gene(rng.randint(1, min(MAX_OFFSET, after, 4))))
        elif before and draw < 0.8:
            genes.append(source_gene(-min(before, MAX_OFFSET, 1 + int(rng.expovariate(1.0)))))
        else:
            genes.append(input_gene(rng.randrange(grammar.inputs)))
        if rng.random() < 0.7:
            genes.append(rng.choice(PARAMETER_LADDERS["weight"]))
    return genes


def random_genome(rng: Random, grammar: Grammar, nodes: tuple[int, int] = (3, 8)) -> Genome:
    """Random node records followed by one output record per channel."""
    count = rng.randint(*nodes)
    genes: list[int] = []
    for index in range(count):
        genes += random_record(rng, grammar, index, count - index - 1)
    for channel in range(grammar.outputs):
        genes.append(output_gene(channel))
        if count and rng.random() < 0.85:
            genes.append(source_gene(-rng.randint(1, min(count, 3))))
        else:
            genes.append(input_gene(rng.randrange(grammar.inputs)))
        if rng.random() < 0.5:
            genes.append(rng.choice(PARAMETER_LADDERS["weight"]))
    return tuple(genes)
