"""Endosymbiotic state-space encoding: two chromosomes in one genome.

A dynamic model splits into two parts, as in state-space form:

    x(k+1) = f( x(k), u(k), y(k) )      the state chromosome
    y(k)   = h( x(k), u(k) )            the output chromosome

The two are inherited together and judged by one score -- two genomes in one
organism, as a mitochondrion's is within its host cell.

**State records** compute a new state from values of the *previous* step: the
inputs, any state (itself included), and any model output. Every state is a
delay, so any connection between states is a legal feedback loop, and feeding
an output back is a single source -- ``y(k-1)`` is just "output 0".

**Output records** compute from values of the *current* step: the inputs, the
states, and output records before them. They never read a later output record,
so the instantaneous part has no loop, and an algebraic loop cannot be written
down at all -- by construction, not by checking.

Records have the fixed-role shape of the A encoding, read directly (the
``A-base`` alphabets), so a comparison with ``A-base`` changes only the graph
the records build:

    genome   [states, state records..., output records..., terminals...]
    record   [kind, parameter, source1, weight1, source2, weight2]
    terminal [source, weight]                      one per output channel

The leading number says how many state records there are; it is not a locus,
so no point mutation touches it, and record insertion and removal keep it
right. Mutations and crossover are those of ``A-base`` applied within each
chromosome, plus ``organelle``: the state chromosome of one parent with the
output chromosome of the other, as an organelle passing between cells.

:class:`StateSpaceConfig` narrows the split further: which node kinds each
chromosome may hold -- an output chromosome of memoryless kinds only, so all
dynamics live in the states, and states whose memory is only delay, integral
and derivative -- and whether each output sums two sources.

The genome is built straight into a signal graph rather than into dictionary
genes, because the dictionary's relative addressing cannot say "a state read
from the previous step". Each state record becomes an operator node feeding a
one-step delay; readers of the state read the delay.
"""

from __future__ import annotations

from dataclasses import dataclass
from random import Random

from ameba_graph import Edge, Graph, Node
from ameba_signal import interface_scaffold
from ameba_signal.operators import OPERATOR_ARITY
from ameba_signal.stateful import STATEFUL_KINDS

from .decoding import Grammar
from .dictionary import DEFAULT_WEIGHT, GENES, KIND_PARAMETERS, PARAMETER_LADDERS, WEIGHTS

Genome = tuple[int, ...]

WIDTH = 6
TERMINAL = 2
#: How many states a source can name, and how far back an output record reaches.
STATE_SLOTS = 16
OUTPUT_REACH = 8
PARAMETER_SLOTS = 8
MUTATION_WEIGHTS = {"creep": 0.5, "reset": 0.3, "insert_record": 0.1, "remove_record": 0.1}

#: Memory kinds a ``memory3`` chromosome keeps: no low- or high-pass filters.
MEMORY3 = ("delay", "integral", "derivative")


@dataclass
class StateSpaceConfig:
    """Which kinds each chromosome holds, and how many sources an output sums.

    A kind set is ``all`` (every kind the grammar allows), ``static`` (the
    memoryless ones), ``memory3`` (memoryless plus delay, integral and
    derivative), or an explicit list of kinds.
    """

    state_kinds: str | tuple[str, ...] = "all"
    output_kinds: str | tuple[str, ...] = "all"
    output_sources: int = 1
    #: Where inputs sit in a source alphabet. ``inputs-first`` is the original
    #: coding: inputs, then states. An extra input then shifts every state's
    #: code, so a +-1 step from u0 lands on the new input instead of x0, and
    #: fresh genomes spread their input references over every input -- which
    #: measurably hurt the search even when the extra input was all zeros.
    #: ``inputs-last`` puts states and outputs at fixed codes and the inputs
    #: after them, and fresh genomes read only the plant's own inputs, so an
    #: extra input changes nothing but the size of the alphabet.
    source_layout: str = "inputs-first"
    #: How a child inherits the two chromosomes. ``mixed`` recombines both.
    #: ``maternal`` treats them as a cell's two genomes: the nuclear one (the
    #: output chromosome) recombines, the mitochondrial one (the states) comes
    #: whole from the first parent, the mother, as mitochondrial DNA does.
    inheritance: str = "mixed"
    #: Point-mutation rate of a state locus relative to an output locus. A
    #: property of the molecule, not of what a gene means: mitochondrial DNA
    #: mutates several times faster than nuclear DNA.
    state_rate: float = 1.0
    #: Scales how often whole records are inserted or removed.
    structural_rate: float = 1.0
    #: Weight of gene duplication, next to the point mutations' 0.8 and the
    #: structural 0.2: a record copied right after itself, free to diverge.
    duplication: float = 0.0
    #: Synonymous codes: every value has ``redundancy`` consecutive numbers, as
    #: several codons name one amino acid, so many +-1 steps change nothing.
    redundancy: int = 1
    #: Context-dependent mutability: a locus equal to a neighbour is
    #: ``1 + hotspot`` times as likely to mutate per such neighbour, as repeats
    #: slip in DNA. Blind to what a gene means; selection can still learn to
    #: keep what matters out of repeats.
    hotspot: float = 0.0
    #: How ``reset`` draws a new source. ``uniform``: any code alike, so every
    #: extra input takes a share of all source resets -- six filter outputs
    #: took a quarter, from states too, and hurt the search as much as six
    #: zero inputs. ``pooled``: all inputs share the one slot a single input
    #: has, drawn among them alike, so extra inputs dilute only each other;
    #: with one input it is ``uniform`` exactly. ``near``: a step away from the
    #: current code, 1 with probability 1/2, 2 with 1/4, ..., as point
    #: mutations are usually small.
    source_reset: str = "uniform"

    def __post_init__(self) -> None:
        if self.output_sources not in (1, 2):
            raise ValueError("output_sources must be 1 or 2")
        if self.source_layout not in ("inputs-first", "inputs-last"):
            raise ValueError("source_layout must be inputs-first or inputs-last")
        if self.inheritance not in ("mixed", "maternal"):
            raise ValueError("inheritance must be mixed or maternal")
        if self.state_rate <= 0:
            raise ValueError("state_rate must be positive")
        if self.structural_rate < 0 or self.duplication < 0 or self.hotspot < 0:
            raise ValueError("structural_rate, duplication and hotspot must not be negative")
        if self.redundancy < 1:
            raise ValueError("redundancy must be at least 1")
        if self.source_reset not in ("uniform", "pooled", "near"):
            raise ValueError("source_reset must be uniform, pooled or near")

    @classmethod
    def from_dict(cls, data: dict) -> "StateSpaceConfig":
        return cls(**data)


def _kind_set(spec, grammar: Grammar) -> tuple[str, ...]:
    if spec == "all":
        kinds = grammar.kinds
    elif spec == "static":
        kinds = tuple(kind for kind in grammar.kinds if kind not in STATEFUL_KINDS)
    elif spec == "memory3":
        kinds = tuple(kind for kind in grammar.kinds if kind not in STATEFUL_KINDS or kind in MEMORY3)
    else:
        unknown = set(spec) - set(grammar.kinds)
        if unknown:
            raise ValueError(f"Kinds not in the grammar: {', '.join(sorted(unknown))}")
        kinds = tuple(kind for kind in grammar.kinds if kind in spec)
    if not kinds:
        raise ValueError(f"Kind set {spec!r} is empty")
    return kinds


class StateSpaceEncoding:
    def __init__(self, grammar: Grammar, init_states: tuple[int, int] = (1, 5),
                 init_outputs: tuple[int, int] = (2, 8), config: StateSpaceConfig | None = None,
                 primary_inputs: int | None = None) -> None:
        self.config = config or StateSpaceConfig()
        #: The plant's own inputs; any after them are auxiliary (a filter
        #: bank's outputs, say). Only the ``inputs-last`` layout uses this.
        self.primary = primary_inputs or grammar.inputs
        if not 1 <= self.primary <= grammar.inputs:
            raise ValueError("primary_inputs must be between 1 and the number of inputs")
        # A summing output's add node is part of the encoding's structure, not
        # a searched kind, so it is built whatever kinds the grammar allows.
        self.kinds = {
            "state": _kind_set(self.config.state_kinds, grammar),
            "output": _kind_set(self.config.output_kinds, grammar),
        }
        self.terminal = TERMINAL * self.config.output_sources
        self.grammar = grammar
        self.init_states = init_states
        self.init_outputs = init_outputs
        inputs, outputs = grammar.inputs, grammar.outputs
        #: Source alphabets. A state reads inputs, states, and model outputs; an
        #: output record reads inputs, states, and earlier output records; a
        #: terminal reads inputs, states, and output records counted from the end.
        self.state_sources = inputs + STATE_SLOTS + outputs
        self.output_sources = inputs + STATE_SLOTS + OUTPUT_REACH
        self.sizes = {"kind": len(grammar.kinds), "parameter": PARAMETER_SLOTS, "weight": len(WEIGHTS)}

    # --- layout ------------------------------------------------------------

    #: Numbers per record; the codon encoding reads records of this width.
    width = WIDTH

    def record_sizes(self, chromosome: str) -> tuple[int, ...]:
        """Alphabet size of each field of a record, ignoring redundancy."""
        sources = self.state_sources if chromosome == "state" else self.output_sources
        return (len(self.kinds[chromosome]), PARAMETER_SLOTS, sources, len(WEIGHTS), sources, len(WEIGHTS))

    def terminal_sizes(self) -> tuple[int, ...]:
        return (self.output_sources, len(WEIGHTS)) * (self.tail() // TERMINAL)

    def split(self, genome: Genome) -> tuple[int, int]:
        """(state records, output records)."""
        states = genome[0]
        records = (len(genome) - 1 - self.tail()) // WIDTH
        return states, records - states

    def tail(self) -> int:
        return self.terminal * self.grammar.outputs

    def role_at(self, genome: Genome, position: int) -> tuple[str, str]:
        """(chromosome, role) of a locus; position 0 is the header."""
        states, outputs = self.split(genome)
        body = 1 + WIDTH * (states + outputs)
        if position >= body:
            return "terminal", ("source", "weight")[(position - body) % TERMINAL]
        record = (position - 1) // WIDTH
        role = ("kind", "parameter", "source", "weight", "source", "weight")[(position - 1) % WIDTH]
        return ("state" if record < states else "output"), role

    def size_at(self, genome: Genome, position: int) -> int:
        return self._meanings_at(genome, position) * self.config.redundancy

    def _meanings_at(self, genome: Genome, position: int) -> int:
        chromosome, role = self.role_at(genome, position)
        if role == "kind":
            return len(self.kinds[chromosome])
        if role != "source":
            return self.sizes[role]
        return self.state_sources if chromosome == "state" else self.output_sources

    # --- building -------------------------------------------------------------

    # --- source layouts -----------------------------------------------------------
    #
    # Decoding is written once, for the inputs-first coding. The inputs-last
    # coding is translated into it before decoding and out of it after creating
    # a record, so both share one tested decoder. The two alphabets have the
    # same size; only the order of their parts differs.

    def _span(self, chromosome: str) -> int:
        """Codes before the inputs in the inputs-last alphabet of a chromosome."""
        return STATE_SLOTS + (self.grammar.outputs if chromosome == "state" else OUTPUT_REACH)

    def _to_first(self, code: int, chromosome: str) -> int:
        span = self._span(chromosome)
        return code + self.grammar.inputs if code < span else code - span

    def _to_last(self, code: int, chromosome: str) -> int:
        inputs = self.grammar.inputs
        return code + self._span(chromosome) if code < inputs else code - inputs

    def _canonical(self, genome: Genome) -> Genome:
        """The genome in the inputs-first coding the decoder reads."""
        r = self.config.redundancy
        if r > 1:
            genome = genome[:1] + tuple(value // r for value in genome[1:])
        if self.config.source_layout == "inputs-first":
            return genome
        out = list(genome)
        for position in range(1, len(genome)):
            chromosome, role = self.role_at(genome, position)
            if role == "source":
                out[position] = self._to_first(genome[position], "state" if chromosome == "state" else "output")
        return tuple(out)

    def build(self, genome: Genome) -> Graph:
        """The signal graph a genome encodes. Always valid; same genome, same graph."""
        genome = self._canonical(genome)
        grammar = self.grammar
        inputs, channels = grammar.inputs, grammar.outputs
        states, outputs = self.split(genome)
        records = [genome[1 + i * WIDTH: 1 + (i + 1) * WIDTH] for i in range(states + outputs)]
        terminals = genome[1 + WIDTH * (states + outputs):]

        def node_of(index: int) -> str:
            return f"p{index}" if index < states else f"h{index - states}"

        # Resolve every reference to ("u", c), ("x", s) for a state, ("h", j) for
        # an output record, or ("y", c) for a model output -- None if it names
        # nothing.
        def state_ref(code):
            if code < inputs:
                return ("u", code)
            code -= inputs
            if code < STATE_SLOTS:
                return ("x", code) if code < states else None
            return ("y", code - STATE_SLOTS)

        def output_ref(code, position):
            if code < inputs:
                return ("u", code)
            code -= inputs
            if code < STATE_SLOTS:
                return ("x", code) if code < states else None
            target = position - (code - STATE_SLOTS + 1)
            return ("h", target) if 0 <= target < position else None

        kinds = [self.kinds["state" if i < states else "output"][record[0]] for i, record in enumerate(records)]
        wanted: list[list[tuple[tuple, float]]] = []
        for index, record in enumerate(records):
            maximum = OPERATOR_ARITY[kinds[index]][1]
            slots = 2 if maximum is None else min(2, maximum)
            refs = []
            for source, weight in ((record[2], record[3]), (record[4], record[5]))[:slots]:
                ref = state_ref(source) if index < states else output_ref(source, index - states)
                if ref is not None and ref not in [r for r, _ in refs]:
                    refs.append((ref, WEIGHTS[weight]))
            wanted.append(refs)

        def terminal_ref(source):
            if source < inputs:
                return ("u", source)
            if source - inputs < STATE_SLOTS:
                s = source - inputs
                return ("x", s) if s < states else None
            j = outputs - (source - inputs - STATE_SLOTS + 1)
            return ("h", j) if 0 <= j < outputs else None

        #: per channel, the (reference, weight) pairs its terminal names
        terminal_refs = []
        for channel in range(channels):
            numbers = terminals[channel * self.terminal: (channel + 1) * self.terminal]
            pairs = [(terminal_ref(numbers[i]), WEIGHTS[numbers[i + 1]]) for i in range(0, len(numbers), 2)]
            terminal_refs.append([(ref, weight) for ref, weight in pairs if ref is not None])

        # Liveness to a fixed point: a record short of its minimum arity is dead,
        # and so is anything that reads it, a state through its delay, a model
        # output through the terminal that feeds it.
        alive = [True] * len(records)

        def live(ref) -> bool:
            kind, i = ref
            if kind == "u":
                return True
            if kind == "x":
                return alive[i]
            if kind == "h":
                return alive[states + i]
            return any(live(ref) for ref, _ in terminal_refs[i])

        def resolve(ref):
            """The graph node a reference reads."""
            kind, i = ref
            if kind == "u":
                return f"u{i}"
            if kind == "x":
                return f"x{i}"
            if kind == "h":
                return node_of(states + i)
            return feed_of(i)

        def feed_of(channel):
            """The node an output channel reads: its sum node, its one source, or the default."""
            live_refs = [ref for ref, _ in terminal_refs[channel] if live(ref)]
            if not live_refs:
                return f"u{min(channel, inputs - 1)}"
            if self.config.output_sources == 2:
                return f"s{channel}"
            return resolve(live_refs[0])

        # Two references can land on one node -- "output 0" and the state that
        # feeds it, say -- so sources are deduplicated after resolution, which
        # can leave a node short of its arity; repeat until nothing changes.
        changed = True
        while changed:
            changed = False
            sources_of = {}
            for index, refs in enumerate(wanted):
                if not alive[index]:
                    continue
                resolved: list[tuple[str, float]] = []
                for ref, weight in refs:
                    if live(ref):
                        name = resolve(ref)
                        if name not in [n for n, _ in resolved]:
                            resolved.append((name, weight))
                minimum, maximum = OPERATOR_ARITY[kinds[index]]
                if maximum is not None:
                    resolved = resolved[:maximum]
                if len(resolved) < minimum:
                    alive[index] = False
                    changed = True
                    break
                sources_of[index] = resolved
        # Each output reads its feed; with summing outputs the feed is an add
        # node over the terminal's live sources.
        sums: dict[int, list[tuple[str, float]]] = {}
        output_sources = []
        for channel, refs in enumerate(terminal_refs):
            live_refs = [(ref, weight) for ref, weight in refs if live(ref)]
            if not live_refs:
                output_sources.append((f"u{min(channel, inputs - 1)}", DEFAULT_WEIGHT))
            elif self.config.output_sources == 2:
                resolved: list[tuple[str, float]] = []
                for ref, weight in live_refs:
                    name = resolve(ref)
                    if name not in [n for n, _ in resolved]:
                        resolved.append((name, weight))
                sums[channel] = resolved
                output_sources.append((f"s{channel}", DEFAULT_WEIGHT))
            else:
                output_sources.append((resolve(live_refs[0][0]), live_refs[0][1]))

        # Keep only what an output can reach, following edges backwards
        # through state delays.
        reached: set[str] = set()
        pending = [source for source, _ in output_sources]
        while pending:
            name = pending.pop()
            if name in reached:
                continue
            reached.add(name)
            if name.startswith("x"):
                pending.append(f"p{name[1:]}")
            elif name.startswith("s"):
                pending.extend(source for source, _ in sums.get(int(name[1:]), []))
            elif name[0] in "ph":
                index = int(name[1:]) + (states if name[0] == "h" else 0)
                pending.extend(source for source, _ in sources_of.get(index, []))

        graph = interface_scaffold(inputs, channels)
        for index, record in enumerate(records):
            name = node_of(index)
            if name not in reached or not alive[index]:
                continue
            attributes = dict(KIND_PARAMETERS.get(kinds[index], {}))
            for parameter in KIND_PARAMETERS.get(kinds[index], {}):
                ladder = PARAMETER_LADDERS.get(parameter)
                if ladder:
                    attributes[parameter] = GENES[ladder[record[1] % len(ladder)]].value
            graph.add_node(Node(name, kinds[index], attributes))
            if index < states and f"x{index}" in reached:
                graph.add_node(Node(f"x{index}", "delay", {"steps": 1, "initial": 0.0}))
        # Every node before any edge: a state may read a sum node that would
        # otherwise not exist yet.
        for channel in sums:
            if f"s{channel}" in reached:
                graph.add_node(Node(f"s{channel}", "add"))
        for index, sources in sources_of.items():
            name = node_of(index)
            if name not in graph.nodes:
                continue
            for source, weight in sources:
                graph.add_edge(Edge(f"e_{source}_{name}", source, name, {"weight": weight}))
            if index < states and f"x{index}" in graph.nodes:
                graph.add_edge(Edge(f"e_{name}_x{index}", name, f"x{index}", {"weight": 1.0}))
        for channel, sources in sums.items():
            if f"s{channel}" not in graph.nodes:
                continue
            for source, weight in sources:
                graph.add_edge(Edge(f"e_{source}_s{channel}", source, f"s{channel}", {"weight": weight}))
        for channel, (source, weight) in enumerate(output_sources):
            graph.add_edge(Edge(f"e_{source}_y{channel}", source, f"y{channel}", {"weight": weight}))
        return graph

    # --- creation ----------------------------------------------------------------

    def _record(self, rng: Random, chromosome: str, index: int, states: int) -> list[int]:
        """A random record that usually reads something that exists."""
        inputs = self.grammar.inputs
        last = self.config.source_layout == "inputs-last"
        # Fresh records read only the plant's own inputs under inputs-last;
        # auxiliary inputs are reached by mutation, if they are worth it.
        readable = self.primary if last else inputs
        record = [rng.randrange(len(self.kinds[chromosome])), rng.randrange(PARAMETER_SLOTS)]
        for _ in range(2):
            draw = rng.random()
            if chromosome == "state":
                if draw < 0.35:
                    code = rng.randrange(readable)
                elif draw < 0.75 and states:
                    code = inputs + rng.randrange(states)
                else:
                    code = inputs + STATE_SLOTS + rng.randrange(self.grammar.outputs)
            else:
                if draw < 0.3:
                    code = rng.randrange(readable)
                elif draw < 0.65 and states:
                    code = inputs + rng.randrange(states)
                elif index:
                    code = inputs + STATE_SLOTS + min(OUTPUT_REACH - 1, int(rng.expovariate(1.0)), index - 1)
                else:
                    code = rng.randrange(readable)
            if last:
                code = self._to_last(code, chromosome)
            record += [code, rng.randrange(self.sizes["weight"])]
        return record

    def _synonyms(self, values: list[int], rng: Random) -> list[int]:
        """Meanings written as codes: any of each value's synonyms."""
        r = self.config.redundancy
        return values if r == 1 else [value * r + rng.randrange(r) for value in values]

    def random_genome(self, rng: Random) -> Genome:
        states = rng.randint(*self.init_states)
        outputs = rng.randint(*self.init_outputs)
        genes = [states]
        for index in range(states):
            genes += self._synonyms(self._record(rng, "state", index, states), rng)
        for index in range(outputs):
            genes += self._synonyms(self._record(rng, "output", index, states), rng)
        inputs = self.grammar.inputs
        for _ in range(self.grammar.outputs):
            for _ in range(self.config.output_sources):
                code = inputs + STATE_SLOTS + rng.randrange(min(3, max(1, outputs)))
                if self.config.source_layout == "inputs-last":
                    code = self._to_last(code, "output")
                genes += self._synonyms([code, rng.randrange(self.sizes["weight"])], rng)
        return tuple(genes)

    # --- variation ----------------------------------------------------------------

    def _locus(self, genome: Genome, rng: Random) -> int:
        """A position for a point mutation; state loci weigh ``state_rate``."""
        rate = self.config.state_rate
        hotspot = self.config.hotspot
        if hotspot:
            state_loci = WIDTH * genome[0]
            weights = []
            for position in range(1, len(genome)):
                repeats = (position > 1 and genome[position - 1] == genome[position]) + (
                    position + 1 < len(genome) and genome[position + 1] == genome[position])
                weights.append((rate if position <= state_loci else 1.0) * (1.0 + hotspot * repeats))
            return 1 + rng.choices(range(len(weights)), weights)[0]
        if rate == 1.0:
            return rng.randrange(1, len(genome))
        state_loci = WIDTH * genome[0]
        other = len(genome) - 1 - state_loci
        if rng.random() * (rate * state_loci + other) < rate * state_loci:
            return 1 + rng.randrange(state_loci)
        return 1 + state_loci + rng.randrange(other)

    def creep(self, genome: Genome, rng: Random) -> Genome | None:
        if len(genome) < 2:
            return None
        position = self._locus(genome, rng)
        size = self.size_at(genome, position)
        value = min(max(genome[position] + rng.choice((-1, 1)), 0), size - 1)
        return genome[:position] + (value,) + genome[position + 1:]

    def reset(self, genome: Genome, rng: Random) -> Genome | None:
        if len(genome) < 2:
            return None
        position = self._locus(genome, rng)
        mode = self.config.source_reset
        if mode != "uniform" and self.role_at(genome, position)[1] == "source":
            value = (self._pooled_source if mode == "pooled" else self._near)(genome, position, rng)
        else:
            value = rng.randrange(self.size_at(genome, position))
        return genome[:position] + (value,) + genome[position + 1:]

    def _pooled_source(self, genome: Genome, position: int, rng: Random) -> int:
        """A source drawn as if there were one input, then which input."""
        r, inputs = self.config.redundancy, self.grammar.inputs
        meanings = self._meanings_at(genome, position)
        drawn = rng.randrange((meanings - inputs + 1) * r)
        meaning, synonym = divmod(drawn, r)
        if self.config.source_layout == "inputs-last":
            span = self._span("state" if self.role_at(genome, position)[0] == "state" else "output")
            if meaning == span and inputs > 1:
                meaning += rng.randrange(inputs)
        elif meaning == 0:
            meaning = rng.randrange(inputs) if inputs > 1 else 0
        else:
            meaning += inputs - 1
        return meaning * r + synonym

    def _near(self, genome: Genome, position: int, rng: Random) -> int:
        """A code a geometric number of steps away, reflected inside the alphabet."""
        size = self.size_at(genome, position)
        if size < 2:
            return genome[position]
        step = 1
        while rng.random() < 0.5:
            step += 1
        value = genome[position] + step * rng.choice((-1, 1))
        period = 2 * (size - 1)
        value %= period
        return value if value < size else period - value

    def _chromosomes(self, genome: Genome) -> tuple[list[Genome], list[Genome], Genome]:
        states, outputs = self.split(genome)
        records = [genome[1 + i * WIDTH: 1 + (i + 1) * WIDTH] for i in range(states + outputs)]
        return records[:states], records[states:], genome[len(genome) - self.tail():]

    @staticmethod
    def _join(state: list, output: list, terminals: Genome) -> Genome:
        return (len(state),) + tuple(g for r in state for g in r) + tuple(g for r in output for g in r) + terminals

    def insert_record(self, genome: Genome, rng: Random) -> Genome | None:
        state, output, terminals = self._chromosomes(genome)
        if rng.random() < 0.5:
            if len(state) >= STATE_SLOTS:
                return None
            at = rng.randint(0, len(state))
            state.insert(at, tuple(self._synonyms(self._record(rng, "state", at, len(state) + 1), rng)))
        else:
            at = rng.randint(0, len(output))
            output.insert(at, tuple(self._synonyms(self._record(rng, "output", at, len(state)), rng)))
        return self._join(state, output, terminals)

    def remove_record(self, genome: Genome, rng: Random) -> Genome | None:
        state, output, terminals = self._chromosomes(genome)
        chromosome = state if rng.random() < 0.5 else output
        if not chromosome:
            return None
        chromosome.pop(rng.randrange(len(chromosome)))
        return self._join(state, output, terminals)

    def duplicate_record(self, genome: Genome, rng: Random) -> Genome | None:
        """A record copied right after itself, in its own chromosome."""
        state, output, terminals = self._chromosomes(genome)
        chromosome = state if rng.random() < 0.5 else output
        if not chromosome or (chromosome is state and len(state) >= STATE_SLOTS):
            return None
        at = rng.randrange(len(chromosome))
        chromosome.insert(at + 1, chromosome[at])
        return self._join(state, output, terminals)

    def _mutation_weights(self) -> dict[str, float]:
        config = self.config
        weights = dict(MUTATION_WEIGHTS)
        if config.structural_rate != 1.0:
            weights["insert_record"] *= config.structural_rate
            weights["remove_record"] *= config.structural_rate
        if config.duplication:
            weights["duplicate_record"] = config.duplication
        return weights

    def mutate(self, genome: Genome, rng: Random, attempts: int = 8) -> Genome:
        weights = self._mutation_weights()
        names = list(weights)
        for _ in range(attempts):
            name = rng.choices(names, [weights[n] for n in names])[0]
            child = getattr(self, name)(genome, rng)
            if child is not None and child != genome:
                return child
        return genome

    def crossover(self, left: Genome, right: Genome, rng: Random) -> Genome:
        """One-point within each chromosome, cut independently in each parent.

        Under ``maternal`` inheritance only the output chromosome is cut; the
        states come whole from ``left``.
        """
        ls, lo, lt = self._chromosomes(left)
        rs, ro, _ = self._chromosomes(right)
        if self.config.inheritance == "maternal":
            state = ls
        else:
            state = ls[: rng.randint(0, len(ls))] + rs[rng.randint(0, len(rs)):]
        output = lo[: rng.randint(0, len(lo))] + ro[rng.randint(0, len(ro)):]
        return self._join(state[:STATE_SLOTS], output, lt)

    def organelle(self, left: Genome, right: Genome, rng: Random) -> Genome:
        """The state chromosome of ``right`` inside the output of ``left``."""
        _, lo, lt = self._chromosomes(left)
        rs, _, _ = self._chromosomes(right)
        return self._join(rs, lo, lt)

    def translate(self, genome: Genome):
        raise NotImplementedError("a state-space genome builds its graph directly; use build()")
