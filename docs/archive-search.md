# Archive search

A second search method, alongside the population engine. It exists because the
population engine could not pass the Narendra rung: a structurally new model is
scored the instant it is born, while its weights are still whatever the mutation
produced, so it loses to a tuned incumbent and its shape is never seen again.
The measured consequence is in [benchmarks.md](benchmarks.md) — evolution
converged to the memoryless floor to four decimal places on every seed tried.

The archive replaces ranking with **local competition**. It holds a fixed number
of models that fail differently from one another, and a candidate is compared
only against its nearest neighbour: close to it, they are the same idea and the
better one keeps the slot; far from it, the candidate is a new idea and takes a
slot from the worst member. Parameter mutation stops being a separate refinement
phase, because a retuned copy is near its own parent and so competes only with
it. Local search becomes something the archive does to itself over time.

`ameba_graph.Archive` is domain-neutral: it never inspects a graph, and whoever
supplies the descriptor decides what "different" means. `ameba_signal` supplies
the signal-domain descriptor; `scripts/archive_search.py` is the driver.

## Standing results

The Narendra plant, 150 steps, memoryless floor 1.26031. Scores are ISE; the
ratio is what matters, and below 1.0 means the model is using state.

| run | configuration | below floor | median | best |
|---|---|---|---|---|
| E2 | baseline, 12000 batches | — | — | **0.1947** |
| E15 | immigrants + cap, 12000 batches | — | — | 0.3057 |
| E1 | baseline, 3000 x 8 seeds | 7/8 | 0.9613 | 0.2589 |
| E10 | immigrants + cap, 3000 x 4 seeds | 4/4 | **0.8554** | 0.4329 |
| E9 | immigrants alone | 6/8 | 0.8971 | 0.7773 |
| E6 | size floor + threshold band | 6/8 | 0.9293 | 0.7798 |
| E3 | niche-scoped parsimony | 2/8 | 1.0000 | 0.8179 |
| E13 | structure-only, threshold 0.05 | 3/8 | 1.0000 | 0.3903 |
| E14 | structure-only, threshold 0.15 | 0/5 | 1.0000 | 1.0469 |
| E16 | structure-only, capacity 50 | 0/4 | 1.0000 | 1.0000 |
| E4 | local competition disabled | 0/8 | 10.7628 | 1.0127 |

The population engine's best on this plant was 1.01966 (0.809 x floor), passing
2 of 4 seeds. The archive's best is **0.245355 (0.1947 x floor)**, and its most
reliable configuration clears the floor in 4 of 4 seeds.

Neither result is a solution. The reference graph reproduces the plant at ~1e-31,
so a score of 0.245 is fitting some dynamics, not identifying the plant's. The
archive has solved *escaping the plateau*; it has not solved identification.

## What is established

### Local competition is the engine, not an enhancement

Disabling in-place replacement (`--no-local-competition`) gives **0/8 below the
floor and a median 10.76x above it** — six of eight runs finished worse than
where a normal run stands after 75 batches. The archives froze outright: `new`
and `tuned` both zero for 3000 consecutive batches, members unchanged from
initialization.

The mechanism is a feedback loop. With same-niche candidates dropped instead of
absorbed, members never consolidate, so pairwise distances stay large, so the
adaptive threshold inflates, so even more candidates read as same-niche and are
dropped. It locks solid. Novelty admission alone does nothing.

### Every form of parsimony pressure is fatal

Two independent mechanisms, both catastrophic:

* A relative node penalty of 1e-3 on the competition score turned a 0.326 run
  into a 2-node graph scoring exactly 1.26031 — the memoryless floor.
* A niche-scoped size tiebreak of 0.02, built specifically on the theory that
  niche scoping made parsimony safe, collapsed **six of eight** runs to a 2-node
  graph at exactly the floor.

The second refuted the theory behind it. The reasoning missed that early in a
run *every* poor model resembles the static one — their residuals all track the
target — so they share its niche, and the tiebreak shrinks the whole archive
into it before any structure can grow.

Size is the search's working material, not waste. The only guard that makes
sense is a lower bound (`min_nodes`), never a charge.

### Growth predicts success, but only alongside tuning

Across 8 seeds, final node count against final score gave a rank correlation of
**-0.857** (significant at n=8). The split was stark: runs ending at 22 or more
nodes scored 0.326, 1.017, 1.178, 1.245; runs under 22 nodes scored 1.100,
1.247, 1.247, 1.260.

But growth alone buys nothing. Structure-only niching produced a **108-node
graph that finished above the floor** with `tuned` at zero throughout. Size pays
only when in-place tuning accompanies it.

### A stalled run never recovers

One run sat at exactly 1.26031 from batch 900 to batch 12000 — **11,100
consecutive batches** without movement. Another was unchanged from batch 3000 to
12000. Extending a dead run is wasted compute; `--stall-patience` abandons it
and re-spends the budget from a fresh initialization instead.

Conversely, extending a *live* run pays: 0.326 to 0.245 between 3000 and 12000
batches, and a second run improved 9.9% over the same stretch while still
descending when it stopped.

### An adaptive threshold cancels out a change of metric

Adding a structural axis to the distance raised the median admission distance
as intended — and raised the adaptive threshold with it, from 0.2946 to 0.4043,
because the threshold is computed from the same pairwise distances. Admissions
were unchanged (31/78 vs 28/78).

**A metric change cannot help while the threshold is derived from that metric.**
This likely explains why the threshold band did so little as well. Exploiting a
descriptor change requires a fixed or bounded threshold.

### The behavioural descriptor is blind to structural change

Median nearest-neighbour distance, measured against a live archive:

| candidate | behaviour | composition | WL depth 1 | WL depth 2 |
|---|---|---|---|---|
| unrelated immigrant | 0.0110 | 0.2150 | **0.6786** | 0.7814 |
| structural mutant | **0.0000** | 0.0333 | **0.0785** | 0.2175 |
| retuned copy | 0.0000 | 0.0000 | 0.0000 | 0.0000 |

A structural mutation produces a behaviourally *identical* model in the median
case. The archive therefore assigns it to its parent's niche, where it must beat
a tuned incumbent while carrying untuned weights — the exact failure the archive
exists to prevent, reappearing inside it.

Weisfeiler-Lehman depth 1 separates best: 8.6x between "variant of something"
and "unrelated", against 6.5x for a bag of kinds and 3.6x for depth 2. Depth 2
over-discriminates, inflating ordinary variants toward "unrelated".

### But structural niching still failed

Despite that, using structure for niche assignment gave **0/5 below the floor**
with `tuned` at zero and `new` running 4-14 per interval — constant churn, no
consolidation. The cause is capacity: the archive holds 10 slots, structure-only
makes every structural mutant its own niche, the slots fill with ten different
structures, and each receives a tenth of the tuning effort.

The descriptor measurement is sound; the application is not. Whether a larger
archive rescues it is untested.

### Random immigrants are behaviourally near everything

A graph generated from scratch, sharing no structure with any member, sits at a
median behavioural distance of **0.010** from its nearest neighbour — the same
effect that makes the plateau a single niche. Immigration therefore only works
when the threshold is capped: uncapped it admits almost nothing new
(`--immigrant-rate 0.1` alone, 6/8, best 0.777), capped at 0.3 it produced the
most reliable configuration measured (4/4, median 0.8554).

Immigration and a threshold cap are complements. Neither did much alone.

## The configuration to start from

```shell
python scripts/archive_search.py --benchmark narendra --batches 3000 --batch 16 \
  --workers 12 --parameter-rate 0.3 --seeds 8 \
  --immigrant-rate 0.1 --threshold-max 0.3
```

Defaults worth knowing, all measured rather than assumed:

| setting | value | why |
|---|---|---|
| `--parameter-rate` | 0.3 | exploration beats tuning at the margin; 0.8 degrades toward the plateau |
| `--node-penalty` | 0 | any size charge collapses the search |
| `--size-tiebreak` | 0 | refuted; see above |
| `--structure-rule` | off | good descriptor, failed application |
| `--probation` | 5 | at most `capacity - 2`, or a full archive protects every slot at once |
| capacity | 10 | untested above this; the constraint on structural niching |

Parents are drawn **uniformly** from the archive, never by score. Biasing toward
the best member is how the archive would collapse back onto the plateau it
exists to escape.

## Open questions

**Is autoregression the distinguishing property?** The best model found (0.245)
feeds its own output-driving node back through delays, with 38 of 46 nodes on a
feedback loop — the same shape as the reference plant (13 of 17). The
second-best (0.385) does not: only 5 of 80 nodes lie on any loop, making it
essentially a feedforward function of input history, which can approximate the
recursive plant but never equal it. This is n=2 and unmeasured at scale. If it
holds, autoregression is a far better niching axis than generic structural
distance, because it is one meaningful bit rather than an undifferentiated
distance.

**Does a larger archive rescue structural niching?** The capacity diagnosis
predicts that 50 slots would let ten structures coexist *and* be tuned. Untested.

**Would binned niches beat distance-plus-threshold?** Every calibration failure
recorded here is an artifact of a threshold: the adaptive one cancels metric
changes, the fixed one needs hand-tuning per metric. MAP-Elites-style bins over
a small number of meaningful axes have no threshold to calibrate.

**Best-of-N favours variance, not mean.** Every guard tried so far raises the
median and lowers the ceiling. Since the output of a search is its best run, a
configuration that reliably reaches the growth mode matters more than one with a
good median — which is why the immigrants-plus-cap result is interesting beyond
its median.
