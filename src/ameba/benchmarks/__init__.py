"""Identification benchmarks, single-input and multi-channel alike.

Every benchmark drives a plant with a control sequence and scores a model that
sees only u(k), so the model must carry its own state through delay nodes. The
benchmarks form a difficulty ladder:

``linear``
    A third-order linear plant. Three unit delays and one weighted sum. The
    smallest honest dynamic identification task.

``narendra``
    The Narendra–Parthasarathy nonlinear plant. Needs a five-way product, a
    division, and three steps of output history.

``quadruple_tank``
    Johansson's quadruple-tank process: two pumps, two measured levels, and a
    transmission zero whose sign is set by the valve split. The first
    multi-channel rung, registered twice — ``tank-minphase`` and
    ``tank-nonminphase`` — because the non-minimum-phase setting inverts the
    initial response, which is the part a memoryless model cannot fake.

``wood_berry``
    The Wood–Berry distillation column: two inputs, two outputs, four
    first-order paths each with its own dead time, strongly interacting. Linear,
    so it keeps an exact reference graph -- the MIMO rung on which a search can
    be checked for finding *the* model rather than only beating the floor.

A method that fails ``linear`` has a search or delay-handling problem. A method
that passes ``linear`` and fails ``narendra`` has a structural-search problem.
A method that passes both and fails the tank has a coupling problem rather than
a single-loop one.
"""

from . import linear, narendra, quadruple_tank, wood_berry
from .common import (
    BASIC_OPERATORS,
    DEFAULT_NODE_PENALTY,
    FULL_OPERATORS,
    LINEAR_OPERATORS,
    CONTROL_INPUT,
    RESPONSE_OUTPUT,
    Benchmark,
    MultiChannelTrajectory,
    PlantTrajectory,
    Trajectory,
    add_delay_chain,
    add_output,
    benchmark_crossover,
    benchmark_engine,
    benchmark_policy,
    channel_evaluation_controls,
    channel_step_controls,
    channel_training_controls,
    crossover_names,
    dynamic_population,
    evaluation_controls,
    identification_dataset,
    memoryless_floor,
    multi_channel_dataset,
    multi_channel_floor,
    mutation_names,
    parameter_mutations,
    seed_graph,
    static_seed_graph,
    static_gain_floor,
    stateless_floor,
    stateless_operators,
    step_controls,
    multisine_controls,
    structural_mutations,
    training_controls,
    trajectory_dataset,
    trajectory_floor,
    trajectory_interface,
    trajectory_rows,
    tuner_mutations,
    tuner_names,
)

#: The registered rungs, keyed by the name the scripts select with
#: ``--benchmark``. The catalogue lives here rather than in either script so the
#: two cannot drift apart on a title, a channel count, or a sample time.
BENCHMARKS = {
    "linear": Benchmark(
        "Third-order linear plant", linear.linear_trajectory, linear.reference_graph
    ),
    "narendra": Benchmark(
        "Narendra plant",
        narendra.narendra_trajectory,
        narendra.reference_graph,
        initialization="dynamic",
    ),
    "wood-berry": Benchmark(
        "Wood–Berry distillation column",
        wood_berry.wood_berry_trajectory,
        wood_berry.reference_graph,
        initialization="dynamic",
        channels=2,
        time_step=wood_berry.TIME_STEP,
        # A memoryless search reaches 0.97 of the closed form (300 steps,
        # hold 10): estimator noise, as on the linear rung, so the closed
        # form stands as the criterion.
        linear_floor_is_strict=True,
    ),
    "tank-minphase": Benchmark(
        "Quadruple tank, minimum phase",
        quadruple_tank.minimum_phase_trajectory,
        # Dynamic for the same reason as Narendra: the static seed sits on the
        # memoryless floor, which is the plateau these rungs have to escape.
        initialization="dynamic",
        channels=2,
        time_step=quadruple_tank.TIME_STEP,
        # A memoryless search reaches 0.62 of the linear floor here, so the
        # closed form is not a pass criterion on this plant.
        linear_floor_is_strict=False,
    ),
    "tank-nonminphase": Benchmark(
        "Quadruple tank, non-minimum phase",
        quadruple_tank.non_minimum_phase_trajectory,
        initialization="dynamic",
        channels=2,
        time_step=quadruple_tank.TIME_STEP,
        # 0.31 of the linear floor is reachable with no state at all.
        linear_floor_is_strict=False,
    ),
}

__all__ = [
    "BASIC_OPERATORS",
    "BENCHMARKS",
    "Benchmark",
    "DEFAULT_NODE_PENALTY",
    "FULL_OPERATORS",
    "LINEAR_OPERATORS",
    "CONTROL_INPUT",
    "RESPONSE_OUTPUT",
    "MultiChannelTrajectory",
    "PlantTrajectory",
    "Trajectory",
    "add_delay_chain",
    "add_output",
    "benchmark_crossover",
    "benchmark_engine",
    "benchmark_policy",
    "channel_evaluation_controls",
    "channel_step_controls",
    "channel_training_controls",
    "crossover_names",
    "dynamic_population",
    "evaluation_controls",
    "identification_dataset",
    "memoryless_floor",
    "multi_channel_dataset",
    "multi_channel_floor",
    "linear",
    "mutation_names",
    "parameter_mutations",
    "narendra",
    "quadruple_tank",
    "wood_berry",
    "seed_graph",
    "static_seed_graph",
    "static_gain_floor",
    "stateless_floor",
    "stateless_operators",
    "step_controls",
    "multisine_controls",
    "structural_mutations",
    "training_controls",
    "trajectory_dataset",
    "trajectory_floor",
    "trajectory_interface",
    "trajectory_rows",
    "tuner_mutations",
    "tuner_names",
]
