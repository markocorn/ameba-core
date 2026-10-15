"""Tag addressing for the state-space genome: sources bind by content, not place.

In the state-space encoding (:mod:`ameba_genome.statespace`) a source names
its target by position -- "state 3", "the output record two back" -- so a
record inserted, removed or silenced renumbers everything after it and rewires
the model. Genes do not find each other that way: a protein binds whatever
fits its shape, wherever on the chromosome that was made.

Here every record carries a tag, and a source names the tag it looks for. It
binds to the record whose tag is nearest the one wanted -- the closest fit --
among the records it may read:

* a state record: any state (itself included, a step back), or a model
  output by its own codes, or an input;
* an output record: any state, or one of the ``OUTPUT_REACH`` output records
  before it, or an input;
* a terminal: any state, or one of the last ``OUTPUT_REACH`` output records,
  or an input.

Ties go to the nearer record in reading order (states first, then output
records), then the earlier one. A record that is lost no longer fits
anything, and what bound to it binds to the next best fit; nothing else moves.

    record   [kind, parameter, tag, source1, weight1, source2, weight2]
    source   tag 0..tags-1 | model output (state records) | input

Inputs sit at the end of each source alphabet, as in the ``inputs-last``
layout. The genome is resolved into the state-space encoding's fixed-record
form and built by it, so both share one tested decoder.
"""

from __future__ import annotations

from dataclasses import replace
from random import Random

from ameba_graph import Graph

from .decoding import Grammar
from .dictionary import WEIGHTS
from .statespace import (MUTATION_WEIGHTS, OUTPUT_REACH, PARAMETER_SLOTS, STATE_SLOTS, TERMINAL, StateSpaceConfig,
                         StateSpaceEncoding)

Genome = tuple[int, ...]
WIDTH = 7


class TagEncoding:
    width = WIDTH

    def __init__(self, grammar: Grammar, config: StateSpaceConfig | None = None, tags: int = 16,
                 primary_inputs: int | None = None) -> None:
        config = config or StateSpaceConfig()
        if tags < 1:
            raise ValueError("tags must be at least 1")
        # The fixed form handed to the decoder is always the inputs-first one.
        self.inner = StateSpaceEncoding(grammar, config=replace(config, source_layout="inputs-first"),
                                        primary_inputs=primary_inputs)
        self.config = config
        self.grammar = grammar
        self.tags = tags
        self.kinds = self.inner.kinds
        self.primary = self.inner.primary
        inputs, outputs = grammar.inputs, grammar.outputs
        self.state_sources = tags + outputs + inputs
        self.output_sources = tags + inputs

    # --- layout ------------------------------------------------------------

    def tail(self) -> int:
        return self.inner.tail()

    def record_sizes(self, chromosome: str) -> tuple[int, ...]:
        sources = self.state_sources if chromosome == "state" else self.output_sources
        return (len(self.kinds[chromosome]), PARAMETER_SLOTS, self.tags, sources, len(WEIGHTS), sources, len(WEIGHTS))

    def terminal_sizes(self) -> tuple[int, ...]:
        return (self.output_sources, len(WEIGHTS)) * (self.tail() // TERMINAL)

    def split(self, genome: Genome) -> tuple[int, int]:
        states = genome[0]
        return states, (len(genome) - 1 - self.tail()) // WIDTH - states

    def size_at(self, genome: Genome, position: int) -> int:
        states, outputs = self.split(genome)
        body = 1 + WIDTH * (states + outputs)
        if position >= body:
            return self.terminal_sizes()[position - body]
        chromosome = "state" if (position - 1) // WIDTH < states else "output"
        return self.record_sizes(chromosome)[(position - 1) % WIDTH]

    def _chromosomes(self, genome: Genome):
        states, outputs = self.split(genome)
        records = [genome[1 + i * WIDTH: 1 + (i + 1) * WIDTH] for i in range(states + outputs)]
        return records[:states], records[states:], genome[len(genome) - self.tail():]

    @staticmethod
    def _join(state, output, terminals) -> Genome:
        return (len(state),) + tuple(g for r in state for g in r) + tuple(g for r in output for g in r) + tuple(terminals)

    # --- resolving ------------------------------------------------------------

    def fixed(self, genome: Genome) -> Genome:
        """The genome in the state-space encoding's fixed-record, inputs-first form."""
        state, output, terminals = self._chromosomes(genome)
        state = state[:STATE_SLOTS]
        inputs, outputs, tags = self.grammar.inputs, self.grammar.outputs, self.tags
        states = len(state)
        state_tags = [record[2] for record in state]
        output_tags = [record[2] for record in output]
        nothing = inputs + STATE_SLOTS - 1  # a state slot no genome with fewer than 16 states has

        def bind(wanted: int, here: int, reach: list[int]):
            """("x", state) or ("h", output record): the best fit among all
            states and the output records in ``reach``; None if there are none."""
            best = None
            for j, tag in enumerate(state_tags):
                key = (abs(tag - wanted), abs(here - j), j)
                if best is None or key < best[0]:
                    best = (key, ("x", j))
            for q in reach:
                key = (abs(output_tags[q] - wanted), abs(here - (states + q)), states + q)
                if best is None or key < best[0]:
                    best = (key, ("h", q))
            return None if best is None else best[1]

        def state_code(source: int, index: int) -> int:
            if source < tags:
                return inputs + bind(source, index, [])[1]  # a state record always fits a state
            if source < tags + outputs:
                return inputs + STATE_SLOTS + (source - tags)
            return source - tags - outputs

        def output_code(source: int, position: int) -> int:
            if source >= tags:
                return source - tags
            fit = bind(source, states + position, list(range(max(0, position - OUTPUT_REACH), position)))
            if fit is None:
                return nothing
            if fit[0] == "x":
                return inputs + fit[1]
            return inputs + STATE_SLOTS + (position - fit[1] - 1)  # a back reference

        def terminal_code(source: int) -> int:
            if source >= tags:
                return source - tags
            count = len(output)
            fit = bind(source, states + count, list(range(max(0, count - OUTPUT_REACH), count)))
            if fit is None:
                return nothing
            if fit[0] == "x":
                return inputs + fit[1]
            return inputs + STATE_SLOTS + (count - fit[1] - 1)

        genes: list[int] = [states]
        for index, (kind, parameter, _, s1, w1, s2, w2) in enumerate(state):
            genes += [kind, parameter, state_code(s1, index), w1, state_code(s2, index), w2]
        for position, (kind, parameter, _, s1, w1, s2, w2) in enumerate(output):
            genes += [kind, parameter, output_code(s1, position), w1, output_code(s2, position), w2]
        for i in range(0, len(terminals), TERMINAL):
            genes += [terminal_code(terminals[i]), terminals[i + 1]]
        return tuple(genes)

    def build(self, genome: Genome) -> Graph:
        return self.inner.build(self.fixed(genome))

    # --- creation ----------------------------------------------------------------

    def _input(self, rng: Random, chromosome: str) -> int:
        offset = self.tags + (self.grammar.outputs if chromosome == "state" else 0)
        return offset + rng.randrange(self.primary if self.config.source_layout == "inputs-last" else self.grammar.inputs)

    def _record(self, rng: Random, chromosome: str, index: int, states: int,
                state_tags: list[int] | None = None, output_tags: list[int] | None = None) -> list[int]:
        """A random record; with the tags of existing records it reads them as the
        state-space encoding's fresh records read their targets."""
        tag = rng.randrange(self.tags)
        record = [rng.randrange(len(self.kinds[chromosome])), rng.randrange(PARAMETER_SLOTS), tag]
        for _ in range(2):
            draw = rng.random()
            if chromosome == "state":
                if draw < 0.35:
                    code = self._input(rng, "state")
                elif draw < 0.75 and states:
                    code = state_tags[rng.randrange(states)] if state_tags else rng.randrange(self.tags)
                else:
                    code = self.tags + rng.randrange(self.grammar.outputs)
            else:
                if draw < 0.3:
                    code = self._input(rng, "output")
                elif draw < 0.65 and states:
                    code = state_tags[rng.randrange(states)] if state_tags else rng.randrange(self.tags)
                elif index:
                    back = min(OUTPUT_REACH - 1, int(rng.expovariate(1.0)), index - 1)
                    code = output_tags[index - 1 - back] if output_tags else rng.randrange(self.tags)
                else:
                    code = self._input(rng, "output")
            record += [code, rng.randrange(len(WEIGHTS))]
        return record

    def random_genome(self, rng: Random) -> Genome:
        states = rng.randint(*self.inner.init_states)
        outputs = rng.randint(*self.inner.init_outputs)
        state_tags = [rng.randrange(self.tags) for _ in range(states)]
        output_tags = [rng.randrange(self.tags) for _ in range(outputs)]
        state = []
        for index in range(states):
            record = self._record(rng, "state", index, states, state_tags, output_tags)
            record[2] = state_tags[index]
            state.append(tuple(record))
        output = []
        for index in range(outputs):
            record = self._record(rng, "output", index, states, state_tags, output_tags)
            record[2] = output_tags[index]
            output.append(tuple(record))
        terminals = []
        for _ in range(self.grammar.outputs):
            for _ in range(self.config.output_sources):
                if outputs:
                    code = output_tags[outputs - 1 - rng.randrange(min(3, outputs))]
                else:
                    code = rng.randrange(self.tags)
                terminals += [code, rng.randrange(len(WEIGHTS))]
        return self._join(state, output, terminals)

    # --- variation ----------------------------------------------------------------

    def creep(self, genome: Genome, rng: Random) -> Genome | None:
        if len(genome) < 2:
            return None
        position = rng.randrange(1, len(genome))
        value = min(max(genome[position] + rng.choice((-1, 1)), 0), self.size_at(genome, position) - 1)
        return genome[:position] + (value,) + genome[position + 1:]

    def reset(self, genome: Genome, rng: Random) -> Genome | None:
        if len(genome) < 2:
            return None
        position = rng.randrange(1, len(genome))
        return genome[:position] + (rng.randrange(self.size_at(genome, position)),) + genome[position + 1:]

    def insert_record(self, genome: Genome, rng: Random) -> Genome | None:
        state, output, terminals = self._chromosomes(genome)
        if rng.random() < 0.5:
            if len(state) >= STATE_SLOTS:
                return None
            at = rng.randint(0, len(state))
            state.insert(at, tuple(self._record(rng, "state", at, len(state) + 1)))
        else:
            at = rng.randint(0, len(output))
            output.insert(at, tuple(self._record(rng, "output", at, len(state))))
        return self._join(state, output, terminals)

    def remove_record(self, genome: Genome, rng: Random) -> Genome | None:
        state, output, terminals = self._chromosomes(genome)
        chromosome = state if rng.random() < 0.5 else output
        if not chromosome:
            return None
        chromosome.pop(rng.randrange(len(chromosome)))
        return self._join(state, output, terminals)

    def mutate(self, genome: Genome, rng: Random, attempts: int = 8) -> Genome:
        names = list(MUTATION_WEIGHTS)
        for _ in range(attempts):
            name = rng.choices(names, [MUTATION_WEIGHTS[n] for n in names])[0]
            child = getattr(self, name)(genome, rng)
            if child is not None and child != genome:
                return child
        return genome

    def crossover(self, left: Genome, right: Genome, rng: Random) -> Genome:
        ls, lo, lt = self._chromosomes(left)
        rs, ro, _ = self._chromosomes(right)
        if self.config.inheritance == "maternal":
            state = ls
        else:
            state = ls[: rng.randint(0, len(ls))] + rs[rng.randint(0, len(rs)):]
        output = lo[: rng.randint(0, len(lo))] + ro[rng.randint(0, len(ro)):]
        return self._join(state[:STATE_SLOTS], output, lt)

    def translate(self, genome: Genome):
        raise NotImplementedError("a tagged genome builds its graph directly; use build()")
