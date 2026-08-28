import unittest
from random import Random

from ameba.cli import example_graph
from ameba_graph import Edge, Graph, Node
from ameba_graph.crossover import (
    AlignedAttributeCrossover,
    CrossoverPortfolio,
    CrossoverError,
    InducedSubgraphInsertionCrossover,
    TypedSubgraphReplacementCrossover,
    UniformGraphCrossover,
)
from ameba_graph.mutation import (
    AddEdge,
    MoveEdgeSource,
    MoveEdgeTarget,
    MutateEdgeAttributes,
    MutateNodeAttributes,
    MutationError,
    MutationPortfolio,
    RemoveEdge,
    RemoveNodeBypass,
    ReplaceNode,
    SplitEdge,
)
from ameba_graph.serialization import graph_dumps, graph_loads
from ameba_signal import SignalGraphPolicy


class FlexiblePolicy:
    def create_node(self, rng: Random) -> Node:
        return Node("ignored", "replacement", {"value": rng.random()})

    def create_edge(self, source: str, target: str, rng: Random) -> Edge:
        return Edge("ignored", source, target, {"weight": rng.random()})

    def can_connect(self, graph: Graph, source: str, target: str) -> bool:
        return source != target and not any(
            edge.source == source and edge.target == target for edge in graph.edges.values()
        )

    def validate(self, graph: Graph) -> None:
        graph.validate_structure()
        if not graph.nodes:
            raise ValueError("graph cannot be empty")

    def mutate_node(self, node: Node, rng: Random) -> Node:
        node.attributes["value"] = float(node.attributes.get("value", 0.0)) + 1.0
        return node

    def mutate_edge(self, edge: Edge, rng: Random) -> Edge:
        edge.attributes["weight"] = float(edge.attributes.get("weight", 0.0)) + 1.0
        return edge

    def cross_node(self, left: Node, right: Node, rng: Random) -> Node:
        left.attributes["value"] = (
            float(left.attributes.get("value", 0.0))
            + float(right.attributes.get("value", 0.0))
        ) / 2.0
        return left

    def cross_edge(self, left: Edge, right: Edge, rng: Random) -> Edge:
        left.attributes["weight"] = (
            float(left.attributes.get("weight", 0.0))
            + float(right.attributes.get("weight", 0.0))
        ) / 2.0
        return left

    @staticmethod
    def can_transfer_node(node: Node) -> bool:
        return node.kind not in {"external", "input", "output"}

    @staticmethod
    def connection_type(graph: Graph, source: str, target: str) -> str:
        return str(graph.edges.get(f"{source}-{target}", Edge("", source, target)).attributes.get("type", "scalar"))


class FixedReproductionTests(unittest.TestCase):
    def setUp(self) -> None:
        self.policy = FlexiblePolicy()

    def test_locks_round_trip_through_graph_schema(self) -> None:
        graph = Graph(
            nodes=[Node("a", "kind", {"value": 1}, True, frozenset({"value"}))],
            edges=[],
        )
        restored = graph_loads(graph_dumps(graph))
        self.assertTrue(restored.nodes["a"].locked)
        self.assertEqual(frozenset({"value"}), restored.nodes["a"].locked_attributes)

    def test_signal_parameter_mutations_respect_locks_and_parents(self) -> None:
        graph = example_graph()
        graph.nodes["two"].locked_attributes = frozenset({"value"})
        original = graph_dumps(graph)
        child = MutateNodeAttributes().mutate(graph, SignalGraphPolicy(), Random(4))
        self.assertEqual(2.0, child.nodes["two"].attributes["value"])
        self.assertNotEqual(1.0, child.nodes["one"].attributes["value"])
        self.assertEqual(original, graph_dumps(graph))

        weighted = MutateEdgeAttributes().mutate(graph, SignalGraphPolicy(), Random(8))
        self.assertTrue(any("weight" in edge.attributes for edge in weighted.edges.values()))
        self.assertEqual(original, graph_dumps(graph))

    def test_locked_attributes_cannot_be_changed(self) -> None:
        graph = Graph(nodes=[Node("a", "kind", {"value": 1}, False, frozenset({"value"}))])
        with self.assertRaises(MutationError):
            MutateNodeAttributes().mutate(graph, self.policy, Random(1))
        edge_graph = Graph(
            nodes=[Node("a", "kind"), Node("b", "kind")],
            edges=[Edge("e", "a", "b", {"weight": 1.0}, False, False, frozenset({"weight"}))],
        )
        with self.assertRaises(MutationError):
            MutateEdgeAttributes().mutate(edge_graph, self.policy, Random(1))

    def test_edge_endpoint_moves_are_transactional_and_lock_aware(self) -> None:
        graph = Graph(
            nodes=[Node(name, "kind") for name in ("a", "b", "c", "d")],
            edges=[Edge("e", "a", "b")],
        )
        original = graph_dumps(graph)
        moved_source = MoveEdgeSource().mutate(graph, self.policy, Random(2))
        moved_target = MoveEdgeTarget().mutate(graph, self.policy, Random(2))
        self.assertNotEqual("a", moved_source.edges["e"].source)
        self.assertNotEqual("b", moved_target.edges["e"].target)
        self.assertEqual(original, graph_dumps(graph))

        graph.edges["e"].source_locked = True
        with self.assertRaises(MutationError):
            MoveEdgeSource().mutate(graph, self.policy, Random(2))

    def test_node_replace_and_bypass_preserve_parent(self) -> None:
        graph = Graph(
            nodes=[Node("a", "kind"), Node("b", "old"), Node("c", "kind")],
            edges=[Edge("ab", "a", "b"), Edge("bc", "b", "c")],
        )
        original = graph_dumps(graph)
        replaced = ReplaceNode().mutate(graph, self.policy, Random(3))
        self.assertTrue(any(node.kind == "replacement" for node in replaced.nodes.values()))
        bypassed = RemoveNodeBypass().mutate(graph, self.policy, Random(7))
        self.assertEqual(2, len(bypassed.nodes))
        self.assertEqual(1, len(bypassed.edges))
        edge = next(iter(bypassed.edges.values()))
        self.assertEqual(("a", "c"), (edge.source, edge.target))
        self.assertEqual(original, graph_dumps(graph))

    def test_attribute_crossover_uses_second_parent_and_preserves_both(self) -> None:
        left = Graph(nodes=[Node("n", "kind", {"value": 0.0})])
        right = Graph(nodes=[Node("n", "kind", {"value": 10.0})])
        left_before, right_before = graph_dumps(left), graph_dumps(right)
        child = AlignedAttributeCrossover().cross(left, right, self.policy, Random(1))
        self.assertEqual(5.0, child.nodes["n"].attributes["value"])
        self.assertEqual(left_before, graph_dumps(left))
        self.assertEqual(right_before, graph_dumps(right))

    def test_structural_crossover_cannot_drop_locked_primary_node(self) -> None:
        left = Graph(nodes=[Node("locked", "kind", locked=True)])
        right = Graph(nodes=[Node("other", "kind")])
        for seed in range(10):
            child = UniformGraphCrossover().cross(left, right, self.policy, Random(seed))
            self.assertIn("locked", child.nodes)

    def test_structural_crossover_retains_locked_edge_and_endpoints(self) -> None:
        left = Graph(
            nodes=[Node("a", "kind"), Node("b", "kind")],
            edges=[Edge("locked", "a", "b", source_locked=True)],
        )
        right = Graph(nodes=[Node("other", "kind")])
        for seed in range(10):
            child = UniformGraphCrossover().cross(left, right, self.policy, Random(seed))
            self.assertIn("locked", child.edges)
            self.assertIn("a", child.nodes)
            self.assertIn("b", child.nodes)

    def test_portfolios_are_bounded_and_repeatable(self) -> None:
        graph = Graph(
            nodes=[Node("a", "kind", {"value": 1.0}), Node("b", "kind")],
            edges=[Edge("e", "a", "b", {"weight": 1.0})],
        )
        mutations = MutationPortfolio(
            [
                MutateNodeAttributes(),
                MutateEdgeAttributes(),
                SplitEdge(),
                AddEdge(),
                RemoveEdge(),
                MoveEdgeSource(),
                MoveEdgeTarget(),
                ReplaceNode(),
                RemoveNodeBypass(),
            ]
        )
        first = mutations.mutate(graph, self.policy, Random(12))
        second = mutations.mutate(graph, self.policy, Random(12))
        self.assertEqual(graph_dumps(first), graph_dumps(second))
        self.assertEqual(2, len(graph.nodes))
        self.assertEqual(1, len(graph.edges))

        crossovers = CrossoverPortfolio(
            [AlignedAttributeCrossover(), UniformGraphCrossover()]
        )
        right = graph.copy()
        right.nodes["a"].attributes["value"] = 9.0
        first_child = crossovers.cross(graph, right, self.policy, Random(14))
        second_child = crossovers.cross(graph, right, self.policy, Random(14))
        self.assertEqual(graph_dumps(first_child), graph_dumps(second_child))

    def test_induced_subgraph_insertion_is_transactional_and_seeded(self) -> None:
        left = Graph(
            nodes=[
                Node("source", "external"),
                Node("middle", "kind"),
                Node("target", "external"),
            ],
            edges=[Edge("in", "source", "middle"), Edge("out", "middle", "target")],
        )
        right = Graph(
            nodes=[
                Node("donor-source", "external"),
                Node("donor", "donor"),
                Node("donor-target", "external"),
            ],
            edges=[
                Edge("donor-in", "donor-source", "donor"),
                Edge("donor-out", "donor", "donor-target"),
            ],
        )
        before = (graph_dumps(left), graph_dumps(right))
        operator = InducedSubgraphInsertionCrossover(max_nodes=1)
        first = operator.cross(left, right, self.policy, Random(19))
        second = operator.cross(left, right, self.policy, Random(19))
        self.assertEqual(graph_dumps(first), graph_dumps(second))
        self.assertTrue(any(node.kind == "donor" for node in first.nodes.values()))
        self.assertEqual(before, (graph_dumps(left), graph_dumps(right)))

    def test_typed_subgraph_replacement_maps_complete_boundary(self) -> None:
        left = Graph(
            nodes=[
                Node("source", "external"),
                Node("old", "old"),
                Node("target", "external"),
            ],
            edges=[Edge("in", "source", "old"), Edge("out", "old", "target")],
        )
        right = Graph(
            nodes=[
                Node("source", "external"),
                Node("new", "new"),
                Node("target", "external"),
            ],
            edges=[Edge("in", "source", "new"), Edge("out", "new", "target")],
        )
        before = (graph_dumps(left), graph_dumps(right))
        operator = TypedSubgraphReplacementCrossover(max_nodes=1)
        child = operator.cross(left, right, self.policy, Random(23))
        self.assertFalse(any(node.kind == "old" for node in child.nodes.values()))
        replacement = next(node for node in child.nodes.values() if node.kind == "new")
        self.assertEqual(replacement.id, child.edges["in"].target)
        self.assertEqual(replacement.id, child.edges["out"].source)
        self.assertEqual(before, (graph_dumps(left), graph_dumps(right)))

        left.nodes["old"].locked = True
        with self.assertRaises(CrossoverError):
            operator.cross(left, right, self.policy, Random(23))


if __name__ == "__main__":
    unittest.main()
