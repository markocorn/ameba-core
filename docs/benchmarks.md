# Identification benchmarks

## The setup

Every benchmark is single-input/single-output. A plant is driven by a scalar
control sequence u(k), and the model is scored on predicting y(k+1) while
seeing **only u(k)**. The model must carry its own state through delay or filter
nodes, so its prediction error feeds back into the next step.

This is deliberately the only setup offered. The common alternative — supplying
measured plant history as extra model inputs — turns identification into fitting
a static map, and a model scored that way never has to represent the system's
dynamics at all.

## Fitness

Fitness is an integral error criterion, minimized. The default is **ISE**, the
integral of squared error, which with a unit sample time is exactly the sum of
squared error:

| criterion | form | character |
|---|---|---|
| `ise` | `∫ e² dt` | default; punishes one large deviation far more than several small ones |
| `iae` | `∫ \|e\| dt` | weights deviations evenly, so it tolerates a few outliers |
| `itse` | `∫ t·e² dt` | discounts early error, penalises error that persists |
| `itae` | `∫ t·\|e\| dt` | as above, without the squared emphasis on large deviations |

`SignalEvaluator(dataset, criterion="iae")` selects one; the script takes
`--criterion`. The integral is the rectangle rule over the run, so `time_step`
scales the result linearly.

Two properties are worth keeping in mind. The score is a sum, not a mean, so it
grows with run length and with input amplitude — runs are only comparable at
matched settings, which is why the reports also carry the normalised fit
percentage. And the criterion measures accuracy alone: nothing penalises graph
size, so an evolved model has no incentive to shed nodes that do not help.

The time-weighted criteria are designed for transient-response tuning, where
"error that persists after a step" is the thing to punish. Under a persistently
exciting random input there is no settling transient, so weighting late samples
above early ones is arbitrary. Prefer ISE or IAE for identification.

## The pass criterion

Improving on the seed is not evidence of anything: a model can improve simply by
finding a better gain on u(k). The real criterion is the **memoryless floor**,
`static_gain_floor(trajectory)` — the best score attainable by `y(k+1) = c·u(k)`.

Because these plants are driven by an independent zero-mean control sequence, no
function of u(k) alone beats the fitted gain. Scoring below the floor is
therefore not reachable without representing state. That single number separates
dynamic identification from gain fitting.

## The ladder

### Rung 1 — third-order linear (`benchmarks.linear`)

```text
y(k+1) = 1.5 y(k) - 0.74 y(k-1) + 0.12 y(k-2) + 0.12 u(k)
```

Poles at 0.4, 0.5, 0.6, so the plant is strictly stable and well damped; the
input gain gives unit DC gain. The reference graph is six nodes: the input, one
weighted sum, three unit delays, the output.

Nothing here is nonlinear and nothing is numerically delicate. It is the
smallest honest dynamic identification task, and it exists to separate two
failure modes: a method that cannot solve this has a search or delay-handling
problem, while a method that solves this but fails rung 2 has a structural-search
problem over nonlinearities.

### Rung 2 — Narendra plant (`benchmarks.narendra`)

Narendra and Parthasarathy (1990), example 4:

```text
y(k+1) = ( y(k) y(k-1) y(k-2) u(k-1) [y(k-2) - 1] + u(k) )
         / ( 1 + y(k-1)^2 + y(k-2)^2 )
```

Parameter tuning alone cannot reach this. It needs a five-way product, a
division, and three steps of output history, so it measures structural search
rather than weight convergence.

## Standing results

Both reference graphs reproduce their plant to floating-point noise (~1e-30 SSE)
across the training input, a sinusoid, and a step sequence. That is the evidence
that delay-based feedback is implemented correctly.

| Benchmark | Seed | Memoryless floor | Evolution | Verdict |
|---|---|---|---|---|
| Linear 3rd order | 40.93 | 9.20 | 0.02 – 0.70 | **solved** |
| Narendra | 4.25 | 1.2603 | 1.2603 | **open** |

**Rung 1 passes, and not the way the reference graph does it.** Evolution does
not rediscover the delay chain. It converges instead on three cascaded
first-order `filter_lp` sections — an equivalent third-order realization it
found on its own. This is the method working as intended: the architecture is
what is being evolved, so a different valid realization is a success, not a
mismatch. The tests therefore assert *state was used*, never that a particular
topology was found.

**Rung 2 fails, and it fails cleanly.** Evolution converges to 1.2603 on every
seed tried — the memoryless floor to four decimal places — with zero stateful
nodes in the best graph. It finds the optimal static gain and then stops. There
is no partial credit and no sign of dynamics.

### Why it is stuck: a needle, not a local optimum

Evolution converges to *exactly* the memoryless floor -- 1.6420 at amplitude 1.0,
15.3578 at amplitude 1.6, identical to four decimals across every seed tried.
This is not a tuning problem, and the usual suspects were ruled out by
measurement:

* not the parsimony penalty -- a penalty of zero gives the same result;
* not the operator set -- the full seventeen operators change nothing;
* not the input amplitude -- more nonlinear content does not help;
* not initialisation -- seeding the population from randomly generated
  six- and twelve-node dynamic cells still collapses to the floor.

The reason shows up when the neighbourhood of the true solution is sampled.
Starting from the exact reference and applying random mutations:

| mutations away | median ISE | beats the floor |
|---|---|---|
| 1 | 3.22 | 9/30 |
| 2 | 610.86 | 5/30 |
| 3 | 57118 | 4/30 |

One mutation from the answer, the median candidate is already worse than a
two-node static gain. The exact model sits on a needle whose surroundings are
worse than the plateau, so no path of improving steps connects them. The static
gain is not a local optimum the search fails to escape; it is a globally
attractive plateau, and the solution is somewhere else entirely.

### What the evidence points at

The neighbourhood is only that bad because a new topology is scored the instant
it is born, while its weights are still whatever the mutation happened to
produce. Giving those candidates a short weight-only refinement before judging
them changes the picture:

| structural mutations | median ISE at birth | after refinement | beats the floor |
|---|---|---|---|
| 1 | 12.44 | **1.14** | 15/25 |
| 2 | 41.82 | 2.32 | 10/25 |
| 3 | 130.96 | 41.82 | 4/25 |

A single structural change scores roughly eight times worse than the static gain
at birth and better than it after tuning. Selection currently discards those
candidates before they can demonstrate anything, which is the mechanism that
keeps the plateau attractive.

That points at protecting structural innovation long enough to be evaluated
fairly. A local multi-scale parameter search after structural change is now
implemented, as is deterministic diverse-population initialization. Speciation
is not yet implemented. Topology-age protection has now been implemented and
measured as an optional strategy; the first configuration stabilized weak runs
but did not improve the pass rate.

## First result beyond the nonlinear plateau

Starting every run from identical two-node cells produced 0/4 passes in a
100-generation measurement. Local refinement alone produced 0/2. Initializing
16 executable random architectures (10--14 internal nodes) and applying local
refinement produced 2/4 passes in 80 generations. The best measured ISE was
**1.01966**, below the **1.26031** memoryless floor; its graph retained seven
stateful nodes. This is evidence of learned dynamics, although not yet a robust
solution across seeds and still far from the exact reference.

For the current strongest repeatable experiment:

```shell
python scripts/benchmark_report.py --benchmark narendra --initialization dynamic \
  --refine --refine-min 12 --refine-patience 8 --refine-max 50 \
  --generations 80 --population 16 --restarts 4 --node-penalty 0 \
  --simulation-workers 4 --no-show
```

Restarts are explicit because the observed per-run success rate is only about
half. AMEBA is now escaping the static plateau, but topology preservation is the
next search problem rather than something the current result hides.

### Topology-age protection experiment

The engine can reserve slots for distinct young topologies for a finite grace
window and select those lineages as parents at a controlled rate. Parameter-only
changes age the current topology; structural changes reset its age. Ages are
stored in checkpoints, so protection remains deterministic across resume.

Using five grace generations, four protected slots, and a 25% protected-parent
rate on the same four seeds gave:

| seed | unprotected ISE | protected ISE | protected verdict |
|---:|---:|---:|---|
| 0 | 1.252880 | 1.252880 | pass |
| 1 | 1.260308 | 1.260309 | floor |
| 2 | **1.019659** | 1.224216 | pass |
| 3 | 1.416696 | 1.260308 | floor |

Both configurations passed 2/4 runs. Protection pulled the worst run back to
the plateau, but it also weakened the best run because four of sixteen slots
were unavailable to pure fitness competition. It therefore remains off by
default. The measured configuration can be reproduced by adding:

```shell
--protect-topologies 5 --protected-slots 4 --protected-parent-rate 0.25
```

This result narrows the next step: survival time alone is insufficient. A
species or quality-diversity mechanism needs to protect *productive structural
behavior*, not every young topology equally.

### Parallel generation simulation

Population scoring supports a persistent process pool. On the development
machine, a 20-generation run with 48 candidates and 300 simulated steps took
4.115 seconds with one worker and 3.135 seconds with four workers, a **1.31x**
speedup. Scores, graphs, topology ages, and seeded evolution were identical.

The gain is deliberately modest when refinement is enabled: final population
simulation is parallel, while each candidate's iterative local refinement still
runs in the coordinator. Independent restart/island parallelism is the next
larger source of concurrency.

## Operator prerequisites

The Narendra plant needs division, which the Python operator set did not have.
It is provided as a unary `reciprocal` rather than a binary `divide`, so that
every multi-input operator stays commutative: operator arguments are ordered by
edge identifier, and a structural mutation is free to change identifiers. A
non-commutative binary operator would silently swap numerator and denominator
when an edge was rewired.

`reciprocal` raises on a zero input, which the simulator converts into a
rejected candidate scoring infinity — the explicit zero policy the Java
reproduction audit asked for.

Squaring needs a relay node. A node cannot feed the same target twice, so one
factor reaches the product through a single-input `add` acting as a unit-gain
relay.

## Locking the model interface

`seed_graph()` structure-locks its input and output nodes. Without the locks, an
unconnected interface node is a free removal target for `RemoveNode`, and no
mutation can create an input or output node — so the search could permanently
discard the terminals it is scored on. This was observed before the locks were
added: evolution deleted the regressors it needed.

Constants are a related one-way loss, so `benchmark_policy()` adds `constant` to
the evolvable operators, letting a removed bias term be recreated.
