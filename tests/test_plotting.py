import unittest

from ameba.benchmarks import linear, narendra, seed_graph
from ameba.plotting import build_layout, is_feedback, node_color
from ameba_graph import Edge, Graph, Node


def _delay_chain_graph() -> Graph:
    """input -> delay -> output, the shape a short evolved model often takes."""
    return Graph(
        nodes=[
            Node("u", "input", {"index": 0}),
            Node("d", "delay", {"steps": 2, "initial": 0.0}),
            Node("y", "output", {"index": 0}),
        ],
        edges=[Edge("e1", "u", "d"), Edge("e2", "d", "y")],
    )


class LayoutTests(unittest.TestCase):
    def test_a_chain_gets_one_node_per_layer(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("a", "add"),
                Node("b", "add"),
                Node("y", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "a"), Edge("e2", "a", "b"), Edge("e3", "b", "y")],
        )
        layout = build_layout(graph)
        self.assertEqual([0, 1, 2, 3], [layout.layers[n] for n in ("u", "a", "b", "y")])
        self.assertEqual(4, layout.width)
        self.assertEqual(1, layout.height)

    def test_the_interface_sits_on_the_outer_columns(self) -> None:
        """Inputs on the left, outputs on the right, everything else between."""
        for graph in (
            linear.reference_graph(),
            narendra.reference_graph(),
            seed_graph(),
            _delay_chain_graph(),
        ):
            layout = build_layout(graph)
            depth = max(layout.layers.values())
            for node in graph.nodes.values():
                layer = layout.layers[node.id]
                if node.kind == "input":
                    self.assertEqual(0, layer, msg=node.id)
                elif node.kind == "output":
                    self.assertEqual(depth, layer, msg=node.id)
                else:
                    self.assertGreater(layer, 0, msg=node.id)
                    self.assertLess(layer, depth, msg=node.id)

    def test_a_delay_fed_straight_from_an_input_stays_inside(self) -> None:
        """The regression: this delay used to share the input column."""
        layout = build_layout(_delay_chain_graph())
        self.assertEqual(0, layout.layers["u"])
        self.assertEqual(1, layout.layers["d"])
        self.assertEqual(2, layout.layers["y"])

    def test_feedback_is_exactly_the_edges_entering_a_delay(self) -> None:
        graph = linear.reference_graph()
        feedback = {edge.id for edge in graph.edges.values() if is_feedback(graph, edge)}
        expected = {
            edge.id
            for edge in graph.edges.values()
            if graph.nodes[edge.target].kind == "delay"
        }
        self.assertEqual(expected, feedback)
        self.assertTrue(feedback)

    def test_layering_survives_feedback_cycles(self) -> None:
        """The reference graphs contain delay loops, which must not deadlock."""
        for graph in (linear.reference_graph(), narendra.reference_graph(), seed_graph()):
            layout = build_layout(graph)
            self.assertEqual(set(graph.nodes), set(layout.positions))
            self.assertEqual(set(graph.nodes), set(layout.layers))

    def test_every_node_gets_a_distinct_position(self) -> None:
        for graph in (linear.reference_graph(), narendra.reference_graph()):
            positions = build_layout(graph).positions
            rounded = {(round(x, 6), round(y, 6)) for x, y in positions.values()}
            self.assertEqual(len(positions), len(rounded))

    def test_forward_edges_never_point_backwards(self) -> None:
        """Only feedback may run right to left; that is what the colour means."""
        for graph in (linear.reference_graph(), narendra.reference_graph()):
            layout = build_layout(graph)
            for edge in graph.edges.values():
                if is_feedback(graph, edge):
                    continue
                self.assertLess(
                    layout.layers[edge.source],
                    layout.layers[edge.target],
                    msg=f"{edge.id} in {graph.nodes[edge.target].kind}",
                )

    def test_crossing_reduction_is_deterministic(self) -> None:
        first = build_layout(narendra.reference_graph()).positions
        second = build_layout(narendra.reference_graph()).positions
        self.assertEqual(first, second)

    def test_node_colors_distinguish_the_structural_roles(self) -> None:
        distinct = {node_color(kind) for kind in ("input", "output", "delay", "add")}
        self.assertEqual(4, len(distinct))
        # Filters are stateful but are not cycle breakers, so they share a colour
        # family with delays rather than with plain arithmetic.
        self.assertNotEqual(node_color("filter_lp"), node_color("add"))


if __name__ == "__main__":
    unittest.main()
