import unittest

from ameba.benchmarks import linear, narendra
from ameba.equations import format_equations, graph_to_equations
from ameba_graph import Edge, Graph, Node


def rendered(graph: Graph) -> dict[str, str]:
    return {equation.name: equation.text for equation in graph_to_equations(graph)}


class EquationRenderingTests(unittest.TestCase):
    def test_linear_reference_recovers_the_published_equation(self) -> None:
        equations = rendered(linear.reference_graph())
        self.assertEqual(
            "response(k) = 0.12 * u0(k) + 1.5 * y_k(k) - 0.74 * y_km1(k) + 0.12 * y_km2(k)",
            equations["response"],
        )

    def test_delay_chain_renders_as_successive_shifts(self) -> None:
        equations = rendered(linear.reference_graph())
        self.assertEqual("y_k(k) = response(k-1)", equations["y_k"])
        self.assertEqual("y_km1(k) = y_k(k-1)", equations["y_km1"])
        self.assertEqual("y_km2(k) = y_km1(k-1)", equations["y_km2"])

    def test_narendra_reference_recovers_the_published_equation(self) -> None:
        equations = rendered(narendra.reference_graph())
        self.assertEqual(
            "response(k) = ((-1 + y_km2(k)) * u_km1(k) * y_k(k) * y_km1(k) * y_km2(k)"
            " + u0(k)) / (1 + y_km1(k)^2 + y_km2(k)^2)",
            equations["response"],
        )

    def test_a_multi_step_delay_shifts_by_its_step_count(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("d", "delay", {"steps": 4, "initial": 0.0}),
                Node("y", "output", {"index": 0}),
            ],
            edges=[Edge("e1", "u", "d"), Edge("e2", "d", "y")],
        )
        self.assertEqual("d(k) = u0(k-4)", rendered(graph)["d"])

    def test_single_use_nodes_are_inlined_and_reused_nodes_are_named(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("scaled", "add"),
                Node("left", "add"),
                Node("right", "add"),
                Node("total", "add"),
                Node("y", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "u", "scaled", {"weight": 2.0}),
                Edge("e2", "scaled", "left"),
                Edge("e3", "scaled", "right"),
                Edge("e4", "left", "total"),
                Edge("e5", "right", "total"),
                Edge("e6", "total", "y"),
            ],
        )
        equations = rendered(graph)
        # Used twice, so it earns its own line rather than being duplicated.
        self.assertIn("scaled", equations)
        self.assertEqual("scaled(k) = 2 * u0(k)", equations["scaled"])
        # Used once each, so these are inlined into the output expression.
        self.assertNotIn("left", equations)
        self.assertNotIn("right", equations)

    def test_negative_weights_render_as_subtraction(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("one", "constant", {"value": 1.0}),
                Node("difference", "add"),
                Node("y", "output", {"index": 0}),
            ],
            edges=[
                Edge("e1", "u", "difference"),
                Edge("e2", "one", "difference", {"weight": -1.0}),
                Edge("e3", "difference", "y"),
            ],
        )
        self.assertEqual("y0(k) = u0(k) - 1", rendered(graph)["y"])

    def test_stateful_operators_render_as_recurrences(self) -> None:
        for kind, expected in (
            ("filter_lp", "s(k) = s(k-1) + 0.5 * (u0(k) - s(k-1))"),
            ("integral", "s(k) = s(k-1) + u0(k)"),
            ("derivative", "s(k) = (u0(k) - u0(k-1)) / 1"),
        ):
            graph = Graph(
                nodes=[
                    Node("u", "input", {"index": 0}),
                    Node("s", kind, {}),
                    Node("y", "output", {"index": 0}),
                ],
                edges=[Edge("e1", "u", "s"), Edge("e2", "s", "y")],
            )
            self.assertEqual(expected, rendered(graph)["s"], msg=kind)

    def test_every_node_that_needs_a_line_gets_exactly_one(self) -> None:
        for graph in (linear.reference_graph(), narendra.reference_graph()):
            equations = graph_to_equations(graph)
            names = [equation.name for equation in equations]
            self.assertEqual(len(names), len(set(names)))
            self.assertTrue(all(equation.text for equation in equations))

    def test_formatting_produces_one_aligned_line_per_equation(self) -> None:
        text = format_equations(linear.reference_graph())
        self.assertEqual(
            len(graph_to_equations(linear.reference_graph())), len(text.splitlines())
        )

    def test_an_empty_graph_does_not_crash(self) -> None:
        self.assertEqual("  (empty graph)", format_equations(Graph()))


if __name__ == "__main__":
    unittest.main()
