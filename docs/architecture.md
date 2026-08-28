# Architecture

AMEBA separates evolutionary graph manipulation from graph interpretation.

## Dependency direction

```text
ameba_graph  <──  ameba_signal
     ▲                  ▲
     └────── ameba ─────┘
```

`ameba_graph` must never import `ameba_signal` or the application package. This
makes the evolutionary layer usable with arbitrary directed graphs.

## Generic graph evolution

`ameba_graph` owns graph identity, topology, structural validation, mutation and
crossover contracts, evaluator contracts, populations, selection, and evolution.
Node kinds and attributes are opaque to this layer. Domain rules enter through a
`GraphPolicy`; candidate scoring enters through an `Evaluator`.

## Mathematical signal simulation

`ameba_signal` interprets a graph as a mathematical signal network. It owns the
operator registry, port rules, signal propagation, numerical safety, stateful
execution, datasets, and signal-domain fitness.

The simulator supports stateless operators and isolated, discrete-time execution
sessions. Delay nodes expose prior state before each combinational evaluation and
therefore act as explicit feedback boundaries. Integral, derivative, low-pass,
and high-pass operators update their private session state when evaluated.

Feedback is valid only when every loop passes through a delay. A loop without a
delay is an algebraic cycle and is rejected by both policy validation and the
runtime. Evaluating a dataset uses one session across all rows; starting another
session or calling `reset()` restores operator initial conditions.

## Application layer

`ameba` composes a graph policy, evolutionary configuration, and evaluator. It
provides the CLI, checkpoint workflows, configuration loading, visualization,
and end-user commands without moving domain behavior into the generic layer.

## Reproduction safety

The generic layer owns transaction boundaries and lock enforcement. Structure
locks protect nodes, endpoint locks protect individual edge ends, and attribute
locks protect domain-owned values. Mutations and crossovers operate on copies,
validate the complete result, use only the supplied random generator, and have
finite attempt budgets. Operator portfolios try each candidate at most once.

## Parallel simulation

An evolution engine may evaluate the graphs in a generation through a persistent
process pool. The coordinator still performs selection and reproduction using
the single seeded random generator, then collects worker scores in population
order. Worker completion order therefore cannot change evolution or checkpoint
replay. Evaluators used this way must be serializable by Python multiprocessing.

This parallelizes final population simulation. Parameter refinement currently
runs in the coordinator because it is an iterative hill climb whose next trial
depends on the previous score.

## Island evolution

The configured total population can be divided evenly into independent islands.
Selection, elitism, topology protection, crossover, and mutation operate only
within an island. At a configured generation interval, the best candidates move
around a deterministic ring: each island receives copies from its predecessor
and replaces the same number of its worst candidates. Migration uses a snapshot,
so no candidate can cross more than one boundary in a migration round.

Island boundaries and the coordinator's random state are stored in checkpoints.
This makes an interrupted multi-island run replay identically after resume,
including the migration schedule. `population_size` remains the total search
budget and must be divisible by `island_count`; tournament, elite, topology
protection, and migration sizes must fit within one island.

Each island may receive its own `GraphPolicy`. This permits heterogeneous search
niches—for example static-only creation, delay-oriented creation, nonlinear
creation, and an unrestricted island—without putting domain knowledge into
`ameba_graph`. Reproduction uses the local policy. A migrant is copied only when
the destination policy validates it; rejected migrants leave the destination's
existing population intact. Signal profiles intentionally restrict local node
creation rather than existing nodes, allowing migration to cross-pollinate the
niches.

## Core rule

Evolution asks only for a numeric score:

```python
score = evaluator.evaluate(candidate_graph)
```

It does not know whether that score came from signal simulation, a neural model,
a workflow engine, or another graph-based domain.
