# Genome encoding

A third search method, beside the population engine and the archive. Instead of
mutating the graph directly, a model is written as a **flat sequence of integer
genes**, a deterministic decoder turns the sequence into a graph, and a
classical generational GA searches over the integers. The graph engine's
operators have to know what a valid graph is. Here only the decoder does, and
variation is just editing a list.

Package: `ameba_genome`. Driver: `scripts/genome_search.py`.

```text
G = [1, 100, 211, 150, 211, 12, 600, 110, 50, 111]

 1:node:add   100:src:u0  211:w=0.5  150:src:+1  211:w=0.5
12:node:delay 600:steps=1 110:src:-1
50:out:y0     111:src:-2

y(k+1) = 0.5·y(k) + 0.5·u(k)
```

## The rules, and how each is met

| # | rule | implementation |
|---|---|---|
| 1 | a genome is a sequence of integers | `Genome = tuple[int, ...]` |
| 2 | each integer is one gene, its meaning in a separate dictionary | `ameba_genome.dictionary.GENES` |
| 3 | structural, connection, parameter, and temporal genes | the `category` of every entry; see the table below |
| 4 | a gene names an allowed parameter value rather than holding one | each parameter family is a fixed alphabet: `211 → w=0.5`, `306 → gain=1` |
| 5 | decoding is deterministic, D(G) → A | `decode(genome, grammar)`; pure function of its arguments |
| 6 | a genome can only produce valid graphs | the decoder is **total**: every integer sequence decodes to a graph `SignalGraphPolicy` accepts |
| 7 | mutation at gene level: replace, add, remove, change a parameter | `replace_gene`, `insert_gene`, `remove_gene`, `step_parameter` |
| 8 | mutation draws only from genes allowed in context | `allowed_at(genome, position, grammar)` |
| 9 | a gene ID is permanent | fixed, append-only ID blocks per family, pinned by a test |

Rule 6 is met more strongly than it asks: rather than every *mutation* keeping
the genome valid, *no* integer sequence can produce an invalid graph. Genes
that cannot be expressed where they stand are introns, which are carried and
inherited but not expressed, as in linear and Cartesian GP. Rule 8 then becomes
a question of efficiency rather than safety: the context stops a mutation from
spending itself writing an intron.

## The dictionary

| IDs | family | category | meaning |
|---|---|---|---|
| 1–17 (to 49) | `node:<kind>` | structural | open a node record of that operator |
| 50–59 | `out:y<c>` | structural | open the record for output channel `c` |
| 100–109 | `src:u<i>` | connection | edge from model input `i` |
| 110–141 | `src:-k` | connection | edge from the node `k` records back |
| 150–181 | `src:+k` | connection | edge from the node `k` records ahead |
| 200–215 | `w=<v>` | parameter | weight of the record's latest edge, 16 values in ±2 |
| 300–379 | `gain=`, `alpha=`, `value=`, `phase=` | parameter | node parameters, each its own alphabet |
| 600–639 | `steps=`, `time_step=` | temporal | delay length, derivative time step |

Each family owns a block and assigns IDs by position in its own value list. A
new operator or a new allele takes the next free ID in its block, so no existing
ID ever changes meaning. `tests/test_genome_encoding.py` pins a sample of them.
`initial` and `initial_input` have no genes. They are starting offsets rather
than dynamics, and they stay at zero.

Sources are **relative** (`src:-2`), except model inputs. A block of genes
therefore means the same thing wherever crossover puts it, which an absolute
node number would not. For example, `node:add src:-1 src:-2` is "sum the two
nodes before me" anywhere in any genome.

## Decoding

1. **Records.** A structural gene opens a record. The genes after it, up to the
   next structural gene, describe it. A weight gene applies to the record's
   latest edge. For a parameter gene, the last one wins.
2. **Legal edges.** An edge from node `s` into node `t` is kept when `s`
   precedes `t`, or when either is a `delay`. Every cycle must contain a
   backwards edge, and every backwards edge touches a delay, so **an algebraic
   loop cannot be written down**, and feedback through a delay always can.
   Duplicates and self-loops are dropped.
3. **Arity.** A node keeps its first sources up to its kind's maximum. A node
   below its minimum is dead, references to it die with it, and this repeats
   to a fixed point.
4. **Outputs.** Output `c` takes the first live source of its `out:y<c>`
   records. If there is none, it falls back to input `u_min(c, I-1)`, so the
   empty genome decodes to the static seed `y = u`. Nodes that reach no output
   are dropped.

## Variation and the GA

| mutation | effect |
|---|---|
| `replace_gene` | one gene swapped for another allowed at its position |
| `insert_gene` | one allowed gene added; a structural gene moves to the next record boundary, so it cannot capture a neighbour's genes |
| `remove_gene` | one gene deleted |
| `step_parameter` | a parameter or temporal gene moved to the neighbouring value on its ladder |
| `insert_record` | *extension:* a complete random node record, i.e. several insertions at once (`--no-insert-record` disables it) |

Crossover is `splice`, a one-point cut-and-splice at record boundaries with
independent cut points, so child length varies.

The GA (`ameba_genome.ga`) is domain-neutral. It sees integer tuples, a
`breed` function, and a scoring function. Each generation keeps an elite of 2,
selects parents by tournament (size 3), crosses with probability 0.5, then
applies one mutation and a geometric number more. Scores are cached by decoded
*phenotype*, so a child that differs from its parent only in introns costs no
simulation.

## First measurements

Same datasets, criteria, and floors as the other two methods: 150 steps, data
seed 7, ISE. "Neutral drift" is `--neutral-drift`, explained below.

| rung | configuration | below floor | best | other methods' best |
|---|---|---|---|---|
| `linear` | 300 gen × 60, 8 seeds | **8/8** | **0.0007×** (5/8 seeds) | population engine 0.02–0.70× |
| `narendra` | 2000 gen × 100, 4 seeds | 0/4 | 1.0033× | — |
| `narendra` | same, neutral drift | 2/4 | **0.9116×** | population 0.809×, archive 0.1947× |
| `tank-minphase` | 1500 gen × 100, 2 seeds, neutral drift | 2/2 | **0.1101×** | — |
| `tank-nonminphase` | same | 2/2 | **0.1461×** | — |

On the tank rungs the floor is the stricter of the two bars. That is the
stateless *search* baseline: 282.35 and 294.11, against closed forms of 504.13
and 453.99. The baseline ran at its own default budget (12 × 20 × 3 seeds), not
at the GA's, so a baseline matched to the GA's budget may sit lower. The best
tank model has 21 nodes, 13 of them stateful.

### The linear rung is easy for this representation

Every seed passed. Five of eight converged on the same model, four cascaded
first-order low-pass sections at 0.0007× the floor, inside 300 generations
and a few seconds. Like the population engine, it does not find the reference
delay chain. A cascade of `filter_lp` is shorter to *write* in genes than three
delays with tuned feedback weights, so it is what the search finds.

### On Narendra, the plateau is a size attractor

Without drift, all four seeds collapsed within 250 generations to the same
three-gene genome, `out:y0 src:u0 w=0.75` (`y = 0.75·u`). That is the
memoryless floor, rounded to the weight alphabet. Unique phenotypes stopped
appearing: about 600 simulations in 2000 generations, so the population was
clones.

The cause was the tie-break. Ties went to the shorter genome, and an
unexpressed record, which is the first step toward any new structure, scores
exactly what its parent scores. It therefore always lost. Leaving ties to
chance lets neutral variants replace their parents and drift, as Cartesian GP
relies on. With that one change, 2 of 4 seeds went below the floor, genomes
grew to 100–160 genes, and the number of distinct phenotypes evaluated rose
from ~600 to ~20,000 per run.

This is the same plateau the population engine and the archive met, for a
related reason. A structural novelty is judged before it can pay off. The
archive answers that with local competition; here the cheapest answer is
neutrality.

## Open questions

* **Alphabet resolution.** Weights are 16 fixed values. The linear rung did not
  need more, but Narendra's coefficients may. One option is interval genes
  (rule 4's "or interval"), where a gene selects a range and a final local
  refinement chooses the point inside it.
* **Neutral drift as the default.** Measured only on 4 seeds. It needs more
  seeds and a size cap, because genomes grow to the 200-gene limit.
* **Crossover's contribution.** Untested. Run `--crossover-rate 0` against the
  default.
* **Homologous crossover.** Permanent IDs make it possible to align two genomes
  by shared genes (rule 9's example: `1 2 3 4 7 10 17` against
  `1 2 3 4 8 11 17`). `splice` does not use that yet.
* **Events.** Rule 3's "time and events": time is covered by `steps` and
  `time_step`, but the simulator has no event concept, so there are no event
  genes.
