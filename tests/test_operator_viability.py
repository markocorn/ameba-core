"""Does every mutation and crossover produce a cell that still carries a signal?

This is the reproduction counterpart to the generation checks. It is not about
fitness: a child may be far worse than its parent and still be a perfectly good
cell. The question is only whether the operators produce cells that work --
structurally valid, and executable, so a signal can actually pass through.

Parents are cells that are known to run, so any failure belongs to the operator
rather than to the starting point. Every operator is measured on four counts:

* it either succeeds or declines cleanly, never raising something unexpected;
* the child is accepted by the domain policy;
* the child executes, carrying the probe signal from input to output;
* the parents come back unchanged.
"""

import unittest
from random import Random

from ameba_graph import GenerationConfig, GraphGenerator
from ameba_graph.crossover import (
    AlignedAttributeCrossover,
    CrossoverError,
    CrossoverPortfolio,
    InducedSubgraphInsertionCrossover,
    TypedSubgraphReplacementCrossover,
    UniformGraphCrossover,
)
from ameba_graph.mutation import (
    AddEdge,
    AddNode,
    MoveEdgeSource,
    MoveEdgeTarget,
    MutateEdgeAttributes,
    MutateNodeAttributes,
    MutationError,
    MutationPortfolio,
    RandomMutation,
    RemoveEdge,
    RemoveNode,
    RemoveNodeBypass,
    ReplaceNode,
    SplitEdge,
)
from ameba_graph.serialization import graph_dumps
from ameba_signal import (
    SignalSimulationError,
    SignalSimulator,
    interface_scaffold,
    simulates,
)
from ameba.benchmarks import benchmark_policy

PROBE = [(0.5, -0.3), (0.1, 0.7), (-0.4, 0.2)] * 8

MUTATIONS = (
    AddNode,
    RemoveNode,
    AddEdge,
    RemoveEdge,
    SplitEdge,
    MutateNodeAttributes,
    MutateEdgeAttributes,
    MoveEdgeSource,
    MoveEdgeTarget,
    ReplaceNode,
    RemoveNodeBypass,
)
CROSSOVERS = (
    UniformGraphCrossover,
    AlignedAttributeCrossover,
    InducedSubgraphInsertionCrossover,
    TypedSubgraphReplacementCrossover,
)


def working_cells(count: int, policy, size: int = 8) -> list:
    """Cells that are known to run, so failures belong to the operator."""
    scaffold = interface_scaffold(2, 2)
    generator = GraphGenerator(
        policy,
        GenerationConfig(min_nodes=size, max_nodes=size, edge_probability=0.2, attempts=40),
    )
    return [
        generator.generate(
            Random(seed), scaffold, accept=lambda candidate: simulates(candidate, PROBE)
        )
        for seed in range(count)
    ]


#: Failures that would mean the graph itself is malformed, as opposed to a
#: value straying outside an operator's domain. Listing what must never happen
#: keeps this honest as operators are added: a new domain error such as
#: "log is undefined" is fine, a missing value never is.
STRUCTURAL_FAULTS = (
    "unavailable in the current time step",
    "Unsupported signal operator",
    "has no output nodes",
    "algebraic cycle",
    "Missing input at index",
    "incompatible state",
    "Invalid stateful operator configuration",
)


def assert_not_structural(case, name: str, cause: str) -> None:
    for marker in STRUCTURAL_FAULTS:
        case.assertNotIn(marker, cause, f"{name} failed structurally: {cause}")


class Outcome:
    """Tally of what an operator did over many attempts."""

    def __init__(self) -> None:
        self.applied = 0
        self.declined = 0
        self.invalid = 0
        self.dead = 0
        self.disturbed = 0
        self.causes: list[str] = []

    @property
    def viable(self) -> int:
        return self.applied - self.invalid - self.dead


class MutationViabilityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = benchmark_policy()
        cls.parents = working_cells(24, cls.policy)

    def exercise(self, operator) -> Outcome:
        outcome = Outcome()
        simulator = SignalSimulator()
        for index, parent in enumerate(self.parents):
            for attempt in range(4):
                before = graph_dumps(parent)
                try:
                    child = operator.mutate(parent, self.policy, Random(index * 7 + attempt))
                except MutationError:
                    outcome.declined += 1
                    continue
                outcome.applied += 1
                if graph_dumps(parent) != before:
                    outcome.disturbed += 1
                try:
                    self.policy.validate(child)
                except Exception:  # noqa: BLE001 - any rejection is a failure here
                    outcome.invalid += 1
                    continue
                try:
                    simulator.run_series(child, PROBE)
                except SignalSimulationError as exc:
                    outcome.dead += 1
                    outcome.causes.append(str(exc))
        return outcome

    def test_every_mutation_produces_a_cell_that_carries_a_signal(self) -> None:
        for factory in MUTATIONS:
            with self.subTest(mutation=factory.__name__):
                outcome = self.exercise(factory())
                self.assertGreater(outcome.applied, 0, "operator never applied")
                self.assertEqual(0, outcome.invalid, "produced a policy-invalid cell")
                self.assertEqual(0, outcome.disturbed, "modified its parent")
                # Some children are structurally fine but numerically
                # degenerate. Measured viability is 96.9%-100% across the
                # mutations, so the bar sits below that with room to spare
                # rather than at a rate nothing actually achieves.
                self.assertGreater(
                    outcome.viable / outcome.applied,
                    0.85,
                    f"{outcome.dead}/{outcome.applied} children could not run",
                )

    def test_a_failed_child_is_only_ever_numerically_degenerate(self) -> None:
        """Not a structural fault: never a missing value or an unknown operator."""
        for factory in MUTATIONS:
            for cause in self.exercise(factory()).causes:
                assert_not_structural(self, factory.__name__, cause)

    def test_add_node_connects_what_it_adds(self) -> None:
        """Regression: an unconnected new node could never satisfy an arity minimum."""
        outcome = self.exercise(AddNode())
        self.assertEqual(0, outcome.declined, "AddNode should always be applicable")

        parent = self.parents[0]
        child = AddNode().mutate(parent, self.policy, Random(1))
        added = set(child.nodes) - set(parent.nodes)
        self.assertEqual(1, len(added))
        self.assertTrue(child.incoming(added.pop()), "the new node was left unconnected")

    def test_composite_mutations_stay_viable(self) -> None:
        for operator in (
            RandomMutation([factory() for factory in MUTATIONS]),
            MutationPortfolio([factory() for factory in MUTATIONS]),
        ):
            with self.subTest(operator=type(operator).__name__):
                outcome = self.exercise(operator)
                self.assertGreater(outcome.applied, 0)
                self.assertEqual(0, outcome.invalid)
                self.assertEqual(0, outcome.disturbed)
                self.assertGreater(outcome.viable / outcome.applied, 0.85)

    def test_a_portfolio_applies_something_for_every_parent(self) -> None:
        outcome = self.exercise(MutationPortfolio([factory() for factory in MUTATIONS]))
        self.assertEqual(0, outcome.declined, "the portfolio should always find an operator")


class CrossoverViabilityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = benchmark_policy()
        cls.parents = working_cells(16, cls.policy)

    def exercise(self, operator) -> Outcome:
        outcome = Outcome()
        simulator = SignalSimulator()
        for index in range(len(self.parents) - 1):
            left, right = self.parents[index], self.parents[index + 1]
            for attempt in range(4):
                before = (graph_dumps(left), graph_dumps(right))
                try:
                    child = operator.cross(left, right, self.policy, Random(index * 7 + attempt))
                except CrossoverError:
                    outcome.declined += 1
                    continue
                outcome.applied += 1
                if (graph_dumps(left), graph_dumps(right)) != before:
                    outcome.disturbed += 1
                try:
                    self.policy.validate(child)
                except Exception:  # noqa: BLE001
                    outcome.invalid += 1
                    continue
                try:
                    simulator.run_series(child, PROBE)
                except SignalSimulationError as exc:
                    outcome.dead += 1
                    outcome.causes.append(str(exc))
        return outcome

    def test_every_crossover_produces_a_cell_that_carries_a_signal(self) -> None:
        for factory in CROSSOVERS:
            with self.subTest(crossover=factory.__name__):
                outcome = self.exercise(factory())
                self.assertGreater(outcome.applied, 0, "operator never applied")
                self.assertEqual(0, outcome.invalid, "produced a policy-invalid cell")
                self.assertEqual(0, outcome.disturbed, "modified a parent")
                # Subgraph insertion is the weakest at ~89%: it grafts donor
                # structure whose values the recipient has never seen.
                self.assertGreater(
                    outcome.viable / outcome.applied,
                    0.85,
                    f"{outcome.dead}/{outcome.applied} children could not run",
                )

    def test_a_failed_child_is_only_ever_numerically_degenerate(self) -> None:
        for factory in CROSSOVERS:
            for cause in self.exercise(factory()).causes:
                assert_not_structural(self, factory.__name__, cause)

    def test_crossing_a_cell_with_itself_never_corrupts_it(self) -> None:
        """A degenerate but legal case: both parents identical.

        Declining is a valid answer here. Subgraph insertion in particular
        cannot attach to a copy of its own recipient, because every external
        node already carries its full complement of inputs, so no outgoing
        boundary edge has anywhere to land. What must never happen is a child
        that is accepted but broken.
        """
        parent = self.parents[0]
        simulator = SignalSimulator()
        for factory in CROSSOVERS:
            with self.subTest(crossover=factory.__name__):
                before = graph_dumps(parent)
                for seed in range(12):
                    try:
                        child = factory().cross(parent, parent, self.policy, Random(seed))
                    except CrossoverError:
                        continue
                    self.policy.validate(child)
                    try:
                        simulator.run_series(child, PROBE)
                    except SignalSimulationError as exc:
                        assert_not_structural(self, factory.__name__, str(exc))
                self.assertEqual(before, graph_dumps(parent), "parent was modified")

    def test_the_portfolio_stays_viable(self) -> None:
        outcome = self.exercise(CrossoverPortfolio([factory() for factory in CROSSOVERS]))
        self.assertGreater(outcome.applied, 0)
        self.assertEqual(0, outcome.invalid)
        self.assertEqual(0, outcome.disturbed)
        self.assertGreater(outcome.viable / outcome.applied, 0.85)


class OperatorCoverageTests(unittest.TestCase):
    def test_every_operator_in_the_modules_is_exercised(self) -> None:
        """A new operator must be added to the lists above, not slip through."""
        import ameba_graph.crossover as crossover_module
        import ameba_graph.mutation as mutation_module

        def concrete(module, method: str, skip: set[str]) -> set[str]:
            return {
                name
                for name in dir(module)
                if not name.startswith("_")
                and isinstance(value := getattr(module, name), type)
                # Defined here, so imported protocols and errors are excluded.
                and value.__module__ == module.__name__
                and hasattr(value, method)
                and name not in skip
            }

        self.assertEqual(
            concrete(mutation_module, "mutate", {"RandomMutation", "MutationPortfolio"}),
            {factory.__name__ for factory in MUTATIONS},
        )
        self.assertEqual(
            concrete(crossover_module, "cross", {"CrossoverPortfolio"}),
            {factory.__name__ for factory in CROSSOVERS},
        )


if __name__ == "__main__":
    unittest.main()
