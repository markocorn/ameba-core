"""A third-order linear plant: the entry rung of the benchmark ladder.

    y(k+1) = 1.5 y(k) - 0.74 y(k-1) + 0.12 y(k-2) + 0.12 u(k)

The feedback coefficients place the poles at 0.4, 0.5, and 0.6, so the plant is
strictly stable and well damped, and the input gain is chosen to give unit DC
gain. Nothing about it is numerically delicate.

The point of this benchmark is that it is the smallest honest dynamic
identification task. It needs no nonlinearity and no division — only a chain of
three unit delays and one weighted sum — but a model still cannot fit it
without representing state, because the model sees u(k) and nothing else.

It exists to separate two failure modes. A method that cannot solve this has a
problem with search or with delay handling. A method that solves this but fails
the Narendra plant has a problem with structural search over nonlinearities.
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

# Coefficients on y(k), y(k-1), y(k-2); poles at 0.4, 0.5, 0.6.
FEEDBACK = (1.5, -0.74, 0.12)

# Chosen so the plant has unit DC gain: GAIN == 1 - sum(FEEDBACK).
GAIN = 0.12

DELAYS = ("y_k", "y_km1", "y_km2")


def linear_trajectory(controls: Sequence[float]) -> PlantTrajectory:
    """Run the plant from rest over the supplied control sequence."""
    y_k = y_km1 = y_km2 = 0.0
    responses: list[float] = []
    for control in controls:
        y_next = (
            FEEDBACK[0] * y_k
            + FEEDBACK[1] * y_km1
            + FEEDBACK[2] * y_km2
            + GAIN * float(control)
        )
        responses.append(y_next)
        y_k, y_km1, y_km2 = y_next, y_k, y_km1
    return PlantTrajectory(tuple(float(control) for control in controls), tuple(responses))


def reference_graph() -> Graph:
    """The exact plant as a single-input model graph.

    Six nodes: the control input, one weighted sum, three unit delays carrying
    the output history, and the response output.
    """
    graph = Graph()
    graph.add_node(Node(CONTROL_INPUT, "input", {"index": 0}))
    graph.add_node(Node("response", "add"))
    graph.add_edge(Edge("e_u_k_response", CONTROL_INPUT, "response", {"weight": GAIN}))

    # The delay chain is fed by the response, so y(k+1) becomes y(k) next step.
    add_delay_chain(graph, "response", DELAYS)
    for name, coefficient in zip(DELAYS, FEEDBACK):
        graph.add_edge(Edge(f"e_{name}_response", name, "response", {"weight": coefficient}))

    return add_output(graph, "response")
