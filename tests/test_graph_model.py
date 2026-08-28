import unittest

from ameba_graph import Edge, Graph, GraphError, Node


class GraphModelTests(unittest.TestCase):
    def test_edge_endpoints_must_exist(self) -> None:
        graph = Graph(nodes=[Node("a", "arbitrary")])
        with self.assertRaises(GraphError):
            graph.add_edge(Edge("edge", "a", "missing"))

    def test_removing_node_removes_incident_edges(self) -> None:
        graph = Graph(
            nodes=[Node("a", "arbitrary"), Node("b", "arbitrary")],
            edges=[Edge("edge", "a", "b")],
        )
        graph.remove_node("a")
        self.assertEqual({}, graph.edges)

    def test_copy_is_independent(self) -> None:
        graph = Graph(nodes=[Node("a", "arbitrary", {"value": 1})])
        clone = graph.copy()
        clone.nodes["a"].attributes["value"] = 2
        self.assertEqual(1, graph.nodes["a"].attributes["value"])


if __name__ == "__main__":
    unittest.main()

