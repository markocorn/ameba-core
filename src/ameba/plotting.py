"""Layered drawing of signal graphs.

Signal graphs are layered dataflow with feedback: values move left to right
through combinational nodes, and delays carry them back. A force-directed or
circular layout throws that structure away, so this builds a Sugiyama-style
layered drawing instead — assign layers, order nodes within each layer to
reduce edge crossings, then place and draw.

Only edges that are not feedback take part in layering, which is what makes the
layer assignment well defined on a graph that contains cycles.
"""

from __future__ import annotations

from dataclasses import dataclass

import networkx as nx

from ameba_graph import Graph
from ameba_signal.stateful import CYCLE_BREAKER_KINDS, STATEFUL_KINDS

NODE_COLORS = {
    "input": "#059669",
    "output": "#dc2626",
    "constant": "#6b7280",
    "delay": "#d97706",
}
STATEFUL_COLOR = "#ea580c"
DEFAULT_COLOR = "#2563eb"

FEEDBACK_COLOR = "#f97316"
EDGE_COLOR = "#9ca3af"

_X_SPACING = 2.0
_Y_SPACING = 1.15
_RADIUS = 0.42


@dataclass(frozen=True, slots=True)
class Layout:
    positions: dict[str, tuple[float, float]]
    layers: dict[str, int]

    @property
    def width(self) -> int:
        return max(self.layers.values(), default=0) + 1

    @property
    def height(self) -> int:
        rows = {round(y, 3) for _, y in self.positions.values()}
        return max(len(rows), 1)


def is_feedback(graph: Graph, edge) -> bool:
    """Feedback is any edge entering a delay: that is where loops close."""
    return graph.nodes[edge.target].kind in CYCLE_BREAKER_KINDS


def build_layout(graph: Graph) -> Layout:
    digraph = nx.DiGraph()
    for node_id in graph.nodes:
        digraph.add_node(node_id)
    for edge in graph.edges.values():
        if not is_feedback(graph, edge):
            digraph.add_edge(edge.source, edge.target)

    layers = _assign_layers(graph, digraph)
    order = _reduce_crossings(graph, layers)
    positions = _place(order)
    return Layout(positions, layers)


def _assign_layers(graph: Graph, digraph: nx.DiGraph) -> dict[str, int]:
    """Longest-path layering with the interface pinned to the outer columns.

    Inputs occupy the first column and outputs the last, with everything else
    strictly between them, so a drawing always reads as signals entering on the
    left and leaving on the right. Without the pinning an input can drift
    inward: the edge from an input straight into a delay is excluded from
    layering as feedback, which would otherwise leave that delay in the first
    column alongside the inputs it is fed by.
    """
    inputs = {node.id for node in graph.nodes.values() if node.kind == "input"}
    outputs = {node.id for node in graph.nodes.values() if node.kind == "output"}

    # Interior nodes start at column 1, which keeps them out of the input column
    # even when no forward edge reaches them.
    layers = {node_id: (0 if node_id in inputs else 1) for node_id in graph.nodes}
    for node_id in nx.topological_sort(digraph):
        if node_id in inputs:
            continue
        predecessors = list(digraph.predecessors(node_id))
        if predecessors:
            layers[node_id] = max(
                layers[node_id], max(layers[item] + 1 for item in predecessors)
            )

    interior_depth = max(
        (layer for node_id, layer in layers.items() if node_id not in outputs), default=0
    )
    for node_id in outputs:
        layers[node_id] = interior_depth + 1
    return layers


def _reduce_crossings(graph: Graph, layers: dict[str, int]) -> dict[int, list[str]]:
    """Order nodes within layers by repeated barycenter sweeps.

    Placing each node next to the average position of its neighbours is the
    standard heuristic for untangling a layered drawing, and a handful of
    alternating sweeps gets most of the available improvement.
    """
    columns: dict[int, list[str]] = {}
    for node_id in sorted(graph.nodes, key=lambda item: (layers[item], item)):
        columns.setdefault(layers[node_id], []).append(node_id)

    neighbours_before: dict[str, list[str]] = {node_id: [] for node_id in graph.nodes}
    neighbours_after: dict[str, list[str]] = {node_id: [] for node_id in graph.nodes}
    for edge in graph.edges.values():
        if is_feedback(graph, edge) or layers[edge.source] == layers[edge.target]:
            continue
        neighbours_before[edge.target].append(edge.source)
        neighbours_after[edge.source].append(edge.target)

    for sweep in range(8):
        keys = sorted(columns) if sweep % 2 == 0 else sorted(columns, reverse=True)
        reference = neighbours_before if sweep % 2 == 0 else neighbours_after
        for layer in keys:
            ranks = {
                node_id: index
                for other in columns
                for index, node_id in enumerate(columns[other])
            }
            # Snapshot the current order first: ``list.sort`` empties the list
            # while it runs, so the key function cannot read it.
            current = {node_id: index for index, node_id in enumerate(columns[layer])}
            columns[layer].sort(
                key=lambda node_id: _barycenter(node_id, reference, ranks, current)
            )
    return columns


def _barycenter(
    node_id: str,
    reference: dict[str, list[str]],
    ranks: dict[str, int],
    current: dict[str, int],
) -> float:
    linked = [ranks[other] for other in reference[node_id] if other in ranks]
    # A node with no neighbours on that side keeps its current position.
    return sum(linked) / len(linked) if linked else float(current[node_id])


def _place(columns: dict[int, list[str]]) -> dict[str, tuple[float, float]]:
    positions: dict[str, tuple[float, float]] = {}
    tallest = max((len(members) for members in columns.values()), default=1)
    for layer, members in columns.items():
        offset = (tallest - len(members)) / 2.0
        for index, node_id in enumerate(members):
            positions[node_id] = (
                layer * _X_SPACING,
                -(index + offset) * _Y_SPACING,
            )
    return positions


def node_color(kind: str) -> str:
    if kind in NODE_COLORS:
        return NODE_COLORS[kind]
    return STATEFUL_COLOR if kind in STATEFUL_KINDS else DEFAULT_COLOR


def draw_graph(axes, graph: Graph, layout: Layout | None = None) -> Layout:
    """Draw ``graph`` onto a matplotlib axes and return the layout used."""
    from matplotlib.patches import Circle

    layout = layout or build_layout(graph)
    positions = layout.positions

    _draw_edges(axes, graph, positions)
    for node_id, (x, y) in positions.items():
        node = graph.nodes[node_id]
        axes.add_patch(
            Circle(
                (x, y),
                _RADIUS,
                facecolor=node_color(node.kind),
                edgecolor="white",
                linewidth=1.6,
                zorder=3,
            )
        )
        axes.text(
            x, y + 0.10, node.kind, fontsize=_fit(node.kind, 7.6),
            color="white", ha="center", va="center", zorder=4,
        )
        axes.text(
            x, y - 0.13, node_id, fontsize=_fit(node_id, 7.0),
            color="white", ha="center", va="center", zorder=4,
        )

    axes.set_aspect("equal")
    xs = [x for x, _ in positions.values()]
    ys = [y for _, y in positions.values()]
    axes.set_xlim(min(xs) - 1.0, max(xs) + 1.0)
    axes.set_ylim(min(ys) - 0.9, max(ys) + 0.9)
    axes.axis("off")
    return layout


def _draw_edges(axes, graph: Graph, positions: dict[str, tuple[float, float]]) -> None:
    # Labels are spread along parallel edges so that two weights sharing a
    # corridor do not print on top of each other.
    lanes: dict[tuple[float, float], int] = {}
    for edge in sorted(graph.edges.values(), key=lambda item: item.id):
        start, end = positions[edge.source], positions[edge.target]
        feedback = is_feedback(graph, edge)
        span = (start[0], end[0])
        lane = lanes.get(span, 0)
        lanes[span] = lane + 1

        curve = 0.28 if feedback else (0.06 + 0.05 * (lane % 3))
        # Trim in data coordinates so the head always lands on the node
        # boundary. A shrink in points drifts as the figure is resized, which
        # is what buries arrowheads under nodes on a dense graph.
        tail, head = _trim(start, end, curve)
        axes.annotate(
            "",
            xy=head,
            xytext=tail,
            zorder=2,
            arrowprops=dict(
                arrowstyle="-|>,head_length=0.7,head_width=0.35",
                color=FEEDBACK_COLOR if feedback else EDGE_COLOR,
                linewidth=1.5 if feedback else 1.1,
                shrinkA=0,
                shrinkB=0,
                mutation_scale=16,
                connectionstyle=f"arc3,rad={curve}",
            ),
        )

        weight = float(edge.attributes.get("weight", 1.0))
        if weight == 1.0:
            continue
        axes.annotate(
            f"{weight:.3g}",
            xy=_along(start, end, 0.30 + 0.16 * (lane % 3)),
            textcoords="offset points",
            xytext=(0, 11 if not feedback else 15),
            fontsize=7.2,
            color="#374151",
            ha="center",
            zorder=5,
            bbox=dict(boxstyle="round,pad=0.15", facecolor="white", edgecolor="none", alpha=0.75),
        )


def _trim(
    start: tuple[float, float], end: tuple[float, float], curve: float
) -> tuple[tuple[float, float], tuple[float, float]]:
    """Pull both endpoints back to the rim of their nodes.

    A curved connector leaves and arrives at an angle, so trimming along the
    straight line between centres leaves the arrowhead floating beside its
    target. ``arc3`` is a quadratic Bezier, so the tangent at each end points
    at the control point -- trimming along those directions puts the head on
    the node it actually points to.
    """
    dx, dy = end[0] - start[0], end[1] - start[1]
    if (dx * dx + dy * dy) ** 0.5 <= 2.0 * _RADIUS:
        return start, end

    # Matplotlib places the arc3 control point here.
    control = ((start[0] + end[0]) / 2 + curve * dy, (start[1] + end[1]) / 2 - curve * dx)
    # A small extra gap keeps the head clear of the node outline.
    return (
        _step(start, control, _RADIUS),
        _step(end, control, _RADIUS + 0.05),
    )


def _step(point: tuple[float, float], toward: tuple[float, float], distance: float):
    """Move ``point`` towards ``toward`` by ``distance``."""
    dx, dy = toward[0] - point[0], toward[1] - point[1]
    length = (dx * dx + dy * dy) ** 0.5
    if length == 0.0:
        return point
    return (point[0] + dx / length * distance, point[1] + dy / length * distance)


def _along(
    start: tuple[float, float], end: tuple[float, float], fraction: float
) -> tuple[float, float]:
    return (
        start[0] + (end[0] - start[0]) * fraction,
        start[1] + (end[1] - start[1]) * fraction,
    )


def _fit(text: str, base: float) -> float:
    """Shrink a label that would otherwise overflow its node."""
    return base if len(text) <= 9 else max(4.4, base * 9.0 / len(text))
