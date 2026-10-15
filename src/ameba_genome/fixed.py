"""Fixed-role encoding: every locus has one job, so a mutation keeps it.

The free encoding lets any number stand anywhere, so a blind mutation can turn
a weight into a node gene and re-partition every record after it. Here a genome
is a run of fixed-shape records instead, in the manner of Cartesian GP:

    node record    [kind, parameter, source1, weight1, source2, weight2]
    output record  [source, weight]       one per output channel, at the end

Each locus belongs to one *role* -- kind, parameter, source, weight -- and its
number is read only as that role's symbol, so a mutation always yields a
well-formed record: nothing changes role, and nothing is re-partitioned.

How a number becomes a symbol is the *layout*, chosen per role:

``direct``   the number is the symbol: 0..16 for 17 kinds, 0..15 for 16 weights
``runs``     the numbers 0..998, each symbol a contiguous run of ~999/n of them;
             +-1 is usually a synonym, otherwise the neighbouring symbol
``shuffle``  the same runs dealt out in a fixed random order; +-1 is a fresh,
             effectively random symbol of the same role
``fine``     weights only: 999 ordered levels from -2 to +2, a step of ~0.004

Beyond those, a number can pass through a transform before it is read as a
run, which changes what a small mutation means without changing what genes
exist (see :data:`EXOTIC_LAYOUTS`): ``xor:K``, ``mul:M``, ``gray``, ``sin:C``,
``sq``, ``logistic:R`` (bijective or nearly so), ``popcount``, ``digits``,
``collatz`` (many-to-one, read modulo the alphabet), ``cumulative`` and
``difference`` (read against the previous locus of the same role -- an
integral and a derivative over the genome), and ``log`` for weights on a
logarithmic grid. The ``bitflip`` mutation treats a locus as a bit field.

Some layouts and operators borrow from biology (:data:`NATURE`): ``codon``
reads a locus as three DNA bases through the standard genetic code, a stop
codon silencing a node; ``diploid:W`` and ``codominant:W`` carry two alleles
per locus; ``majority:B`` three copies, expressed by vote (canalisation);
``golden`` reads a number by the golden ratio, as a sunflower places seeds;
``gradient:C`` reads it against its position, as a morphogen gradient does;
``epigenetic`` silences a node for half the values of its kind. ``tag``
addressing binds a source to the node whose tag is nearest, like a key to a
lock. The mutations ``base``, ``duplicate_record``, ``transpose``, ``invert``
and ``burst`` are point substitution with transition bias, gene duplication,
a jumping gene, an inversion, and a burst of radiation; ``role_rates`` makes
some roles mutation hot spots; the ``transfer`` crossover is horizontal gene
transfer.

The named *schemes* ``base``, ``runs``, ``shuffle`` and ``mixed`` are fixed
combinations of layouts. Shuffled tables are drawn once from a fixed seed per
role, so they are permanent, and a test pins them.

A source symbol is ``0..I-1`` for the inputs, then ``-1..-K``, then ``+1..+K``.
A node keeps as many sources as its kind allows, at most two, and a locus its
kind does not use (a constant's sources, a tanh's second source, an add's
parameter) is carried unexpressed.

Everything above is the default. :class:`FixedConfig` makes each choice a
setting -- per-role layout, how many numbers a widened locus holds, how many
sources a node record carries (one to three), whether an output sums two
sources, relative or absolute source addressing, the mutation mix and the size
of a ``+-`` step, and the size of fresh genomes -- so a family of encodings can
be compared under one GA.

Decoding goes through the dictionary: :meth:`FixedEncoding.translate` writes
the canonical gene sequence and the ordinary decoder does the rest, so
legality, arity, and validity are exactly what they are everywhere else.
"""

from __future__ import annotations

from dataclasses import dataclass, field
import math
from random import Random

from ameba_signal.operators import OPERATOR_ARITY

from .decoding import Grammar
from .dictionary import (
    FINE_WEIGHT_LEVELS,
    LOG_WEIGHT_LEVELS,
    KIND_PARAMETERS,
    MAX_OFFSET,
    PARAMETER_LADDERS,
    WEIGHTS,
    fine_weight_gene,
    log_weight_gene,
    input_gene,
    node_gene,
    output_gene,
    source_gene,
    weight_gene,
)

Genome = tuple[int, ...]

#: The default record shapes, kept for callers that size genomes by hand.
NODE_WIDTH = 6
OUTPUT_WIDTH = 2
NODE_ROLES = ("kind", "parameter", "source", "weight", "source", "weight")
OUTPUT_ROLES = ("source", "weight")
#: Reach of a relative source in this encoding.
REACH = 8
#: The largest parameter ladder; a smaller one is indexed modulo its length.
PARAMETER_SLOTS = 8
#: How many numbers a widened locus holds by default: 0..998.
WIDE = 999
#: How many node positions an absolute source can name.
ABSOLUTE_SLOTS = 32

#: Per-role layout for each named scheme.
SCHEMES: dict[str, dict[str, str]] = {
    "base": {"kind": "direct", "parameter": "direct", "source": "direct", "weight": "direct"},
    "runs": {"kind": "runs", "parameter": "runs", "source": "runs", "weight": "runs"},
    "shuffle": {"kind": "shuffle", "parameter": "shuffle", "source": "shuffle", "weight": "shuffle"},
    "mixed": {"kind": "shuffle", "parameter": "runs", "source": "shuffle", "weight": "fine"},
}

DEFAULT_MUTATION = {"creep": 0.5, "reset": 0.3, "insert_record": 0.1, "remove_record": 0.1}
#: Every mutation a config may weight; those it leaves out get weight zero.
ALL_MUTATIONS = (*DEFAULT_MUTATION, "bitflip", "base", "duplicate_record", "transpose", "invert", "burst")
#: How many tags ``tag`` addressing distinguishes.
TAG_SPACE = 32
#: The standard genetic code, codons in TCAG order: T=0, C=1, A=2, G=3.
GENETIC_CODE = "FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG"
_AMINO = sorted(set(GENETIC_CODE) - {"*"})
#: A symbol that silences the node it stands for.
SILENT = -1

NATURE = {
    "codon": "a locus is three bases read by the standard genetic code; synonymous codons are neutral, a stop codon silences the node",
    "diploid:W": "two alleles per locus (0..W-1 each), the dominant (larger) one expressed",
    "codominant:W": "two alleles per locus, their mean expressed",
    "majority:B": "three copies per locus (0..B-1 each), expressed by majority vote: canalisation",
    "golden": "number read by the golden ratio, as a sunflower places its seeds",
    "gradient:C": "number read against its record's position, as a morphogen gradient does",
    "epigenetic": "kind locus: half its values silence the node, as methylation does",
    "tag addressing": "a source binds to the earlier node with the nearest tag: lock and key",
    "base": "mutation: change one base of a codon, transitions twice as likely as transversions",
    "duplicate_record": "mutation: copy a node record elsewhere: gene duplication",
    "transpose": "mutation: move a node record elsewhere: a jumping gene",
    "invert": "mutation: reverse a block of node records: an inversion",
    "burst": "mutation: three to eight changes at once: a burst of radiation",
    "role_rates": "per-role mutation weights: mutation hot spots",
    "transfer": "crossover: one record of the other parent inserted: horizontal gene transfer",
}

#: Layouts beyond direct/runs/shuffle/fine, with what a +-1 step becomes.
EXOTIC_LAYOUTS = {
    "xor:K": "number XOR K, then runs: neighbours stay close in the low bits",
    "mul:M": "number times M modulo the width, then runs: a fixed stride scramble",
    "gray": "Gray-decoded number, then runs: one bit flip is often a small step",
    "sin:C": "sine of C times the number, stretched over the width, then runs",
    "sq": "number squared modulo the width, then runs",
    "logistic:R": "20 steps of the logistic map from the number, then runs: chaotic for R near 4",
    "popcount": "count of one-bits, modulo the alphabet: many numbers per symbol",
    "digits": "decimal digit sum, modulo the alphabet",
    "collatz": "Collatz stopping time of number + 1, modulo the alphabet",
    "cumulative": "running sum of this role's loci so far, then runs: an integral",
    "difference": "difference to the previous locus of this role, then runs: a derivative",
    "log": "weights only: 200 levels, 0.001 to 2 in magnitude, even in log",
}
_ARGUMENT = {"xor", "mul", "sin", "logistic", "diploid", "codominant", "majority", "gradient"}
_PLAIN = {"direct", "runs", "shuffle", "gray", "sq", "popcount", "digits", "collatz", "cumulative", "difference",
          "codon", "golden", "epigenetic"}
_MANY_TO_ONE = {"popcount", "digits", "collatz"}
_CONTEXT = {"cumulative", "difference", "gradient"}


def known_layout(layout: str, role: str) -> bool:
    name, _, argument = layout.partition(":")
    if name in ("fine", "log"):
        return role == "weight" and not argument
    if name == "epigenetic":
        return role == "kind" and not argument
    if name in _ARGUMENT:
        try:
            float(argument)
        except ValueError:
            return False
        return True
    return name in _PLAIN and not argument


def _gray_decode(code: int) -> int:
    value = code
    while code:
        code >>= 1
        value ^= code
    return value


def _collatz(n: int) -> int:
    steps = 0
    while n != 1 and steps < 500:
        n = n // 2 if n % 2 == 0 else 3 * n + 1
        steps += 1
    return steps


def _transformed(name: str, argument: str, n: int, width: int) -> int:
    if name == "xor":
        return (n ^ int(argument)) % width
    if name == "mul":
        return (n * int(argument)) % width
    if name == "gray":
        return _gray_decode(n) % width
    if name == "sin":
        return int((math.sin(float(argument) * n) + 1.0) / 2.0 * (width - 1) + 0.5)
    if name == "sq":
        return (n * n) % width
    x = (n + 0.5) / width  # logistic
    rate = float(argument)
    for _ in range(20):
        x = rate * x * (1.0 - x)
    return min(width - 1, max(0, int(x * width)))


def _many_to_one(name: str, n: int) -> int:
    if name == "popcount":
        return bin(n).count("1")
    if name == "digits":
        return sum(int(digit) for digit in str(n))
    return _collatz(n + 1)


@dataclass
class FixedConfig:
    """Every choice the fixed-role encoding makes, as a setting.

    ``layouts``        role -> ``direct`` | ``runs`` | ``shuffle`` | ``fine``,
                       or the name of a scheme
    ``width``          numbers in a widened (runs/shuffle) locus
    ``sources``        source/weight pairs per node record, 1..3
    ``output_sources`` 1, or 2 to have each output sum two sources
    ``reach``          how far a relative source reaches either way
    ``addressing``     ``relative`` (back/ahead) or ``absolute`` (node index)
    ``mutation``       weights of creep / reset / insert_record / remove_record
    ``step``           ``unit``, ``geometric:P`` or ``uniform:N`` creep size
    ``init_nodes``     node records in a fresh genome, as (low, high)
    """

    layouts: dict[str, str] | str = field(default_factory=lambda: dict(SCHEMES["base"]))
    width: int = WIDE
    sources: int = 2
    output_sources: int = 1
    reach: int = REACH
    addressing: str = "relative"
    mutation: dict[str, float] = field(default_factory=lambda: dict(DEFAULT_MUTATION))
    step: str = "unit"
    init_nodes: tuple[int, int] = (3, 10)
    #: Per-role weights for where a point mutation lands; None is uniform.
    role_rates: dict[str, float] | None = None

    def __post_init__(self) -> None:
        if isinstance(self.layouts, str):
            self.layouts = dict(SCHEMES[self.layouts])
        self.layouts = {**SCHEMES["base"], **self.layouts}
        for role, layout in self.layouts.items():
            if not known_layout(layout, role):
                raise ValueError(f"Unknown layout {layout!r} for role {role!r}")
        if not 1 <= self.sources <= 3:
            raise ValueError("sources must be 1..3")
        if self.output_sources not in (1, 2):
            raise ValueError("output_sources must be 1 or 2")
        if not 1 <= self.reach <= MAX_OFFSET:
            raise ValueError(f"reach must be 1..{MAX_OFFSET}")
        if self.addressing not in ("relative", "absolute", "tag"):
            raise ValueError("addressing must be relative, absolute or tag")
        if self.addressing == "tag":
            self.layouts.setdefault("tag", "direct")
        if self.role_rates is not None:
            unknown = set(self.role_rates) - {"kind", "parameter", "source", "weight", "tag"}
            if unknown:
                raise ValueError(f"Unknown role(s) in role_rates: {', '.join(sorted(unknown))}")
        if self.width < 2:
            raise ValueError("width must be at least 2")
        self.init_nodes = tuple(self.init_nodes)
        self.mutation = {**{name: 0.0 for name in ALL_MUTATIONS}, **self.mutation}
        unknown = set(self.mutation) - set(ALL_MUTATIONS)
        if unknown:
            raise ValueError(f"Unknown mutation(s): {', '.join(sorted(unknown))}")
        if not any(weight > 0 for weight in self.mutation.values()):
            raise ValueError("At least one mutation needs a positive weight")
        kind, _, value = self.step.partition(":")
        if kind not in ("unit", "geometric", "uniform") or (kind != "unit" and not value):
            raise ValueError(f"Unknown step {self.step!r}")

    @classmethod
    def from_dict(cls, data: dict) -> "FixedConfig":
        return cls(**data)


def _runs(symbols: int, width: int = WIDE) -> list[int]:
    return [value * symbols // width for value in range(width)]


def _shuffled(symbols: int, role: str, width: int = WIDE) -> list[int]:
    table = _runs(symbols, width)
    Random(f"ameba-genome-fixed-{role}-v1").shuffle(table)
    return table


def _table(layout: str, symbols: int, role: str, width: int) -> list[int] | None:
    """Number -> symbol for one role, or None where the number is the symbol.

    A context layout gets the plain runs table: its transform needs the loci
    before it, so it is applied while a genome is read, not here.
    """
    name, _, argument = layout.partition(":")
    if name == "direct":
        return None
    if name == "runs" or name in _CONTEXT:
        return _runs(symbols, width)
    if name == "codon":
        silent_stop = role == "kind"
        return [
            SILENT if amino == "*" and silent_stop else
            (_AMINO.index(amino) if amino != "*" else len(_AMINO)) % symbols
            for amino in GENETIC_CODE
        ]
    if name in ("diploid", "codominant"):
        alleles = int(argument)
        runs = _runs(symbols, alleles)
        pick = max if name == "diploid" else (lambda a, b: (a + b) // 2)
        return [runs[pick(n // alleles, n % alleles)] for n in range(alleles * alleles)]
    if name == "majority":
        base = int(argument)
        runs = _runs(symbols, base)
        table = []
        for n in range(base ** 3):
            a, b, c = n // (base * base), n // base % base, n % base
            table.append(runs[b if b == c else a])
        return table
    if name == "golden":
        runs = _runs(symbols, width)
        return [runs[int((n * 0.6180339887498949) % 1.0 * width)] for n in range(width)]
    if name == "epigenetic":
        return _runs(symbols, width) + [SILENT] * width
    if name == "shuffle":
        return _shuffled(symbols, role, width)
    if name == "fine":
        return list(range(FINE_WEIGHT_LEVELS))
    if name == "log":
        return _runs(LOG_WEIGHT_LEVELS, width)
    if name in _MANY_TO_ONE:
        return [_many_to_one(name, n) % symbols for n in range(width)]
    runs = _runs(symbols, width)
    return [runs[_transformed(name, argument, n, width)] for n in range(width)]


class FixedEncoding:
    def __init__(self, grammar: Grammar, scheme: str = "base", reach: int = REACH,
                 config: FixedConfig | None = None) -> None:
        if config is None:
            if scheme not in SCHEMES:
                raise ValueError(f"Unknown scheme {scheme!r}; expected one of {', '.join(SCHEMES)}")
            config = FixedConfig(layouts=dict(SCHEMES[scheme]), reach=reach)
        if config.output_sources == 2 and "add" not in grammar.kinds:
            raise ValueError("Summing outputs need the add kind")
        self.grammar = grammar
        self.config = config
        self.scheme = scheme
        self.reach = config.reach
        self.layouts = config.layouts
        tagged = config.addressing == "tag"
        self.node_roles = ("kind", "parameter") + (("tag",) if tagged else ()) + ("source", "weight") * config.sources
        self.node_width = len(self.node_roles)
        self.lead = 3 if tagged else 2
        self.output_roles = ("source", "weight") * config.output_sources
        sources = grammar.inputs + {
            "relative": 2 * self.reach, "absolute": ABSOLUTE_SLOTS, "tag": TAG_SPACE,
        }[config.addressing]
        self.symbols = {
            "kind": len(grammar.kinds),
            "parameter": PARAMETER_SLOTS,
            "source": sources,
            "weight": len(WEIGHTS),
            "tag": TAG_SPACE,
        }
        #: role -> table from number to symbol, or None where the number is the symbol.
        self.tables: dict[str, list[int] | None] = {}
        for role, layout in self.layouts.items():
            self.tables[role] = _table(layout, self.symbols[role], role, config.width)
        self.context = {
            role: layout.partition(":")[0]
            for role, layout in self.layouts.items() if layout.partition(":")[0] in _CONTEXT
        }
        self.preimages: dict[str, dict[int, list[int]]] = {}
        for role, table in self.tables.items():
            if table is not None:
                inverse: dict[int, list[int]] = {}
                for number, symbol in enumerate(table):
                    inverse.setdefault(symbol, []).append(number)
                self.preimages[role] = inverse
        self.MUTATION_WEIGHTS = dict(config.mutation)

    # --- layout ------------------------------------------------------------

    def size(self, role: str) -> int:
        table = self.tables[role]
        return self.symbols[role] if table is None else len(table)

    def symbol(self, role: str, number: int) -> int:
        table = self.tables[role]
        return number if table is None else table[number]

    def number_for(self, role: str, symbol: int, rng: Random) -> int:
        """Some number that reads as ``symbol``; any synonym, chosen at random."""
        if self.tables[role] is None:
            return symbol
        if self.layouts[role] == "fine":
            return rng.randrange(FINE_WEIGHT_LEVELS)
        preimages = self.preimages[role].get(symbol)
        if preimages is None:  # a many-to-one layout can leave a symbol unreachable
            return rng.randrange(len(self.tables[role]))
        return rng.choice(preimages)

    def tail(self) -> int:
        return len(self.output_roles) * self.grammar.outputs

    def node_count(self, genome: Genome) -> int:
        return (len(genome) - self.tail()) // self.node_width

    def role_at(self, genome: Genome, position: int) -> str:
        body = len(genome) - self.tail()
        if position < body:
            return self.node_roles[position % self.node_width]
        return self.output_roles[(position - body) % len(self.output_roles)]

    # --- decoding ----------------------------------------------------------

    def _source(self, number: int, position: int = 0, output: bool = False) -> list[int]:
        """The connection gene for a source number, as a list (empty: none).

        ``position`` is the record's own node index -- or, for an output, how
        many nodes precede it -- which absolute addressing needs to turn a node
        index into the relative reference the dictionary speaks.
        """
        return self._source_code(self.symbol("source", number), position, output)

    def _source_code(self, code: int, position: int = 0, output: bool = False,
                     tags: list[int] | None = None, kind: str | None = None) -> list[int]:
        inputs = self.grammar.inputs
        if self.config.addressing == "tag" and code >= inputs:
            wanted = code - inputs
            # Bind to the nearest tag among earlier nodes -- any node, for a
            # delay, since only through a delay may an edge run forward.
            candidates = [j for j in range(len(tags or ())) if j != position
                          and (j < position or kind == "delay" or output)]
            if output:
                candidates = list(range(min(position, len(tags or ()))))
            if not candidates:
                return []
            target = min(candidates, key=lambda j: (abs(tags[j] - wanted), abs(position - j)))
            if output:
                offset = target - position
            else:
                offset = target - position
            if offset == 0 or abs(offset) > MAX_OFFSET:
                return []
            return [source_gene(offset)]
        if code < inputs:
            return [input_gene(code)]
        if self.config.addressing == "relative":
            if code < inputs + self.reach:
                return [source_gene(-(code - inputs + 1))]
            return [source_gene(code - inputs - self.reach + 1)]
        target = code - inputs
        if output:
            # From an output, "+1" is the node at its own position.
            offset = target - position + 1 if target >= position else target - position
        else:
            offset = target - position
        if offset == 0 or abs(offset) > MAX_OFFSET:
            return []
        return [source_gene(offset)]

    def _weight(self, number: int) -> int:
        return self._weight_symbol(self.symbol("weight", number))

    def _weight_symbol(self, symbol: int) -> int:
        layout = self.layouts["weight"]
        if layout == "fine":
            return fine_weight_gene(symbol)
        if layout == "log":
            return log_weight_gene(symbol)
        return weight_gene(WEIGHTS[symbol])

    def symbols_of(self, genome: Genome) -> list[int]:
        """The symbol every locus reads as, context layouts included."""
        out: list[int] = []
        running: dict[str, int] = {}
        previous: dict[str, int] = {}
        width = self.config.width
        for position, number in enumerate(genome):
            role = self.role_at(genome, position)
            table = self.tables[role]
            layout = self.context.get(role)
            if layout == "cumulative":
                value = (running.get(role, 0) + number) % width
                running[role] = value
                out.append(table[value])
            elif layout == "difference":
                value = (number - previous.get(role, 0)) % width
                previous[role] = number
                out.append(table[value])
            elif layout is not None:  # gradient:C
                step = int(self.layouts[role].partition(":")[2])
                record = min(position // self.node_width, self.node_count(genome))
                out.append(table[(number + step * record) % width])
            else:
                out.append(number if table is None else table[number])
        return out

    @staticmethod
    def _pairs(numbers) -> list[tuple[int, int]]:
        return [(numbers[i], numbers[i + 1]) for i in range(0, len(numbers), 2)]

    def translate(self, genome: Genome) -> tuple[int, ...]:
        """The canonical gene sequence the ordinary decoder reads."""
        genes: list[int] = []
        width = self.node_width
        count = self.node_count(genome)
        symbols = self.symbols_of(genome)
        lead = self.lead
        tags = [symbols[i * width + 2] for i in range(count)] if lead == 3 else None
        silent_kind = "add" if "add" in self.grammar.kinds else self.grammar.kinds[0]
        for index in range(count):
            record = symbols[index * width:(index + 1) * width]
            if record[0] == SILENT:
                # A silenced node keeps its place, so no reference shifts,
                # but has no sources and is therefore never expressed.
                genes.append(node_gene(silent_kind))
                continue
            kind = self.grammar.kinds[record[0]]
            genes.append(node_gene(kind))
            for name in KIND_PARAMETERS.get(kind, {}):
                ladder = PARAMETER_LADDERS.get(name)
                if ladder:
                    genes.append(ladder[record[1] % len(ladder)])
            maximum = OPERATOR_ARITY[kind][1]
            slots = self.config.sources if maximum is None else min(self.config.sources, maximum)
            for source, weight in self._pairs(record[lead:])[:slots]:
                reference = self._source_code(source, index, tags=tags, kind=kind)
                if reference:
                    genes += reference + [self._weight_symbol(weight)]
        body = len(genome) - self.tail()
        stride = len(self.output_roles)
        outputs = self.grammar.outputs
        if self.config.output_sources == 1:
            for channel in range(outputs):
                source, weight = symbols[body + channel * stride: body + (channel + 1) * stride]
                genes += [output_gene(channel), *self._source_code(source, count, output=True, tags=tags),
                          self._weight_symbol(weight)]
            return tuple(genes)
        # Each output sums two sources through an add node of its own, placed
        # after every genome node, so no genome reference shifts.
        for channel in range(outputs):
            numbers = symbols[body + channel * stride: body + (channel + 1) * stride]
            genes.append(node_gene("add"))
            for source, weight in self._pairs(numbers):
                reference = self._source_code(source, count + channel, tags=tags)
                if reference:
                    genes += reference + [self._weight_symbol(weight)]
        for channel in range(outputs):
            genes += [output_gene(channel), source_gene(-(outputs - channel))]
        return tuple(genes)

    # --- creation ----------------------------------------------------------

    def _draw_source(self, rng: Random, kind: str | None, position: int) -> int:
        """A source symbol that usually chains to a near earlier node.

        Only a delay reaches ahead, because only through a delay is a forward
        edge legal; chaining keeps a fresh genome from being pruned to nothing.
        """
        inputs = self.grammar.inputs
        draw = rng.random()
        if self.config.addressing == "tag":
            if draw < 0.8:
                return inputs + rng.randrange(TAG_SPACE)
            return rng.randrange(inputs)
        if self.config.addressing == "relative":
            if kind == "delay" and draw < 0.3:
                return inputs + self.reach + min(self.reach - 1, int(rng.expovariate(1.0)))
            if draw < 0.8:
                return inputs + min(self.reach - 1, int(rng.expovariate(1.0)))
            return rng.randrange(inputs)
        if kind == "delay" and draw < 0.3:
            return inputs + min(ABSOLUTE_SLOTS - 1, position + 1 + int(rng.expovariate(1.0)))
        if draw < 0.8 and position > 0:
            return inputs + max(0, position - 1 - int(rng.expovariate(1.0)))
        return rng.randrange(inputs)

    def random_record(self, rng: Random, position: int = 0) -> list[int]:
        kind_symbol = rng.randrange(self.symbols["kind"])
        kind = self.grammar.kinds[kind_symbol]
        record = [
            self.number_for("kind", kind_symbol, rng),
            rng.randrange(self.size("parameter")),
        ]
        if self.lead == 3:
            record.append(rng.randrange(self.size("tag")))
        for _ in range(self.config.sources):
            code = self._draw_source(rng, kind, position)
            record += [self.number_for("source", code, rng), rng.randrange(self.size("weight"))]
        return record

    def random_genome(self, rng: Random, nodes: tuple[int, int] | None = None) -> Genome:
        count = rng.randint(*(nodes or self.config.init_nodes))
        genes: list[int] = []
        for index in range(count):
            genes += self.random_record(rng, index)
        inputs = self.grammar.inputs
        for _ in range(self.grammar.outputs):
            for _ in range(self.config.output_sources):
                if self.config.addressing == "tag":
                    code = inputs + rng.randrange(TAG_SPACE)
                elif self.config.addressing == "relative":
                    code = inputs + rng.randrange(3)
                elif count:
                    code = inputs + max(0, count - 1 - rng.randrange(3))
                else:
                    code = rng.randrange(inputs)
                genes += [self.number_for("source", code, rng), rng.randrange(self.size("weight"))]
        if self.context:
            genes = self._contextualize(genes)
        return tuple(genes)

    def _contextualize(self, values: list[int]) -> list[int]:
        """Rewrite intended per-locus values for the context layouts.

        Generation draws each locus as if it were read on its own; under a
        cumulative or difference layout it is read against the loci before
        it, so the numbers are rewritten to make each locus read as drawn.
        """
        out = list(values)
        running: dict[str, int] = {}
        previous: dict[str, int] = {}
        width = self.config.width
        genome = tuple(values)
        for position, value in enumerate(values):
            role = self.role_at(genome, position)
            layout = self.context.get(role)
            if layout == "cumulative":
                out[position] = (value - running.get(role, 0)) % width
                running[role] = value
            elif layout == "difference":
                out[position] = (value + previous.get(role, 0)) % width
                previous[role] = out[position]
            elif layout == "gradient":
                step = int(self.layouts[role].partition(":")[2])
                record = min(position // self.node_width, self.node_count(genome))
                out[position] = (value - step * record) % width
        return out

    # --- variation ---------------------------------------------------------

    def _step(self, rng: Random) -> int:
        kind, _, value = self.config.step.partition(":")
        if kind == "unit":
            return rng.choice((-1, 1))
        if kind == "geometric":
            size = 1
            while rng.random() < float(value):
                size += 1
        else:
            size = rng.randint(1, int(value))
        return size if rng.random() < 0.5 else -size

    def _position(self, genome: Genome, rng: Random) -> int:
        rates = self.config.role_rates
        if rates is None:
            return rng.randrange(len(genome))
        weights = [rates.get(self.role_at(genome, i), 0.0) for i in range(len(genome))]
        if not any(weights):
            return rng.randrange(len(genome))
        return rng.choices(range(len(genome)), weights)[0]

    def creep(self, genome: Genome, rng: Random) -> Genome | None:
        position = self._position(genome, rng)
        size = self.size(self.role_at(genome, position))
        value = min(max(genome[position] + self._step(rng), 0), size - 1)
        return genome[:position] + (value,) + genome[position + 1:]

    def bitflip(self, genome: Genome, rng: Random) -> Genome | None:
        """Flip one bit of one locus: the locus as a Boolean field."""
        position = self._position(genome, rng)
        size = self.size(self.role_at(genome, position))
        bits = max(1, (size - 1).bit_length())
        value = genome[position] ^ (1 << rng.randrange(bits))
        if value >= size:
            value %= size
        return genome[:position] + (value,) + genome[position + 1:]

    def base(self, genome: Genome, rng: Random) -> Genome | None:
        """Change one base of a codon locus; transitions twice as likely.

        A locus under any other layout is treated as base-4 digits all the
        same, so the mutation stays defined everywhere.
        """
        position = self._position(genome, rng)
        size = self.size(self.role_at(genome, position))
        digits = max(1, math.ceil(math.log(size, 4)))
        digit = rng.randrange(digits)
        value = genome[position]
        old = value // 4 ** digit % 4
        if rng.random() < 2 / 3:
            new = old ^ 1  # T<->C, A<->G: a transition
        else:
            new = rng.choice([b for b in range(4) if b not in (old, old ^ 1)])
        value += (new - old) * 4 ** digit
        if not 0 <= value < size:
            value %= size
        return genome[:position] + (value,) + genome[position + 1:]

    def duplicate_record(self, genome: Genome, rng: Random) -> Genome | None:
        records = self._records(genome)
        if not records:
            return None
        copy = rng.choice(records)
        at = rng.randint(0, len(records)) * self.node_width
        return genome[:at] + copy + genome[at:]

    def transpose(self, genome: Genome, rng: Random) -> Genome | None:
        records = self._records(genome)
        if len(records) < 2:
            return None
        moved = records.pop(rng.randrange(len(records)))
        records.insert(rng.randint(0, len(records)), moved)
        return tuple(g for record in records for g in record) + self._outputs(genome)

    def invert(self, genome: Genome, rng: Random) -> Genome | None:
        records = self._records(genome)
        if len(records) < 2:
            return None
        a, b = sorted(rng.sample(range(len(records) + 1), 2))
        records[a:b] = records[a:b][::-1]
        return tuple(g for record in records for g in record) + self._outputs(genome)

    def burst(self, genome: Genome, rng: Random) -> Genome | None:
        for _ in range(rng.randint(3, 8)):
            genome = (self.creep if rng.random() < 0.5 else self.reset)(genome, rng)
        return genome

    def reset(self, genome: Genome, rng: Random) -> Genome | None:
        position = self._position(genome, rng)
        value = rng.randrange(self.size(self.role_at(genome, position)))
        return genome[:position] + (value,) + genome[position + 1:]

    def insert_record(self, genome: Genome, rng: Random) -> Genome | None:
        index = rng.randint(0, self.node_count(genome))
        at = index * self.node_width
        return genome[:at] + tuple(self.random_record(rng, index)) + genome[at:]

    def remove_record(self, genome: Genome, rng: Random) -> Genome | None:
        count = self.node_count(genome)
        if count == 0:
            return None
        at = rng.randrange(count) * self.node_width
        return genome[:at] + genome[at + self.node_width:]

    def mutate(self, genome: Genome, rng: Random, attempts: int = 8) -> Genome:
        names = [name for name, weight in self.MUTATION_WEIGHTS.items() if weight > 0]
        for _ in range(attempts):
            name = rng.choices(names, [self.MUTATION_WEIGHTS[n] for n in names])[0]
            child = getattr(self, name)(genome, rng)
            if child is not None and child != genome:
                return child
        return genome

    # --- crossover -----------------------------------------------------------
    #
    # Records are aligned by position, so beyond cutting and splicing a child
    # can also be assembled record by record, or locus by locus, from the two
    # parents -- which a free sequence, whose records do not line up, cannot.

    def _records(self, genome: Genome) -> list[Genome]:
        w = self.node_width
        return [genome[i * w:(i + 1) * w] for i in range(self.node_count(genome))]

    def _outputs(self, genome: Genome) -> Genome:
        return genome[len(genome) - self.tail():]

    def crossover(self, left: Genome, right: Genome, rng: Random) -> Genome:
        """One-point at node-record boundaries; the outputs come from ``left``.

        The cut is independent in each parent, so the child's length varies.
        """
        head = rng.randint(0, self.node_count(left)) * self.node_width
        tail = rng.randint(0, self.node_count(right)) * self.node_width
        right_body = len(right) - self.tail()
        return left[:head] + right[tail:right_body] + self._outputs(left)

    def two_point(self, left: Genome, right: Genome, rng: Random) -> Genome:
        """A block of ``right``'s records replaces a block of ``left``'s.

        Both blocks are chosen independently, so the child's length varies; an
        empty block makes this a pure insertion or a pure deletion.
        """
        w = self.node_width
        a, b = sorted(rng.randint(0, self.node_count(left)) for _ in range(2))
        c, d = sorted(rng.randint(0, self.node_count(right)) for _ in range(2))
        return (
            left[:a * w] + right[c * w:d * w]
            + left[b * w:len(left) - self.tail()] + self._outputs(left)
        )

    def uniform_record(self, left: Genome, right: Genome, rng: Random) -> Genome:
        """Each aligned record from either parent; length from a random parent."""
        ours, theirs = self._records(left), self._records(right)
        length = len(rng.choice((ours, theirs)))
        records = []
        for index in range(length):
            options = [item[index] for item in (ours, theirs) if index < len(item)]
            records.append(rng.choice(options))
        outputs = rng.choice((self._outputs(left), self._outputs(right)))
        return tuple(gene for record in records for gene in record) + outputs

    def uniform_locus(self, left: Genome, right: Genome, rng: Random) -> Genome:
        """Each aligned locus from either parent; length from a random parent."""
        ours, theirs = self._records(left), self._records(right)
        length = len(rng.choice((ours, theirs)))
        genes: list[int] = []
        for index in range(length):
            if index < len(ours) and index < len(theirs):
                genes += [rng.choice(pair) for pair in zip(ours[index], theirs[index])]
            else:
                genes += (ours if index < len(ours) else theirs)[index]
        genes += [rng.choice(pair) for pair in zip(self._outputs(left), self._outputs(right))]
        return tuple(genes)

    def transfer(self, left: Genome, right: Genome, rng: Random) -> Genome:
        """Horizontal gene transfer: one record of ``right`` inserted into ``left``."""
        donors = self._records(right)
        if not donors:
            return left
        at = rng.randint(0, self.node_count(left)) * self.node_width
        return left[:at] + rng.choice(donors) + left[at:]

    CROSSOVERS = ("crossover", "two_point", "uniform_record", "uniform_locus", "transfer")
