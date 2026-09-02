"""Versioned persistence for resumable evolutionary runs."""

from __future__ import annotations

import json
import math
from random import Random
from typing import Any, Mapping

from .evolution import EvolutionCheckpoint, Individual
from .serialization import SerializationError, graph_from_dict, graph_to_dict

CHECKPOINT_SCHEMA = "ameba.evolution-checkpoint"
CHECKPOINT_SCHEMA_VERSION = 3


def checkpoint_to_dict(checkpoint: EvolutionCheckpoint) -> dict[str, Any]:
    return {
        "schema": CHECKPOINT_SCHEMA,
        "version": CHECKPOINT_SCHEMA_VERSION,
        "generation": checkpoint.generation,
        "random_state": _tuples_to_lists(checkpoint.random_state),
        "island_sizes": list(checkpoint.island_sizes),
        "population": [
            {
                "score": _score_to_json(individual.score),
                "raw_score": (
                    _score_to_json(individual.raw_score)
                    if individual.raw_score is not None
                    else None
                ),
                "topology_age": individual.topology_age,
                "graph": graph_to_dict(individual.graph),
            }
            for individual in checkpoint.population
        ],
    }


def checkpoint_from_dict(payload: Mapping[str, Any]) -> EvolutionCheckpoint:
    if payload.get("schema") != CHECKPOINT_SCHEMA:
        raise SerializationError(f"Expected schema {CHECKPOINT_SCHEMA!r}")
    version = payload.get("version")
    if version not in (1, 2, CHECKPOINT_SCHEMA_VERSION):
        raise SerializationError(
            f"Unsupported checkpoint version {version!r}; "
            f"expected 1, 2, or {CHECKPOINT_SCHEMA_VERSION}"
        )
    generation = payload.get("generation")
    if not isinstance(generation, int) or isinstance(generation, bool) or generation < 0:
        raise SerializationError("Checkpoint generation must be a non-negative integer")
    raw_population = payload.get("population")
    if not isinstance(raw_population, list) or not raw_population:
        raise SerializationError("Checkpoint population must be a non-empty array")

    population: list[Individual] = []
    for raw_item in raw_population:
        if not isinstance(raw_item, Mapping):
            raise SerializationError("Checkpoint population item must be an object")
        graph_data = raw_item.get("graph")
        if not isinstance(graph_data, Mapping):
            raise SerializationError("Checkpoint graph must be an object")
        topology_age = raw_item.get("topology_age", 0)
        if (
            not isinstance(topology_age, int)
            or isinstance(topology_age, bool)
            or topology_age < 0
        ):
            raise SerializationError("Checkpoint topology_age must be a non-negative integer")
        population.append(
            Individual(
                graph_from_dict(graph_data),
                _score_from_json(raw_item.get("score")),
                topology_age,
                (
                    _score_from_json(raw_item.get("raw_score"))
                    if version == 3 and raw_item.get("raw_score") is not None
                    else None
                ),
            )
        )

    state = _lists_to_tuples(payload.get("random_state"))
    if not isinstance(state, tuple):
        raise SerializationError("Checkpoint random_state must be an array")
    try:
        Random().setstate(state)
    except (TypeError, ValueError) as exc:
        raise SerializationError(f"Invalid random state: {exc}") from exc
    raw_sizes = payload.get("island_sizes", []) if version in (2, 3) else []
    if (
        not isinstance(raw_sizes, list)
        or any(not isinstance(size, int) or isinstance(size, bool) or size < 1 for size in raw_sizes)
        or (raw_sizes and sum(raw_sizes) != len(population))
    ):
        raise SerializationError("Checkpoint island_sizes must partition the population")
    return EvolutionCheckpoint(tuple(population), generation, state, tuple(raw_sizes))


def checkpoint_dumps(checkpoint: EvolutionCheckpoint, *, indent: int | None = 2) -> str:
    try:
        return json.dumps(
            checkpoint_to_dict(checkpoint),
            allow_nan=False,
            indent=indent,
            sort_keys=True,
        )
    except (TypeError, ValueError) as exc:
        raise SerializationError(f"Checkpoint contains non-JSON data: {exc}") from exc


def checkpoint_loads(data: str) -> EvolutionCheckpoint:
    try:
        payload = json.loads(data)
    except json.JSONDecodeError as exc:
        raise SerializationError(f"Invalid JSON: {exc}") from exc
    if not isinstance(payload, Mapping):
        raise SerializationError("Checkpoint document must be an object")
    return checkpoint_from_dict(payload)


def _score_to_json(score: float) -> float | str:
    if math.isnan(score):
        return "NaN"
    if score == math.inf:
        return "Infinity"
    if score == -math.inf:
        return "-Infinity"
    return score


def _score_from_json(value: object) -> float:
    if value == "NaN":
        return math.nan
    if value == "Infinity":
        return math.inf
    if value == "-Infinity":
        return -math.inf
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise SerializationError("Checkpoint score must be numeric or a special score string")
    return float(value)


def _tuples_to_lists(value: object) -> object:
    if isinstance(value, tuple):
        return [_tuples_to_lists(item) for item in value]
    return value


def _lists_to_tuples(value: object) -> object:
    if isinstance(value, list):
        return tuple(_lists_to_tuples(item) for item in value)
    return value
