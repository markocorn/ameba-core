"""The Wood–Berry distillation column: a coupled MIMO rung with dead times.

R. K. Wood and M. W. Berry, "Terminal composition control of a binary
distillation column", Chemical Engineering Science 28(9), 1973.

A pilot-scale column separating methanol from water. Reflux flow and steam
flow are the inputs; the methanol fractions of the top and bottom products are
the outputs. Every input reaches every output, each path through its own
first-order lag and its own dead time, in minutes:

                 reflux u1                       steam u2
    top    y1    12.8 e^(-1 s) / (16.7 s + 1)    -18.9 e^(-3 s) / (21.0 s + 1)
    bottom y2     6.6 e^(-7 s) / (10.9 s + 1)    -19.4 e^(-3 s) / (14.4 s + 1)

It is the standard example of a strongly interacting process: steam affects
the top composition almost as much as reflux does, and with the opposite sign,
so neither output can be understood from its "own" input alone. The four dead
times differ -- one minute to seven -- so a model has to carry four separate
delays rather than one shared one.

Discretisation is exact at one sample per minute, with the input held between
samples (zero-order hold): each path becomes

    x(k+1) = a x(k) + K (1 - a) u(k - d),    a = exp(-1 / tau)

with ``d`` the dead time in samples. The outputs are reported divided by ten.
That is a change of units, not of dynamics, and it keeps every gain inside the
+-2 weight range a signal graph uses, so the exact model is reachable.

Unlike the quadruple tank this plant is linear, so it has an exact reference
graph: four delay lines into four first-order lags, summed in pairs. A search
can therefore be judged on whether it found *the* model, not only on how far
below the memoryless floor it got.
"""

from __future__ import annotations

from math import exp
from typing import Sequence

from ameba_graph import Edge, Graph, Node
from ameba_signal import interface_scaffold

from .common import MultiChannelTrajectory

#: Steady-state gains, [output][input], in the column's own units.
GAINS = ((12.8, -18.9), (6.6, -19.4))
#: First-order time constants, minutes.
TIME_CONSTANTS = ((16.7, 21.0), (10.9, 14.4))
#: Dead times, in samples of one minute.
DEAD_TIMES = ((1, 3), (7, 3))
#: Outputs are reported in tenths, so every gain fits the weight range.
OUTPUT_SCALE = 0.1
#: One sample per minute; the dead times are whole samples at this rate.
TIME_STEP = 1.0


def pole(output: int, control: int) -> float:
    """The discrete pole ``a`` of one path."""
    return exp(-TIME_STEP / TIME_CONSTANTS[output][control])


def wood_berry_trajectory(controls: Sequence[Sequence[float]]) -> MultiChannelTrajectory:
    """Run the column from rest over the supplied two-channel control sequence.

    ``responses[k]`` is y(k+1) given u up to k, as for every rung.
    """
    rows = [tuple(float(value) for value in row) for row in controls]
    if any(len(row) != 2 for row in rows):
        raise ValueError("The Wood–Berry column has exactly two inputs")
    states = [[0.0, 0.0], [0.0, 0.0]]
    responses: list[tuple[float, ...]] = []
    for step in range(len(rows)):
        outputs = []
        for i in range(2):
            for j in range(2):
                lagged = step - DEAD_TIMES[i][j]
                u = rows[lagged][j] if lagged >= 0 else 0.0
                a = pole(i, j)
                gain = GAINS[i][j] * OUTPUT_SCALE
                states[i][j] = a * states[i][j] + gain * (1.0 - a) * u
            outputs.append(states[i][0] + states[i][1])
        responses.append(tuple(outputs))
    return MultiChannelTrajectory(tuple(rows), tuple(responses))


def reference_graph() -> Graph:
    """The exact column as a two-input, two-output signal graph.

    Each path is a delay line of its dead time feeding a first-order low-pass
    with ``alpha = 1 - a`` and the path gain on its incoming edge, so

        x(k) = a x(k-1) + (1 - a) K u(k - d)

    which is the recursion above. Each output sums its two paths. Fourteen
    nodes: two inputs, four delays, four lags, two sums, two outputs.
    """
    graph = interface_scaffold(2, 2)
    for i in range(2):
        total = f"sum{i}"
        graph.add_node(Node(total, "add"))
        for j in range(2):
            delay, lag = f"d{i}{j}", f"lag{i}{j}"
            graph.add_node(Node(delay, "delay", {"steps": DEAD_TIMES[i][j], "initial": 0.0}))
            graph.add_node(Node(lag, "filter_lp", {"alpha": 1.0 - pole(i, j), "initial": 0.0}))
            graph.add_edge(Edge(f"e_u{j}_{delay}", f"u{j}", delay, {"weight": 1.0}))
            graph.add_edge(Edge(f"e_{delay}_{lag}", delay, lag, {"weight": GAINS[i][j] * OUTPUT_SCALE}))
            graph.add_edge(Edge(f"e_{lag}_{total}", lag, total, {"weight": 1.0}))
        graph.add_edge(Edge(f"e_{total}_y{i}", total, f"y{i}", {"weight": 1.0}))
    return graph
