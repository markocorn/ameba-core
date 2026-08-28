"""Local parameter search applied to a freshly changed topology.

A structural change is scored the instant it is made, while its parameters are
still whatever the mutation happened to produce. That is not a fair reading of
what the new shape can do: a topology that would beat its parent once tuned is
discarded for a birth score it was never going to keep.

This runs a short parameter-only hill climb before the candidate competes. It
always spends a minimum effort, then keeps going for as long as the score is
still moving, and stops once it stalls.
"""

from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass
from numbers import Real
from random import Random
from typing import Sequence

from .model import Graph, GraphError
from .protocols import Evaluator, GraphMutation, GraphPolicy


@dataclass(frozen=True, slots=True)
class RefinementConfig:
    """Effort budget for one refinement.

    ``min_steps`` is the fair hearing every new topology gets regardless of
    early results. After that ``patience`` decides: refinement continues while
    the score keeps improving and stops once that many attempts in a row fail
    to. ``max_steps`` is the hard ceiling, because a candidate that improves by
    a sliver every time would otherwise never stop.
    """

    min_steps: int = 8
    patience: int = 6
    max_steps: int = 40
    min_improvement: float = 0.0
    scales: tuple[float, ...] = (1.0, 0.25, 0.0625)

    def __post_init__(self) -> None:
        if self.min_steps < 0:
            raise ValueError("min_steps cannot be negative")
        if self.patience < 1:
            raise ValueError("patience must be positive")
        if self.max_steps < self.min_steps:
            raise ValueError("max_steps cannot be below min_steps")
        if self.min_improvement < 0.0:
            raise ValueError("min_improvement cannot be negative")
        if not self.scales or any(scale <= 0.0 or scale > 1.0 for scale in self.scales):
            raise ValueError("scales must contain values greater than zero and at most one")


@dataclass(frozen=True, slots=True)
class Refinement:
    graph: Graph
    score: float
    steps: int
    improvements: int


class ParameterRefiner:
    """Hill-climb parameter mutations for as long as they keep paying off."""

    def __init__(
        self,
        mutations: Sequence[GraphMutation],
        config: RefinementConfig | None = None,
    ) -> None:
        if not mutations:
            raise ValueError("At least one parameter mutation is required")
        self.mutations = tuple(mutations)
        self.config = config or RefinementConfig()

    def refine(
        self,
        graph: Graph,
        evaluator: Evaluator,
        policy: GraphPolicy,
        rng: Random,
    ) -> Refinement:
        best = graph
        best_score = float(evaluator.evaluate(graph))
        stalled = 0
        steps = 0
        improvements = 0

        while steps < self.config.max_steps:
            # Patience only applies once the topology has had its fair hearing.
            if steps >= self.config.min_steps and stalled >= self.config.patience:
                break
            steps += 1
            try:
                candidate = rng.choice(self.mutations).mutate(best, policy, rng)
                scale = rng.choice(self.config.scales)
                if scale != 1.0:
                    candidate = _scaled_parameters(best, candidate, scale)
                    policy.validate(candidate)
            except (GraphError, ValueError, RuntimeError):
                stalled += 1
                continue

            score = float(evaluator.evaluate(candidate))
            if score < best_score - self.config.min_improvement:
                best, best_score = candidate, score
                improvements += 1
                stalled = 0
            else:
                stalled += 1

        return Refinement(best, best_score, steps, improvements)


def _scaled_parameters(original: Graph, proposed: Graph, scale: float) -> Graph:
    """Interpolate numeric parameter changes without knowing their domain meaning."""
    if set(original.nodes) != set(proposed.nodes) or set(original.edges) != set(proposed.edges):
        return proposed
    candidate = deepcopy(proposed)
    for old, new in (
        *((original.nodes[key], candidate.nodes[key]) for key in sorted(original.nodes)),
        *((original.edges[key], candidate.edges[key]) for key in sorted(original.edges)),
    ):
        for name, new_value in new.attributes.items():
            old_value = old.attributes.get(name)
            if (
                isinstance(old_value, Real)
                and not isinstance(old_value, bool)
                and isinstance(new_value, Real)
                and not isinstance(new_value, bool)
                and old_value != new_value
            ):
                value = float(old_value) + scale * (float(new_value) - float(old_value))
                new.attributes[name] = round(value) if isinstance(old_value, int) else value
    return candidate
