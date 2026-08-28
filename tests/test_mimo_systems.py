"""Multiple-input/multiple-output systems.

Graphs are built as explicit state-space models

    x(k) = A x(k-1) + B u(k)
    y(k) = C x(k)   + D u(k)

and checked three ways: against closed-form matrix powers where the state
matrix admits one, against the analytic DC gain, and against the defining
properties of a linear system -- superposition, homogeneity, and time
invariance. Those last three need no reference implementation at all, which
makes them the strongest evidence available for a coupled system.
"""

import math
import unittest

from ameba_graph import Edge, Graph, Node
from ameba_signal import SignalSimulationError, SignalSimulator

Matrix = list[list[float]]


def state_space_graph(
    a: Matrix, b: Matrix, c: Matrix, d: Matrix, inputs: int | None = None
) -> Graph:
    """Build a graph realising the state-space system (A, B, C, D).

    ``inputs`` only needs supplying for a stateless system, where B has no rows
    to count columns from.
    """
    states = len(a)
    if inputs is None:
        inputs = len(b[0]) if b else (len(d[0]) if d else 0)
    outputs = len(c)

    graph = Graph()
    for index in range(inputs):
        graph.add_node(Node(f"u{index}", "input", {"index": index}))
    for index in range(states):
        graph.add_node(Node(f"x{index}", "add"))
        graph.add_node(Node(f"d{index}", "delay", {"steps": 1, "initial": 0.0}))
        graph.add_edge(Edge(f"e_x{index}_d{index}", f"x{index}", f"d{index}"))

    # x_i(k) = sum_j A[i][j] x_j(k-1) + sum_m B[i][m] u_m(k)
    for row in range(states):
        for column in range(states):
            graph.add_edge(
                Edge(f"e_d{column}_x{row}", f"d{column}", f"x{row}", {"weight": a[row][column]})
            )
        for column in range(inputs):
            graph.add_edge(
                Edge(f"e_u{column}_x{row}", f"u{column}", f"x{row}", {"weight": b[row][column]})
            )

    # y_p(k) = sum_i C[p][i] x_i(k) + sum_m D[p][m] u_m(k)
    for row in range(outputs):
        graph.add_node(Node(f"y{row}", "add"))
        for column in range(states):
            graph.add_edge(
                Edge(f"e_x{column}_y{row}", f"x{column}", f"y{row}", {"weight": c[row][column]})
            )
        for column in range(inputs):
            graph.add_edge(
                Edge(f"e_u{column}_y{row}", f"u{column}", f"y{row}", {"weight": d[row][column]})
            )
        graph.add_node(Node(f"out{row}", "output", {"index": row}))
        graph.add_edge(Edge(f"e_y{row}_out{row}", f"y{row}", f"out{row}"))

    return graph


def run(graph: Graph, rows: list[tuple[float, ...]]) -> list[list[float]]:
    return SignalSimulator().run_series(graph, rows)


def zeros(rows: int, columns: int) -> Matrix:
    return [[0.0] * columns for _ in range(rows)]


class MatrixGainTests(unittest.TestCase):
    def test_a_three_input_two_output_gain_matrix(self) -> None:
        gain = [[1.0, 2.0, 3.0], [-1.0, 0.5, 0.0]]
        graph = state_space_graph(zeros(0, 0), [], gain, gain)

        for signal in ((1.0, 0.0, 0.0), (0.0, 1.0, 0.0), (2.0, -1.0, 4.0)):
            produced = run(graph, [signal])[0]
            for row in range(2):
                expected = sum(gain[row][col] * signal[col] for col in range(3))
                self.assertAlmostEqual(expected, produced[row], places=12, msg=f"row={row}")

    def test_one_input_fans_out_to_three_outputs(self) -> None:
        gain = [[2.0], [-3.0], [0.5]]
        graph = state_space_graph(zeros(0, 0), [], gain, gain)
        produced = run(graph, [(4.0,)])[0]
        self.assertEqual([8.0, -12.0, 2.0], produced)

    def test_outputs_are_emitted_in_index_order_regardless_of_creation_order(self) -> None:
        graph = Graph(
            nodes=[
                Node("u", "input", {"index": 0}),
                Node("a", "add"),
                Node("b", "add"),
                Node("c", "add"),
                Node("third", "output", {"index": 2}),
                Node("first", "output", {"index": 0}),
                Node("second", "output", {"index": 1}),
            ],
            edges=[
                Edge("e1", "u", "a", {"weight": 1.0}),
                Edge("e2", "u", "b", {"weight": 2.0}),
                Edge("e3", "u", "c", {"weight": 3.0}),
                Edge("e4", "c", "third"),
                Edge("e5", "a", "first"),
                Edge("e6", "b", "second"),
            ],
        )
        self.assertEqual([[1.0, 2.0, 3.0]], run(graph, [(1.0,)]))


class DecoupledChannelTests(unittest.TestCase):
    """A diagonal system must behave as two independent SISO systems."""

    def setUp(self) -> None:
        self.poles = (0.6, -0.4)
        self.graph = state_space_graph(
            [[self.poles[0], 0.0], [0.0, self.poles[1]]],
            [[1.0, 0.0], [0.0, 1.0]],
            [[1.0, 0.0], [0.0, 1.0]],
            zeros(2, 2),
        )

    def test_each_channel_follows_its_own_impulse_response(self) -> None:
        rows = [(1.0, 1.0)] + [(0.0, 0.0)] * 39
        produced = run(self.graph, rows)
        for k, values in enumerate(produced):
            for channel, pole in enumerate(self.poles):
                self.assertAlmostEqual(pole**k, values[channel], places=12, msg=f"k={k}")

    def test_driving_one_channel_leaves_the_other_silent(self) -> None:
        for driven in (0, 1):
            rows = [tuple(1.0 if index == driven else 0.0 for index in range(2))] * 30
            produced = run(self.graph, rows)
            quiet = 1 - driven
            self.assertTrue(all(values[quiet] == 0.0 for values in produced))
            self.assertGreater(max(abs(values[driven]) for values in produced), 0.5)


class CoupledStateSpaceTests(unittest.TestCase):
    def test_triangular_state_matrix_matches_its_closed_form_powers(self) -> None:
        """For A = [[a, b], [0, d]], A^k has an exact expression.

        A^k = [[a^k, b(a^k - d^k)/(a - d)], [0, d^k]]
        """
        a, b, d = 0.8, 0.5, 0.3
        graph = state_space_graph(
            [[a, b], [0.0, d]],
            [[1.0], [1.0]],
            [[1.0, 0.0], [0.0, 1.0]],
            zeros(2, 1),
        )
        rows = [(1.0,)] + [(0.0,)] * 39
        produced = run(graph, rows)

        # The impulse loads x(0) = B, then the state evolves as A^k x(0).
        for k, values in enumerate(produced):
            first = a**k + b * (a**k - d**k) / (a - d)
            second = d**k
            self.assertAlmostEqual(first, values[0], places=11, msg=f"k={k}")
            self.assertAlmostEqual(second, values[1], places=11, msg=f"k={k}")

    def test_cross_coupling_actually_couples_the_channels(self) -> None:
        """The mirror of the decoupled test: off-diagonal terms must transfer."""
        graph = state_space_graph(
            [[0.5, 0.4], [0.0, 0.5]],
            [[0.0], [1.0]],
            [[1.0, 0.0], [0.0, 1.0]],
            zeros(2, 1),
        )
        produced = run(graph, [(1.0,)] + [(0.0,)] * 20)
        # Input reaches state 1 only, but state 0 is driven through A[0][1].
        self.assertEqual(0.0, produced[0][0])
        self.assertGreater(max(abs(values[0]) for values in produced), 0.1)

    def test_the_steady_state_matches_the_analytic_dc_gain(self) -> None:
        """Under a constant input the output settles at C(I-A)^-1 B + D."""
        a = [[0.5, 0.2], [-0.1, 0.6]]
        b = [[1.0, 0.0], [0.0, 1.0]]
        c = [[1.0, 1.0], [2.0, -1.0]]
        d = [[0.1, 0.0], [0.0, 0.3]]
        graph = state_space_graph(a, b, c, d)

        signal = (1.0, -2.0)
        produced = run(graph, [signal] * 400)[-1]

        # (I - A) inverted by the 2x2 closed form.
        p, q = 1.0 - a[0][0], -a[0][1]
        r, s = -a[1][0], 1.0 - a[1][1]
        determinant = p * s - q * r
        inverse = [[s / determinant, -q / determinant], [-r / determinant, p / determinant]]

        state = [
            sum(inverse[i][j] * sum(b[j][m] * signal[m] for m in range(2)) for j in range(2))
            for i in range(2)
        ]
        expected = [
            sum(c[o][i] * state[i] for i in range(2))
            + sum(d[o][m] * signal[m] for m in range(2))
            for o in range(2)
        ]
        for index, value in enumerate(expected):
            self.assertAlmostEqual(value, produced[index], places=9, msg=f"out={index}")

    def test_a_direct_feedthrough_term_appears_immediately(self) -> None:
        """D bypasses the state, so it must show up on the very first step."""
        graph = state_space_graph(
            [[0.9]], [[1.0]], [[1.0]], [[5.0]]
        )
        produced = run(graph, [(1.0,), (0.0,)])
        # Step 0: D*u + C*x(0) where x(0) = B*u = 1.
        self.assertAlmostEqual(6.0, produced[0][0], places=12)
        self.assertAlmostEqual(0.9, produced[1][0], places=12)


class LinearSystemPropertyTests(unittest.TestCase):
    """Properties every linear system must satisfy, checked directly."""

    def setUp(self) -> None:
        self.graph = state_space_graph(
            [[0.7, 0.3], [-0.2, 0.5]],
            [[1.0, 0.5], [0.0, 1.0]],
            [[1.0, -1.0], [0.5, 2.0]],
            [[0.0, 0.1], [0.2, 0.0]],
        )
        self.first = [(math.sin(0.2 * k), math.cos(0.1 * k)) for k in range(60)]
        self.second = [(0.4 * k % 1.3 - 0.5, math.sin(0.7 * k)) for k in range(60)]

    def test_superposition_holds_across_both_inputs(self) -> None:
        combined = [
            (a[0] + b[0], a[1] + b[1]) for a, b in zip(self.first, self.second)
        ]
        left = run(self.graph, self.first)
        right = run(self.graph, self.second)
        both = run(self.graph, combined)
        for k, (a, b, total) in enumerate(zip(left, right, both)):
            for index in range(2):
                self.assertAlmostEqual(
                    a[index] + b[index], total[index], places=10, msg=f"k={k}"
                )

    def test_scaling_the_input_scales_the_output(self) -> None:
        for factor in (0.0, 2.5, -3.0):
            scaled = [(factor * a, factor * b) for a, b in self.first]
            base = run(self.graph, self.first)
            produced = run(self.graph, scaled)
            for k, (reference, actual) in enumerate(zip(base, produced)):
                for index in range(2):
                    self.assertAlmostEqual(
                        factor * reference[index], actual[index], places=10, msg=f"k={k}"
                    )

    def test_the_system_is_time_invariant(self) -> None:
        """Delaying the input by n steps delays every output by exactly n."""
        shift = 7
        delayed = [(0.0, 0.0)] * shift + self.first
        base = run(self.graph, self.first)
        produced = run(self.graph, delayed)
        for k in range(len(base)):
            for index in range(2):
                self.assertAlmostEqual(
                    base[k][index], produced[k + shift][index], places=10, msg=f"k={k}"
                )

    def test_a_zero_input_produces_a_zero_output(self) -> None:
        produced = run(self.graph, [(0.0, 0.0)] * 40)
        self.assertTrue(all(value == 0.0 for values in produced for value in values))


class MimoErrorTests(unittest.TestCase):
    def test_a_missing_input_index_is_reported(self) -> None:
        graph = state_space_graph(
            [[0.5]], [[1.0, 1.0]], [[1.0]], zeros(1, 2)
        )
        with self.assertRaises(SignalSimulationError):
            run(graph, [(1.0,)])  # only one value supplied for two inputs

    def test_extra_supplied_inputs_are_ignored(self) -> None:
        graph = state_space_graph([[0.5]], [[1.0]], [[1.0]], zeros(1, 1))
        self.assertEqual(run(graph, [(1.0,)]), run(graph, [(1.0, 99.0, -5.0)]))

    def test_every_output_index_is_present_in_each_row(self) -> None:
        graph = state_space_graph(
            [[0.5, 0.0], [0.0, 0.5]],
            [[1.0], [1.0]],
            [[1.0, 0.0], [0.0, 1.0], [1.0, 1.0]],
            zeros(3, 1),
        )
        produced = run(graph, [(1.0,)] * 5)
        self.assertTrue(all(len(values) == 3 for values in produced))
        for values in produced:
            self.assertAlmostEqual(values[0] + values[1], values[2], places=12)


if __name__ == "__main__":
    unittest.main()
