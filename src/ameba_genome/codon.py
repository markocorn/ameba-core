"""Start-codon encoding: records found by a marker, not by their position.

The state-space genome (:mod:`ameba_genome.statespace`) cuts its chromosomes
into records of six numbers by position, so a single number inserted or
deleted would shift every record after it; only whole records can come and go.
DNA finds its genes instead: a ribosome scans for a start codon and reads the
gene that follows it, and whatever lies between genes is not read at all.

Here every number is drawn from one alphabet ``0 .. alphabet-1``. A chromosome
is scanned from its start; the value ``start`` begins a record, the six numbers
after it are read as ``[kind, parameter, source1, weight1, source2, weight2]``,
each taken modulo the size of its role, and scanning resumes after the record.
Anything else is skipped. So

* a point insertion or deletion is a frameshift: it garbles the record it
  falls in, and the record after it if the shifted read swallows its start
  codon, but reading realigns at the next start codon it meets -- never a
  shift of everything downstream;
* a mutation that makes a ``start`` in skipped numbers gives birth to a gene
  from them, and one that destroys a ``start`` silences its gene;
* crossover can cut a chromosome anywhere, and the halves still read.

The scanned records are written into the fixed-record form and built by
:class:`StateSpaceEncoding`, so both encodings share one tested decoder and a
genome with no skipped numbers builds the graph its records would there.

    genome   [state numbers, state numbers..., output numbers..., terminals...]

The leading number is the length of the state chromosome; like the
state-space header it is not a locus. Terminals keep their fixed place at the
end and are read modulo their sizes too.
"""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from ameba_graph import Graph

from .decoding import Grammar
from .statespace import STATE_SLOTS, StateSpaceConfig, StateSpaceEncoding
from .tags import TagEncoding

Genome = tuple[int, ...]


@dataclass
class CodonConfig:
    """The codon alphabet and how genomes grow and shrink.

    ``indels`` picks the structural mutations: ``point`` inserts or deletes
    single numbers (frameshifts), ``record`` inserts a fresh start-marked
    record or deletes a whole read one (as the state-space encoding does), and
    ``both`` does either. ``junk`` is the most skipped numbers a fresh genome
    holds before each record: introns between genes. ``reset`` draws a new
    number from the whole alphabet (``alphabet``), or, for a number a record
    or terminal reads, from the size of the role it is read in (``role``), so
    a source draws an input as often as in the state-space encoding.
    ``tags``: records address each other by tag (:mod:`ameba_genome.tags`)
    instead of by position, with this many tags; None keeps positions.
    """

    alphabet: int = 32
    start: int | None = None  # default: the last value of the alphabet
    indels: str = "point"
    junk: int = 0
    reset: str = "alphabet"
    tags: int | None = None

    def __post_init__(self) -> None:
        if self.start is None:
            self.start = self.alphabet - 1
        if not 0 <= self.start < self.alphabet:
            raise ValueError("start must lie in the alphabet")
        if self.indels not in ("point", "record", "both"):
            raise ValueError("indels must be point, record or both")
        if self.junk < 0:
            raise ValueError("junk must not be negative")
        if self.reset not in ("alphabet", "role"):
            raise ValueError("reset must be alphabet or role")

    @classmethod
    def from_dict(cls, data: dict) -> "CodonConfig":
        return cls(**data)


class CodonEncoding:
    def __init__(self, grammar: Grammar, config: StateSpaceConfig | None = None,
                 codon: CodonConfig | None = None, primary_inputs: int | None = None) -> None:
        self.codon = codon or CodonConfig()
        config = config or StateSpaceConfig()
        if config.redundancy != 1:
            raise ValueError("the codon encoding reads numbers modulo their role; redundancy must be 1")
        if self.codon.tags:
            self.inner = TagEncoding(grammar, config, self.codon.tags, primary_inputs)
        else:
            self.inner = StateSpaceEncoding(grammar, config=config, primary_inputs=primary_inputs)
        self.config = config
        self.grammar = grammar
        self.width = self.inner.width
        largest = max(*self.inner.record_sizes("state"), *self.inner.record_sizes("output"),
                      *self.inner.terminal_sizes())
        if self.codon.alphabet < largest:
            raise ValueError(f"alphabet must hold at least {largest} values")
        structural = {"point": ("insert_number", "delete_number"),
                      "record": ("insert_record", "delete_record"),
                      "both": ("insert_number", "delete_number", "insert_record", "delete_record")}
        names = structural[self.codon.indels]
        self.weights = {"creep": 0.5, "reset": 0.3, **{name: 0.2 / len(names) for name in names}}

    # --- reading ---------------------------------------------------------------

    def tail(self) -> int:
        return self.inner.tail()

    def _parts(self, genome: Genome) -> tuple[list[int], list[int], list[int]]:
        cut = 1 + genome[0]
        end = len(genome) - self.tail()
        return list(genome[1:cut]), list(genome[cut:end]), list(genome[end:])

    @staticmethod
    def _join(state, output, terminals) -> Genome:
        return (len(state),) + tuple(state) + tuple(output) + tuple(terminals)

    def scan(self, numbers) -> list[tuple[int, int]]:
        """(start index, end index) of every record a chromosome reads."""
        found, i, start = [], 0, self.codon.start
        width = self.width
        while i + width < len(numbers):
            if numbers[i] == start:
                found.append((i, i + 1 + width))
                i += 1 + width
            else:
                i += 1
        return found

    def records(self, numbers, chromosome: str) -> list[tuple[int, ...]]:
        sizes = self.inner.record_sizes(chromosome)
        return [tuple(v % size for v, size in zip(numbers[a + 1:b], sizes)) for a, b in self.scan(numbers)]

    def fixed(self, genome: Genome) -> Genome:
        """The genome in the fixed-record form of the encoding it wraps."""
        state, output, terminals = self._parts(genome)
        states = self.records(state, "state")[:STATE_SLOTS]
        outputs = self.records(output, "output")
        tail = tuple(v % size for v, size in zip(terminals, self.inner.terminal_sizes()))
        return (len(states),) + tuple(g for r in states for g in r) + tuple(g for r in outputs for g in r) + tail

    def build(self, genome: Genome) -> Graph:
        return self.inner.build(self.fixed(genome))

    # --- creation ----------------------------------------------------------------

    def _junk(self, rng: Random) -> list[int]:
        values = [v for v in range(self.codon.alphabet) if v != self.codon.start]
        return [rng.choice(values) for _ in range(rng.randint(0, self.codon.junk))] if self.codon.junk else []

    def random_genome(self, rng: Random) -> Genome:
        """The wrapped encoding's fresh records, each behind a start codon."""
        fixed = self.inner.random_genome(rng)
        width = self.width
        records = [fixed[1 + i * width: 1 + (i + 1) * width] for i in range((len(fixed) - 1 - self.tail()) // width)]
        state, output = [], []
        for index, record in enumerate(records):
            target = state if index < fixed[0] else output
            target += self._junk(rng) + [self.codon.start] + list(record)
        return self._join(state, output, fixed[len(fixed) - self.tail():])

    # --- variation ----------------------------------------------------------------

    def creep(self, genome: Genome, rng: Random) -> Genome | None:
        if len(genome) < 2:
            return None
        position = rng.randrange(1, len(genome))
        value = min(max(genome[position] + rng.choice((-1, 1)), 0), self.codon.alphabet - 1)
        return genome[:position] + (value,) + genome[position + 1:]

    def reset(self, genome: Genome, rng: Random) -> Genome | None:
        if len(genome) < 2:
            return None
        position = rng.randrange(1, len(genome))
        size = self._role_size(genome, position) if self.codon.reset == "role" else self.codon.alphabet
        return genome[:position] + (rng.randrange(size),) + genome[position + 1:]

    def _role_size(self, genome: Genome, position: int) -> int:
        """The size of the role a number is read in; the alphabet for a start
        codon or a number nothing reads."""
        end = len(genome) - self.tail()
        if position >= end:
            return self.inner.terminal_sizes()[position - end]
        cut = 1 + genome[0]
        chromosome = "state" if position < cut else "output"
        numbers = genome[1:cut] if chromosome == "state" else genome[cut:end]
        index = position - (1 if chromosome == "state" else cut)
        for a, b in self.scan(numbers):
            if a < index < b:
                return self.inner.record_sizes(chromosome)[index - a - 1]
        return self.codon.alphabet

    def insert_number(self, genome: Genome, rng: Random) -> Genome | None:
        state, output, terminals = self._parts(genome)
        chromosome = state if rng.random() < 0.5 else output
        chromosome.insert(rng.randint(0, len(chromosome)), rng.randrange(self.codon.alphabet))
        return self._join(state, output, terminals)

    def delete_number(self, genome: Genome, rng: Random) -> Genome | None:
        state, output, terminals = self._parts(genome)
        chromosome = state if rng.random() < 0.5 else output
        if not chromosome:
            return None
        chromosome.pop(rng.randrange(len(chromosome)))
        return self._join(state, output, terminals)

    def insert_record(self, genome: Genome, rng: Random) -> Genome | None:
        """A fresh record behind a start codon, at any position."""
        state, output, terminals = self._parts(genome)
        name = "state" if rng.random() < 0.5 else "output"
        chromosome = state if name == "state" else output
        read = len(self.scan(chromosome))
        if name == "state" and read >= STATE_SLOTS:
            return None
        states = len(self.scan(state)) + (name == "state")
        record = self.inner._record(rng, name, rng.randint(0, read), min(states, STATE_SLOTS))
        at = rng.randint(0, len(chromosome))
        chromosome[at:at] = [self.codon.start] + record
        return self._join(state, output, terminals)

    def delete_record(self, genome: Genome, rng: Random) -> Genome | None:
        """One read record, start codon and all."""
        state, output, terminals = self._parts(genome)
        chromosome = state if rng.random() < 0.5 else output
        found = self.scan(chromosome)
        if not found:
            return None
        a, b = rng.choice(found)
        del chromosome[a:b]
        return self._join(state, output, terminals)

    def mutate(self, genome: Genome, rng: Random, attempts: int = 8) -> Genome:
        names = list(self.weights)
        for _ in range(attempts):
            name = rng.choices(names, [self.weights[n] for n in names])[0]
            child = getattr(self, name)(genome, rng)
            if child is not None and child != genome:
                return child
        return genome

    def crossover(self, left: Genome, right: Genome, rng: Random) -> Genome:
        """One cut anywhere in each chromosome of each parent; under maternal
        inheritance the state chromosome comes whole from ``left``."""
        ls, lo, lt = self._parts(left)
        rs, ro, _ = self._parts(right)
        if self.config.inheritance == "maternal":
            state = ls
        else:
            state = ls[: rng.randint(0, len(ls))] + rs[rng.randint(0, len(rs)):]
        output = lo[: rng.randint(0, len(lo))] + ro[rng.randint(0, len(ro)):]
        return self._join(state, output, lt)

    def translate(self, genome: Genome):
        raise NotImplementedError("a codon genome builds its graph directly; use build()")
