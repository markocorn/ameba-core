"""Single-input/single-output identification benchmarks.

Every benchmark drives a plant with a scalar control sequence and scores a
model that sees only u(k), so the model must carry its own state through delay
nodes. The benchmarks form a difficulty ladder:

``linear``
    A third-order linear plant. Three unit delays and one weighted sum. The
    smallest honest dynamic identification task.

``narendra``
    The Narendra–Parthasarathy nonlinear plant. Needs a five-way product, a
    division, and three steps of output history.

A method that fails ``linear`` has a search or delay-handling problem. A method
that passes ``linear`` and fails ``narendra`` has a structural-search problem.
"""

from . import linear, narendra
from .common import (
    BASIC_OPERATORS,
    DEFAULT_NODE_PENALTY,
    FULL_OPERATORS,
    LINEAR_OPERATORS,
    CONTROL_INPUT,
    RESPONSE_OUTPUT,
    PlantTrajectory,
    add_delay_chain,
    add_output,
    benchmark_crossover,
    benchmark_engine,
    benchmark_policy,
    dynamic_population,
    evaluation_controls,
    identification_dataset,
    parameter_mutations,
    seed_graph,
    static_gain_floor,
    step_controls,
    structural_mutations,
    training_controls,
)

__all__ = [
    "BASIC_OPERATORS",
    "DEFAULT_NODE_PENALTY",
    "FULL_OPERATORS",
    "LINEAR_OPERATORS",
    "CONTROL_INPUT",
    "RESPONSE_OUTPUT",
    "PlantTrajectory",
    "add_delay_chain",
    "add_output",
    "benchmark_crossover",
    "benchmark_engine",
    "benchmark_policy",
    "dynamic_population",
    "evaluation_controls",
    "identification_dataset",
    "linear",
    "parameter_mutations",
    "narendra",
    "seed_graph",
    "static_gain_floor",
    "step_controls",
    "structural_mutations",
    "training_controls",
]
