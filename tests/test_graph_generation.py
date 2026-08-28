"""Random initial graph generation.

The generic generator is exercised against both a permissive domain and the
real signal domain. Signal graphs are generated around a fixed interface
scaffold: how many inputs and outputs a model has is part of the identification
problem, so the terminals are supplied rather than discovered.
"""

import unittest
from random import Random

from ameba_graph import (
    Edge,
    GenerationConfig,
    Graph,
    GraphGenerationError,
    GraphGenerator,
    Node,
)
from ameba_signal import (
    SignalGraphPolicy,
    SignalSimulationError,
    SignalSimulator,
    interface_scaffold,
    simulates,
)
from ameba_signal.operators import OPERATOR_ARITY


class PermissivePolicy:
    """A domain that accepts any structure, to test the generator in isolation."""

    def create_node(self, rng: Random) -> Node:
        return Node("ignored", rng.choice(("red", "blue")), {"size": rng.randint(1, 4)})

    def create_edge(self, source: str, target: str, rng: Random) -> Edge:
        return Edge("ignored", source, target, {"weight": rng.uniform(-1.0, 1.0)})

    def can_connect(self, graph: Graph, source: str, target: str) -> bool:
        return source != target

    def validate(self, graph: Graph) -> None:
        graph.validate_structure()

    def mutate_node(self, node: Node, rng: Random) -> Node:
        return node

    def mutate_edge(self, edge: Edge, rng: Random) -> Edge:
        return edge

    def cross_node(self, left: Node, right: Node, rng: Random) -> Node:
        return left

    def cross_edge(self, left: Edge, right: Edge, rng: Random) -> Edge:
        return left

    def can_transfer_node(self, node: Node) -> bool:
        return True

    def connection_type(self, graph: Graph, source: str, target: str) -> str:
        return "any"


class GeneratorMechanicsTests(unittest.TestCase):
    def setUp(self) -> None:
        self.policy = PermissivePolicy()

    def test_node_count_stays_inside_the_configured_range(self) -> None:
        generator = GraphGenerator(self.policy, GenerationConfig(min_nodes=4, max_nodes=9))
        for seed in range(50):
            graph = generator.generate(Random(seed))
            self.assertGreaterEqual(len(graph.nodes), 4)
            self.assertLessEqual(len(graph.nodes), 9)

    def test_generation_is_reproducible_for_a_given_seed(self) -> None:
        generator = GraphGenerator(self.policy, GenerationConfig(min_nodes=10, max_nodes=10))
        for seed in (1, 7, 99):
            first = generator.generate(Random(seed))
            second = generator.generate(Random(seed))
            self.assertEqual(first.nodes, second.nodes)
            self.assertEqual(first.edges, second.edges)

    def test_identifiers_are_unique_and_endpoints_exist(self) -> None:
        generator = GraphGenerator(self.policy, GenerationConfig(min_nodes=10, max_nodes=10))
        for seed in range(30):
            graph = generator.generate(Random(seed))
            self.assertEqual(len(graph.nodes), len({node.id for node in graph.nodes.values()}))
            for edge in graph.edges.values():
                self.assertIn(edge.source, graph.nodes)
                self.assertIn(edge.target, graph.nodes)

    def test_zero_edge_probability_produces_no_random_edges(self) -> None:
        generator = GraphGenerator(
            self.policy,
            GenerationConfig(
                min_nodes=10, max_nodes=10, edge_probability=0.0, connect_isolated=False
            ),
        )
        self.assertEqual(0, len(generator.generate(Random(5)).edges))

    def test_the_repair_pass_connects_nodes_the_random_pass_skipped(self) -> None:
        """It runs after the random pass, so it adds edges even at probability zero."""
        generator = GraphGenerator(
            self.policy,
            GenerationConfig(
                min_nodes=10, max_nodes=10, edge_probability=0.0, connect_isolated=True
            ),
        )
        graph = generator.generate(Random(5))
        self.assertGreater(len(graph.edges), 0)
        # The fallback rule, used when a policy reports no input requirements,
        # is simply that a node should not be left unconnected.
        roots = [node_id for node_id in graph.nodes if not graph.incoming(node_id)]
        self.assertLessEqual(len(roots), 1)

    def test_edge_probability_controls_the_density(self) -> None:
        def density(probability: float) -> float:
            generator = GraphGenerator(
                self.policy,
                GenerationConfig(min_nodes=10, max_nodes=10, edge_probability=probability),
            )
            counts = [len(generator.generate(Random(seed)).edges) for seed in range(40)]
            return sum(counts) / len(counts)

        self.assertLess(density(0.1), density(0.4))
        self.assertLess(density(0.4), density(0.8))

    def test_a_policy_that_rejects_everything_exhausts_the_attempt_budget(self) -> None:
        class ImpossiblePolicy(PermissivePolicy):
            def validate(self, graph: Graph) -> None:
                raise ValueError("never acceptable")

        generator = GraphGenerator(ImpossiblePolicy(), GenerationConfig(attempts=3))
        with self.assertRaises(GraphGenerationError):
            generator.generate(Random(0))

    def test_invalid_configurations_are_rejected(self) -> None:
        with self.assertRaises(ValueError):
            GenerationConfig(min_nodes=5, max_nodes=2)
        with self.assertRaises(ValueError):
            GenerationConfig(edge_probability=1.5)
        with self.assertRaises(ValueError):
            GenerationConfig(attempts=0)


class SignalScaffoldTests(unittest.TestCase):
    def test_the_scaffold_fixes_the_interface_with_locked_terminals(self) -> None:
        for inputs, outputs in ((1, 1), (3, 2), (2, 4)):
            scaffold = interface_scaffold(inputs, outputs)
            kinds = [node.kind for node in scaffold.nodes.values()]
            self.assertEqual(inputs, kinds.count("input"))
            self.assertEqual(outputs, kinds.count("output"))
            self.assertTrue(all(node.locked for node in scaffold.nodes.values()))
            for kind, count in (("input", inputs), ("output", outputs)):
                indices = sorted(
                    node.attributes["index"]
                    for node in scaffold.nodes.values()
                    if node.kind == kind
                )
                self.assertEqual(list(range(count)), indices)

    def test_a_scaffold_needs_at_least_one_terminal_of_each_kind(self) -> None:
        for inputs, outputs in ((0, 1), (1, 0)):
            with self.assertRaises(ValueError):
                interface_scaffold(inputs, outputs)


class SignalDomainGenerationTests(unittest.TestCase):
    """Generation into the real signal domain, around a fixed interface."""

    def setUp(self) -> None:
        self.policy = SignalGraphPolicy()

    def _generator(self, size: int, attempts: int = 25, **kwargs) -> GraphGenerator:
        return GraphGenerator(
            self.policy,
            GenerationConfig(min_nodes=size, max_nodes=size, attempts=attempts, **kwargs),
        )

    def test_generation_succeeds_at_every_size(self) -> None:
        for size in (1, 3, 10, 20):
            scaffold = interface_scaffold(1, 1)
            generator = self._generator(size)
            for seed in range(20):
                graph = generator.generate(Random(seed), scaffold)
                self.policy.validate(graph)
                self.assertEqual(size + 2, len(graph.nodes), msg=f"size={size}")

    def test_generation_succeeds_for_mimo_interfaces(self) -> None:
        for inputs, outputs in ((1, 1), (2, 2), (3, 2), (2, 3)):
            scaffold = interface_scaffold(inputs, outputs)
            generator = self._generator(10)
            for seed in range(20):
                graph = generator.generate(Random(seed), scaffold)
                self.policy.validate(graph)
                kinds = [node.kind for node in graph.nodes.values()]
                self.assertEqual(inputs, kinds.count("input"))
                self.assertEqual(outputs, kinds.count("output"))

    def test_generated_graphs_run_in_the_simulator(self) -> None:
        """Validation is structural, so some candidates still fail numerically.

        A reciprocal that receives zero, or a feedback loop that diverges, is
        rejected at simulation time rather than by the policy. Evolution treats
        that as an infinite score, so it is expected -- but most candidates
        must actually run, or generation would be useless as a seed.
        """
        scaffold = interface_scaffold(2, 2)
        generator = self._generator(10)
        simulator = SignalSimulator()
        rows = [(0.5, -0.3)] * 20

        ran = 0
        for seed in range(60):
            graph = generator.generate(Random(seed), scaffold)
            try:
                produced = simulator.run_series(graph, rows)
            except SignalSimulationError:
                continue
            self.assertTrue(all(len(row) == 2 for row in produced))
            ran += 1
        self.assertGreater(ran, 30, "most generated graphs should be simulable")

    def test_the_scaffold_is_never_modified(self) -> None:
        scaffold = interface_scaffold(2, 2)
        before = (sorted(scaffold.nodes), sorted(scaffold.edges))
        generator = self._generator(10)
        for seed in range(10):
            generator.generate(Random(seed), scaffold)
        self.assertEqual(before, (sorted(scaffold.nodes), sorted(scaffold.edges)))

    def test_generation_is_reproducible_with_a_scaffold(self) -> None:
        scaffold = interface_scaffold(2, 2)
        generator = self._generator(10)
        first = generator.generate(Random(11), scaffold)
        second = generator.generate(Random(11), scaffold)
        self.assertEqual(first.nodes, second.nodes)
        self.assertEqual(first.edges, second.edges)

    def test_the_repair_pass_is_what_makes_generation_reliable(self) -> None:
        """Random edge placement alone leaves most operator nodes starved."""
        scaffold = interface_scaffold(2, 2)

        def successes(connect_isolated: bool) -> int:
            generator = self._generator(10, attempts=1, connect_isolated=connect_isolated)
            total = 0
            for seed in range(120):
                try:
                    generator.generate(Random(seed), scaffold)
                    total += 1
                except GraphGenerationError:
                    pass
            return total

        self.assertEqual(120, successes(True))
        self.assertLess(successes(False), 60)

    def test_a_generated_cell_actually_runs_when_execution_is_required(self) -> None:
        """Structural validity is not executability, so generation must check.

        Without the gate roughly a quarter of accepted graphs die on their
        first run -- almost always a reciprocal fed a zero by a delay's initial
        condition or by a multiply with a zero factor.
        """
        scaffold = interface_scaffold(2, 2)
        generator = self._generator(10, attempts=40)
        horizon = [(0.5, -0.3)] * 60

        for seed in range(60):
            graph = generator.generate(
                Random(seed), scaffold, accept=lambda candidate: simulates(candidate, horizon)
            )
            produced = SignalSimulator().run_series(graph, horizon)
            self.assertEqual(len(horizon), len(produced))
            self.assertTrue(all(len(row) == 2 for row in produced))

    def test_probing_the_full_horizon_beats_probing_a_short_one(self) -> None:
        """Divergence can take more steps to appear than a short probe covers."""
        scaffold = interface_scaffold(2, 2)
        generator = self._generator(10, attempts=40)
        horizon = [(0.5, -0.3)] * 100

        def survivors(probe_length: int) -> int:
            probe = [(0.5, -0.3)] * probe_length
            total = 0
            for seed in range(120):
                graph = generator.generate(
                    Random(seed),
                    scaffold,
                    accept=lambda candidate: simulates(candidate, probe),
                )
                if simulates(graph, horizon):
                    total += 1
            return total

        self.assertEqual(120, survivors(100))
        self.assertLess(survivors(4), 120)

    def test_the_acceptance_check_runs_after_policy_validation(self) -> None:
        seen: list[int] = []

        def accept(graph: Graph) -> bool:
            self.policy.validate(graph)  # must already be structurally valid
            seen.append(len(graph.nodes))
            return False

        generator = self._generator(6, attempts=4)
        with self.assertRaises(GraphGenerationError) as caught:
            generator.generate(Random(0), interface_scaffold(1, 1), accept=accept)
        self.assertEqual(4, len(seen))
        self.assertIn("acceptance check", str(caught.exception))

    def test_simulates_reports_executability(self) -> None:
        working = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("a", "add"),
                Node("y", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "a"), Edge("e2", "a", "y")],
        )
        broken = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("inv", "reciprocal"),
                Node("y", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "inv"), Edge("e2", "inv", "y")],
        )
        self.assertTrue(simulates(working, [(1.0,), (2.0,)]))
        self.assertFalse(simulates(broken, [(0.0,)]))

    def test_generation_without_a_scaffold_still_fails(self) -> None:
        """The terminals have to come from somewhere; the policy cannot make them."""
        with self.assertRaises(GraphGenerationError) as caught:
            self._generator(10).generate(Random(0))
        self.assertIn("input and one output", str(caught.exception))

    def test_the_policy_never_creates_a_terminal_node(self) -> None:
        """Which is why the interface must be supplied rather than discovered."""
        kinds = {self.policy.create_node(Random(seed)).kind for seed in range(300)}
        self.assertNotIn("input", kinds)
        self.assertNotIn("output", kinds)

    def test_terminals_cannot_be_added_to_the_evolvable_set(self) -> None:
        self.assertNotIn("input", OPERATOR_ARITY)
        with self.assertRaises(ValueError):
            SignalGraphPolicy(evolvable_kinds=("add", "input"))

    def test_the_policy_reports_which_nodes_still_need_inputs(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("starved", "add"),
                Node("fed", "add"),
                Node("y", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "fed")],
        )
        self.assertFalse(self.policy.requires_more_inputs(graph, "u"))
        self.assertFalse(self.policy.requires_more_inputs(graph, "fed"))
        self.assertTrue(self.policy.requires_more_inputs(graph, "starved"))
        self.assertTrue(self.policy.requires_more_inputs(graph, "y"))


if __name__ == "__main__":
    unittest.main()
