"""Domain-neutral graph representation and evolution contracts."""

from .evaluation import ParsimoniousEvaluator, live_nodes, prune
from .model import Edge, Graph, GraphError, Node
from .protocols import Evaluator, GraphCrossover, GraphMutation, GraphPolicy
from .evolution import (
    EvolutionCheckpoint,
    EvolutionConfig,
    EvolutionEngine,
    EvolutionResult,
    Individual,
)
from .refinement import ParameterRefiner, Refinement, RefinementConfig
from .generation import GenerationConfig, GraphGenerationError, GraphGenerator

__all__ = [
    "Edge",
    "Evaluator",
    "EvolutionConfig",
    "EvolutionCheckpoint",
    "EvolutionEngine",
    "EvolutionResult",
    "GenerationConfig",
    "Graph",
    "GraphCrossover",
    "GraphError",
    "GraphGenerationError",
    "GraphGenerator",
    "GraphMutation",
    "GraphPolicy",
    "Individual",
    "Node",
    "ParameterRefiner",
    "Refinement",
    "RefinementConfig",
    "ParsimoniousEvaluator",
    "live_nodes",
    "prune",
]
