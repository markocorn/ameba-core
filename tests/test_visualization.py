import unittest

from ameba.cli import example_graph
from ameba.visualization import graph_to_html


class VisualizationTests(unittest.TestCase):
    def test_visualization_is_standalone_and_embeds_graph(self) -> None:
        html = graph_to_html(example_graph(), "Linear graph")
        self.assertIn("<!doctype html>", html)
        self.assertIn('"schema": "ameba.graph"', html)
        self.assertIn("Linear graph", html)
        self.assertNotIn("https://", html)


if __name__ == "__main__":
    unittest.main()
