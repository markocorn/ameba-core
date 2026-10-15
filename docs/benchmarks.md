# Identification benchmarks

## The setup

A plant is driven by a control sequence u(k) and the model is scored on
predicting y(k+1) while seeing **only u(k)**. The model must carry its own state
through delay or filter nodes, so its prediction error feeds back into the next
step. Most rungs are single-input/single-output; the quadruple tank is
two-input/two-output, and nothing else about the setup changes at that width —
one dataset, one evaluator, one score summed over the output channels.

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

### The multi-channel form, and where it stops working

For several inputs and outputs the same idea is `memoryless_floor(dataset)`: a
static linear map fitted from every input at one step to every output at the
next, which is exactly optimal for ISE. It reduces to the single gain when there
is one channel each.

It is a **weaker bar**, because two conditions that make the linear fit optimal
are assumed rather than checked:

* the controls must be independent, and
* the plant's static relation must be linear.

The tank breaks both. Its input is deliberately held for ten samples, so it is
autocorrelated, and its level varies as the square of flow. Measured over seeds
1–3 at 150 steps, 12 candidates and 20 generations, a search restricted to
stateless operators reaches **0.62** of the floor on the minimum-phase setting
and **0.31** on the non-minimum-phase one. A memoryless model therefore gets a
third to two thirds of the way to the floor on this plant, so scoring below it
does **not** demonstrate that state was used. The measurement is pinned by
`tests/test_mimo_benchmark.py::MemorylessStrictnessTests`.

### Where the closed form fails, search for the baseline

`stateless_floor(dataset, …)` establishes the memoryless bar by search instead of
by assumption: evolution is restricted to operators that carry no state, and the
best score it reaches *is* the floor. Nothing is assumed about the plant, and the
engine, dataset, criterion and time step are the ones the judged run used, so the
two are comparable. Three independent seeds are searched and the best kept, which
raises the bar and so errs toward *not* claiming dynamics.

Each `Benchmark` says which bar applies through `linear_floor_is_strict`. Where
it is `False` the report scripts take the **stricter of the two**, because a
closed-form optimum is itself reachable without state and so remains a legitimate
memoryless score — a search that fails to reach it is under-searched rather than
reassuring. `--no-stateless-baseline` in `benchmark_report.py` falls back to the
closed form; in `archive_search.py` the baseline is automatic for a benchmark
that needs it and is requested explicitly with `--stateless-baseline` for
`--dataset`, where no benchmark record exists to say so.

Measured with the baseline defaults on every registered rung:

| benchmark | closed form | memoryless search | ratio | criterion |
|---|---|---|---|---|
| `linear` | 9.1994 | 8.5724 | 0.93 | closed form |
| `narendra` | 1.2603 | 1.2621 | 1.00 | closed form |
| `tank-minphase` | 132.4007 | 81.8809 | 0.62 | **memoryless search** |
| `tank-nonminphase` | 117.3226 | 36.8379 | 0.31 | **memoryless search** |

Both numbers are in-sample on the same data, so this compares two estimators
rather than measuring either against a held-out truth. That is what the small
gaps mean: least squares is optimal among *linear* maps, but a flexible search
can still fit its sample better, so a few percent below the closed form on an
independent white-noise input is the search overfitting rather than the closed
form having the wrong shape. Narendra's 1.00 is the control case, where the
search finds nothing better at all. The tank's factor of two is not that.

The change alters verdicts rather than decoration. On `tank-nonminphase` at 150
steps, 20 generations and 12 candidates, the evolved model scores 358.8 — below
the closed-form floor of 453.99, which the old criterion would have called
dynamics identified, and well above the memoryless baseline of 181.24, which is
the correct answer.

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

### Rung 3 — quadruple tank (`benchmarks.quadruple_tank`)

Johansson's quadruple-tank process: two pumps, four tanks, two measured levels.
Each pump reaches each measured level by two routes — one fast and direct, one
slow and over the top — and the valve split decides which dominates. Outflow goes
as the square root of level, so this is not a linear system wearing a MIMO shape.

Two valve settings are registered:

| benchmark | split | transmission zero | character |
|---|---|---|---|
| `tank-minphase` | g₁ + g₂ = 1.30 | left half plane | each output moves straight to where its pump drives it |
| `tank-nonminphase` | g₁ + g₂ = 0.77 | right half plane | each output first moves *away* from where its pump drives it |

The non-minimum-phase setting is the point of the rung. Its inverse response
cannot be produced by a smoothed gain: it needs two routes with different delays,
which is the structure identification is supposed to find.

Neither setting has a reference graph. The linear and Narendra rungs have one
that reproduces its plant to floating-point noise, and that is the strongest
evidence available for those rungs; an exact tank model would need the
square-root outflow fed back through an integrator. Until that exists the plant
is evidenced by its own physics, which `tests/test_quadruple_tank.py` checks
directly: the equilibrium is a fixed point, tank 3 drains exactly what pump 2
routes over the top, integration has converged, and the non-minimum-phase
configuration shows the inverse response its zero predicts.

**No standing search result exists for this rung.** It is registered, tested, and
reproducible from `scripts/make_mimo_dataset.py --plant tank-minphase` (or
`tank-nonminphase`); it has not yet been measured. Its pass criterion is the
memoryless search baseline rather than the closed-form floor — see "Where the
closed form fails, search for the baseline" above.

## Standing results

Both reference graphs reproduce their plant to floating-point noise (~1e-30 SSE)
across the training input, a sinusoid, and a step sequence. That is the evidence
that delay-based feedback is implemented correctly.

| Benchmark | Seed | Memoryless floor | Evolution | Verdict |
|---|---|---|---|---|
| Linear 3rd order | 40.93 | 9.20 | 0.02 – 0.70 | **solved** |
| Narendra | 4.25 | 1.2603 | 1.2603 | **open** |

The tank rungs are registered and tested but not yet measured, so they do not
appear above. Their closed-form floors at the recorded settings — 300 steps,
dt 3.0 — are 223.24 for `tank-minphase` and 188.86 for `tank-nonminphase`, but
the criterion on those rungs is the memoryless search baseline rather than the
form, and no run has been scored against that yet.

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
