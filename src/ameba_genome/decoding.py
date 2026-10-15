"""Deterministic decoding of an integer genome into a signal graph: D(G) -> A.

The decoder is *total*: every sequence of integers decodes to a graph that
:class:`ameba_signal.SignalGraphPolicy` accepts. Nothing a mutation or
crossover produces can therefore be invalid, and validity never has to be
checked or repaired after the fact. Genes that cannot be expressed where they
stand are introns -- carried, inherited, and silently not expressed -- which is
the same arrangement linear and Cartesian GP use.

Decoding runs in four passes.

1. **Records.** A structural gene opens a record; the genes after it, up to the
   next structural gene, describe it. A connection adds a source to the open
   record, a weight gene sets the weight of that record's latest source, and a
   parameter gene sets a parameter the record's kind owns (the last one wins).
   Genes before the first record, unknown IDs, and genes that do not apply to
   the open record are introns.

2. **Legal edges.** Node records are numbered in genome order. An edge from
   node ``s`` into node ``t`` is legal when ``s`` comes before ``t``, or when
   either end is a ``delay``. Every cycle therefore contains an edge that runs
   backwards, every such edge touches a delay, and so every feedback loop
   passes through one: an algebraic loop cannot be written down. Duplicate
   sources and self-loops are dropped.

3. **Arity.** A node keeps at most its kind's maximum number of sources, the
   first ones in genome order. A node left with fewer than its minimum is
   dead, and so is any reference to it; this is repeated until nothing
   changes, so the result does not depend on the order nodes are visited.

4. **Outputs.** Output ``c`` takes the first live source of the ``out:y<c>``
   records. With none, it falls back to input ``u_min(c, I-1)`` at unit
   weight -- the uninformed static seed the other search methods start from,
   so an empty genome is that seed. Nodes that reach no output are dropped.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Sequence

from ameba_graph import Edge, Graph, Node
from ameba_signal import interface_scaffold
from ameba_signal.operators import EVOLVABLE_OPERATORS, OPERATOR_ARITY

from .dictionary import (
    DEFAULT_WEIGHT,
    GENES,
    KIND_PARAMETERS,
    MAX_CHANNELS,
    NODE_KINDS,
    SOFT_KINDS,
    STRUCTURAL,
)

Genome = tuple[int, ...]


@dataclass(frozen=True, slots=True)
class Grammar:
    """What a genome is decoded against: the interface and the allowed kinds.

    ``kinds`` restricts which node genes are expressed. A node gene for any
    other kind still opens a record -- so the genes after it do not leak into
    the previous node -- but the record is dead.
    """

    inputs: int = 1
    outputs: int = 1
    kinds: tuple[str, ...] = EVOLVABLE_OPERATORS

    def __post_init__(self) -> None:
        if not 1 <= self.inputs <= MAX_CHANNELS or not 1 <= self.outputs <= MAX_CHANNELS:
            raise ValueError(f"A genome interface needs 1..{MAX_CHANNELS} inputs and outputs")
        unknown = set(self.kinds) - set(NODE_KINDS) - set(SOFT_KINDS)
        if unknown:
            raise ValueError(f"No node genes for kind(s): {', '.join(sorted(unknown))}")
        if not self.kinds:
            raise ValueError("At least one node kind is required")


@dataclass(slots=True)
class Record:
    """One decoded record: a node, an output, or the void before the first."""

    role: str  # "node", "output", or "void"
    start: int
    kind: str | None = None
    channel: int | None = None
    #: For a node, its index among node records; for an output, how many node
    #: records precede it. Relative sources resolve against this.
    position: int = 0
    expressible: bool = True
    parameters: dict[str, float | int] = field(default_factory=dict)
    #: (reference, weight); a reference is ("u", channel) or ("n", index).
    sources: list[list] = field(default_factory=list)


def parse(genome: Sequence[int], grammar: Grammar) -> list[Record]:
    """Pass 1: split a genome into records, ignoring what cannot apply."""
    records: list[Record] = [Record("void", 0, expressible=False)]
    nodes = 0
    for position, gene_id in enumerate(genome):
        gene = GENES.get(gene_id)
        if gene is None:
            continue
        if gene.family == "node":
            records.append(
                Record("node", position, kind=gene.kind, position=nodes,
                       expressible=gene.kind in grammar.kinds)
            )
            nodes += 1
            continue
        if gene.family == "output":
            records.append(
                Record("output", position, channel=gene.channel, position=nodes,
                       expressible=gene.channel < grammar.outputs)
            )
            continue
        record = records[-1]
        if not record.expressible:
            continue
        if gene.family == "input":
            if gene.channel < grammar.inputs and _accepts_sources(record):
                record.sources.append([("u", gene.channel), DEFAULT_WEIGHT])
        elif gene.family in ("back", "ahead"):
            if _accepts_sources(record):
                step = -gene.offset if gene.family == "back" else gene.offset
                # An output sits between node records, so "+1" from it is the
                # node at its own position rather than the one after.
                anchor = record.position - 1 if record.role == "output" and step > 0 else record.position
                record.sources.append([("n", anchor + step), DEFAULT_WEIGHT])
        elif gene.family == "weight":
            if record.sources:
                record.sources[-1][1] = gene.value
        elif gene.family == "parameter":
            if record.role == "node" and gene.name in KIND_PARAMETERS.get(record.kind, {}):
                record.parameters[gene.name] = gene.value
    return records


def _accepts_sources(record: Record) -> bool:
    if record.role == "output":
        return True
    return OPERATOR_ARITY[record.kind][1] != 0


def decode(genome: Sequence[int], grammar: Grammar) -> Graph:
    """D(G): the graph a genome encodes. Same genome, same graph, always."""
    records = parse(genome, grammar)
    nodes = [record for record in records if record.role == "node"]
    count = len(nodes)

    def legal(reference, target: Record) -> bool:
        if reference[0] == "u":
            return True
        index = reference[1]
        if not 0 <= index < count:
            return False
        if target.role == "output":
            # Outputs have no outgoing edges, so no source can close a loop.
            return True
        if index == target.position:
            return False
        return (
            index < target.position
            or nodes[index].kind == "delay"
            or target.kind == "delay"
        )

    # Pass 2: legal, deduplicated candidate sources per node, in genome order.
    candidates: list[list[tuple[tuple, float]]] = []
    for record in nodes:
        kept: list[tuple[tuple, float]] = []
        seen: set[tuple] = set()
        if record.expressible:
            for reference, weight in record.sources:
                if reference not in seen and legal(reference, record):
                    seen.add(reference)
                    kept.append((reference, weight))
        candidates.append(kept)

    # Pass 3: arity, to a fixed point. The alive set only shrinks, so this ends.
    alive = [record.expressible for record in nodes]
    chosen: list[list[tuple[tuple, float]]] = [[] for _ in nodes]
    changed = True
    while changed:
        changed = False
        for index, record in enumerate(nodes):
            if not alive[index]:
                continue
            minimum, maximum = OPERATOR_ARITY[record.kind]
            usable = [
                item for item in candidates[index]
                if item[0][0] == "u" or alive[item[0][1]]
            ]
            if maximum is not None:
                usable = usable[:maximum]
            chosen[index] = usable
            if len(usable) < minimum:
                alive[index] = False
                changed = True

    # Pass 4: outputs, then everything that can reach one.
    output_sources: dict[int, tuple[tuple, float]] = {}
    for record in records:
        if record.role != "output" or not record.expressible or record.channel in output_sources:
            continue
        for reference, weight in record.sources:
            if legal(reference, record) and (reference[0] == "u" or alive[reference[1]]):
                output_sources[record.channel] = (reference, weight)
                break
    for channel in range(grammar.outputs):
        output_sources.setdefault(
            channel, (("u", min(channel, grammar.inputs - 1)), DEFAULT_WEIGHT)
        )

    reached: set[int] = set()
    pending = [ref[1] for ref, _ in output_sources.values() if ref[0] == "n"]
    while pending:
        index = pending.pop()
        if index in reached:
            continue
        reached.add(index)
        pending.extend(ref[1] for ref, _ in chosen[index] if ref[0] == "n")

    graph = interface_scaffold(grammar.inputs, grammar.outputs)
    for index in sorted(reached):
        record = nodes[index]
        attributes = dict(KIND_PARAMETERS.get(record.kind, {}))
        attributes.update(record.parameters)
        graph.add_node(Node(_node_id(index), record.kind, attributes))
    for index in sorted(reached):
        for reference, weight in chosen[index]:
            _connect(graph, reference, _node_id(index), weight)
    for channel in range(grammar.outputs):
        reference, weight = output_sources[channel]
        _connect(graph, reference, f"y{channel}", weight)
    return graph


def expressed_nodes(genome: Sequence[int], grammar: Grammar) -> int:
    """How many node records survive decoding: the phenotype's size."""
    return sum(1 for node in decode(genome, grammar).nodes.values()
               if node.kind not in ("input", "output"))


def _node_id(index: int) -> str:
    return f"n{index}"


def _connect(graph: Graph, reference: tuple, target: str, weight: float) -> None:
    source = f"u{reference[1]}" if reference[0] == "u" else _node_id(reference[1])
    graph.add_edge(Edge(f"e_{source}_{target}", source, target, {"weight": weight}))


def is_structural(gene_id: int) -> bool:
    gene = GENES.get(gene_id)
    return gene is not None and gene.category == STRUCTURAL
