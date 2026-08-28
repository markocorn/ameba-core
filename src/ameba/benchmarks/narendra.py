"""The Narendra plant: the nonlinear rung of the benchmark ladder.

Narendra and Parthasarathy (1990), example 4:

    y(k+1) = ( y(k) y(k-1) y(k-2) u(k-1) [y(k-2) - 1] + u(k) )
             / ( 1 + y(k-1)^2 + y(k-2)^2 )

Parameter tuning alone cannot reach this. Fitting it requires a five-way
product, a division, and three steps of output history, so the score measures
whether structural search works — not whether weight mutation converges.

Like every benchmark here the model sees only u(k) and must carry its own state
through delay nodes.
"""

from __future__ import annotations

from typing import Sequence

from ameba_graph import Edge, Graph, Node

from .common import (
    CONTROL_INPUT,
    PlantTrajectory,
    add_delay_chain,
    add_output,
)

RESPONSE_DELAYS = ("y_k", "y_km1", "y_km2")
CONTROL_DELAY = "u_km1"


def narendra_trajectory(controls: Sequence[float]) -> PlantTrajectory:
    """Run the plant from rest over the supplied control sequence."""
    y_k = y_km1 = y_km2 = 0.0
    u_km1 = 0.0
    responses: list[float] = []
    for control in controls:
        u_k = float(control)
        y_next = (y_k * y_km1 * y_km2 * u_km1 * (y_km2 - 1.0) + u_k) / (
            1.0 + y_km1 * y_km1 + y_km2 * y_km2
        )
        responses.append(y_next)
        y_k, y_km1, y_km2 = y_next, y_k, y_km1
        u_km1 = u_k
    return PlantTrajectory(tuple(float(control) for control in controls), tuple(responses))


def reference_graph() -> Graph:
    """The exact plant as a single-input model graph."""
    graph = Graph()
    graph.add_node(Node(CONTROL_INPUT, "input", {"index": 0}))
    add_delay_chain(graph, CONTROL_INPUT, (CONTROL_DELAY,))
    # Placeholder edge order: the response node is created by the equation
    # builder below, so the output history chain is closed afterwards.
    y_k, y_km1, y_km2 = RESPONSE_DELAYS
    for name in RESPONSE_DELAYS:
        graph.add_node(Node(name, "delay", {"steps": 1, "initial": 0.0}))

    response = _add_plant_equation(graph, CONTROL_INPUT, CONTROL_DELAY, y_k, y_km1, y_km2)

    # Every one of these loops closes through a delay, so the graph stays free
    # of algebraic cycles while y(k+1) becomes y(k) on the following step.
    graph.add_edge(Edge(f"e_{response}_{y_k}", response, y_k))
    graph.add_edge(Edge(f"e_{y_k}_{y_km1}", y_k, y_km1))
    graph.add_edge(Edge(f"e_{y_km1}_{y_km2}", y_km1, y_km2))
    return add_output(graph, response)


def _add_plant_equation(
    graph: Graph,
    u_k: str,
    u_km1: str,
    y_k: str,
    y_km1: str,
    y_km2: str,
) -> str:
    """Add the plant arithmetic and return the node holding y(k+1)."""
    graph.add_node(Node("one", "constant", {"value": 1.0}))

    # y(k-2) - 1
    graph.add_node(Node("offset", "add"))
    graph.add_edge(Edge(f"e_{y_km2}_offset", y_km2, "offset"))
    graph.add_edge(Edge("e_one_offset", "one", "offset", {"weight": -1.0}))

    # y(k) y(k-1) y(k-2) u(k-1) [y(k-2) - 1]
    graph.add_node(Node("product", "multiply"))
    for source in (y_k, y_km1, y_km2, u_km1, "offset"):
        graph.add_edge(Edge(f"e_{source}_product", source, "product"))

    graph.add_node(Node("numerator", "add"))
    graph.add_edge(Edge("e_product_numerator", "product", "numerator"))
    graph.add_edge(Edge(f"e_{u_k}_numerator", u_k, "numerator"))

    denominator_terms = ["one", _add_square(graph, y_km1), _add_square(graph, y_km2)]
    graph.add_node(Node("denominator", "add"))
    for term in denominator_terms:
        graph.add_edge(Edge(f"e_{term}_denominator", term, "denominator"))

    graph.add_node(Node("inverse", "reciprocal"))
    graph.add_edge(Edge("e_denominator_inverse", "denominator", "inverse"))

    graph.add_node(Node("response", "multiply"))
    graph.add_edge(Edge("e_numerator_response", "numerator", "response"))
    graph.add_edge(Edge("e_inverse_response", "inverse", "response"))
    return "response"


def _add_square(graph: Graph, source: str) -> str:
    """Square one signal, returning the product node.

    A node cannot feed the same target twice, so one factor reaches the product
    through a single-input ``add``, which acts as a unit-gain relay.
    """
    relay = f"relay_{source}"
    square = f"square_{source}"
    graph.add_node(Node(relay, "add"))
    graph.add_edge(Edge(f"e_{source}_{relay}", source, relay))
    graph.add_node(Node(square, "multiply"))
    graph.add_edge(Edge(f"e_{source}_{square}", source, square))
    graph.add_edge(Edge(f"e_{relay}_{square}", relay, square))
    return square
