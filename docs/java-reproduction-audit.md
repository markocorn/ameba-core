# Java reproduction audit

This audit covers all 52 Java mutation/crossover wrapper and interface files,
all parameter-operation implementations, both cell-level crossovers, and the
`FactoryReproduction` dispatch path in the archived Java implementation.

The audit is static because a Java/Maven toolchain is not available in the
current environment. The conclusions below distinguish reusable evolutionary
ideas from Java behavior that should not become Python compatibility behavior.

## Executive conclusion

The Java reproduction system is not reliable enough to port behavior-for-behavior.
Some local numeric operations are sound, but dispatch bugs mean the node and edge
"crossovers" are not cross-parent operations. Several structural operators have
invalid-candidate paths, unsafe empty selections, unseeded randomness, or range
errors. The outer generation loop hides these failures by discarding failed
children and retrying without a retry limit.

Python should preserve operator intent, not accidental Java behavior.

## Dispatch-level findings

### Critical

- Edge crossover selects both edges from the first-parent clone. Node crossover
  likewise selects both nodes from the first-parent clone. The second parent is
  not used by either family.
- Both selections are made independently, so the same edge or node can be chosen
  twice and produce a self-crossover or no-op.
- The public `repCrossCell` helper clones `parent1`, applies crossover to the
  original parents, ignores the returned graph, and returns the unchanged clone.
- Locked-node handling creates a temporary mutation bag, tries to remove a list
  object from a list of strings, never uses the temporary bag, and can still pick
  `randCell`, discarding locked structure.

### High

- Node mutation/crossover first checks that a node has at least one unlocked
  parameter, then selects from all parameters and writes through the ordinary
  setter. A locked parameter can therefore be changed.
- Failed reproduction is caught in `Incubator`, the population index is
  decremented, and the operation is retried without a failure budget. A broken or
  impossible configuration can loop indefinitely.
- Probability bags are not validated. Empty bags fail at selection time; very
  large integer weights allocate correspondingly large lists.

## Parameter and weight operations

| Family | Java status | Python decision |
|---|---|---|
| Decimal additive mutation | Sound basic idea | Keep with explicit bounds |
| Integer additive mutation | Upper bound is exclusive; equal limits crash | Redesign with validated inclusive bounds |
| Decimal random replacement | Sound basic idea | Keep |
| Integer random replacement | Upper bound is exclusive; equal limits crash | Redesign |
| Sign inversion | Sound basic idea | Keep |
| Reciprocal mutation | Zero becomes infinity and then saturates | Keep only with explicit zero policy |
| Boolean add/random mutation | Both simply flip; the random generator is unused | Replace with one explicit boolean flip |
| Copy/average crossover | Useful when values come from different parents | Keep |
| Add/subtract/multiply crossover | Can saturate heavily; integer overflow occurs before clamping | Optional, with safe arithmetic |
| Divide crossover | Zero and NaN map implicitly to upper limits | Redesign with explicit invalid-result handling |
| Edge weight wrappers | Mutate the edge correctly | Keep the concepts, use policy-owned attributes |
| Edge crossover wrappers | Mutate `edge1` but return `null` | Replace with value-returning operations |

## Structural mutations

| Java operator | Assessment | Python direction |
|---|---|---|
| `AddEdge` | Useful intent; contains a redundant second source lookup and relies on final validation | Keep transactionally |
| `AddNode1` | Replaces an edge source, then fills new inputs randomly; error path dereferences an edge type that is always `null` | Redesign |
| `AddNode2` | Moves the old edge target to the new node and relies on unrelated random reconnection for the displaced target | Do not port directly |
| `AddNode3` | Clear serial edge split | Keep; implemented as transactional `SplitEdge` |
| `AddNode4` | Useful add-and-connect concept but can partially mutate before failure and can create invalid topology | Redesign through policy transactions |
| `AddNodesGroup` | Default construction requests 50 inner nodes in a cell capped at 50 total nodes; group-size expression uses `+ max` instead of `+ min` | Reject Java implementation; redesign |
| `MoveSourceEdge` | Useful topology mutation | Keep with candidate prevalidation |
| `MoveTargetEdge` | Useful topology mutation; empty unlocked list fails before its null handling | Keep with candidate prevalidation |
| `RandCell` | Population injection, not a local mutation; can violate lock expectations | Move to population strategy |
| `RemoveEdge` | Useful and guarded by minimum-input checks | Keep transactionally |
| `RemoveNode` | Reconnects outgoing edges randomly; abandoned collectors retain stale references until collection | Consolidate into explicit removal policies |
| `RemoveNode1` | Tests all inner nodes but samples the fully-unlocked list, which may be empty | Do not port directly |
| `RemoveNode2` | Samples without an empty guard and bypasses normal edge-removal bookkeeping | Do not port directly |
| `RemoveNodesGroup` | Useful subgraph-removal intent; uses unseeded shuffles and complex in-place reconnection | Redesign with induced-subgraph mapping |
| `ReplaceNode` | Useful intent; in-place reconnection leaves old collector bookkeeping and has nullable fallback paths | Redesign transactionally |
| `SwitchEdgesSources` | Useful intent; uses unseeded shuffle | Keep with injected RNG and validation |
| `SwitchEdgesTargets` | Checks total edge count instead of unlocked-target edge count; can index an undersized list | Redesign |

## Cell-level crossovers

| Java operator | Assessment | Python direction |
|---|---|---|
| `AddNodes` | Genuine second-parent subgraph transfer, but capacity/empty cases are unchecked and reconnection is in-place | Keep concept; clone and remap an induced subgraph |
| `TransferNodes` | Genuine replacement crossover, but uses unseeded shuffles, unchecked empty groups, and fragile border-edge bookkeeping | Keep concept; redesign as typed subgraph replacement |

## Reproducibility problems

The implementation injects seeded `Random` instances in many classes, but also
uses `Collections.shuffle(list)` without the injected generator in cell grouping,
node replacement, edge switching, group removal, group crossover, and factory
reconnection. Identical top-level seeds therefore do not guarantee identical
runs.

## Compatibility policy

The Python rewrite should classify migration tests as follows:

1. **Mathematical parity:** retain well-defined parameter formulas and node
   equations where inputs are valid.
2. **Intent parity:** implement structural concepts such as edge splitting,
   subgraph insertion, and subgraph replacement using transactional graph copies
   and policy validation.
3. **No parity:** do not reproduce ignored second parents, locked-parameter
   mutation, unseeded randomness, indefinite retries, stale bookkeeping, invalid
   range calculations, or implicit NaN/infinity saturation.

Every Python reproduction operation should satisfy invariant tests: parents are
unchanged, identifiers remain unique, all endpoints exist, domain policy accepts
the child, locks are respected, seeded runs repeat exactly, and failure leaves no
partially modified graph.

## Python replacement status

Implemented and enabled in the default CLI portfolio:

- bounded, policy-owned node-parameter and edge-weight mutation;
- transactional edge insertion, removal, source movement, and target movement;
- transactional edge splitting, node replacement, plain removal, and bypass
  removal;
- aligned attribute crossover that actually reads both parents;
- structural node/edge-set crossover with primary-parent lock preservation;
- the `AddNodes` concept as `InducedSubgraphInsertionCrossover`, which clones a
  connected donor subgraph under fresh identifiers and recreates its boundary
  through policy-typed attachment points;
- the `TransferNodes` concept as `TypedSubgraphReplacementCrossover`, which
  replaces a connected primary subgraph only when every boundary edge maps to a
  donor boundary edge of the same direction and connection type;
- explicit structure, endpoint, and attribute locks in the versioned graph
  schema; and
- finite, seeded mutation and crossover portfolios that try alternatives after
  an operator is inapplicable.

The Python tests cover parent immutability, lock behavior, policy validation,
locked-edge endpoint retention, seeded repeatability, and exact checkpoint
resume. Candidate identifiers are sorted before seeded shuffling so a graph
serialization round trip cannot alter the later random sequence.

Both subgraph crossovers reach the child only through the typed boundary
contract (`GraphPolicy.can_transfer_node` and `GraphPolicy.connection_type`), so
the Java reconnection fragility is not reproduced: an unmatched boundary aborts
the attempt instead of rewiring arbitrarily. In the signal domain that contract
keeps input, output, and constant identities outside transferable subgraphs.

Population injection (`RandCell`) remains intentionally unimplemented. It is
reserved for a future population strategy rather than represented as a local
graph mutation.
