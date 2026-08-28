"""Versioned signal-dataset serialization with Java-era compatibility."""

from __future__ import annotations

import json
from typing import Any, Mapping

from ameba_graph.serialization import SerializationError

from .evaluator import Dataset

DATASET_SCHEMA = "ameba.signal-dataset"
DATASET_SCHEMA_VERSION = 1


def dataset_to_dict(dataset: Dataset) -> dict[str, Any]:
    return {
        "schema": DATASET_SCHEMA,
        "version": DATASET_SCHEMA_VERSION,
        "inputs": [list(row) for row in dataset.inputs],
        "outputs": [list(row) for row in dataset.outputs],
    }


def dataset_from_dict(payload: Mapping[str, Any]) -> Dataset:
    # The Java engine.json format had only inputs and outputs. Accept it as a
    # migration format while always writing the versioned Python schema.
    if "schema" in payload or "version" in payload:
        if payload.get("schema") != DATASET_SCHEMA:
            raise SerializationError(f"Expected schema {DATASET_SCHEMA!r}")
        if payload.get("version") != DATASET_SCHEMA_VERSION:
            raise SerializationError(
                f"Unsupported dataset version {payload.get('version')!r}; "
                f"expected {DATASET_SCHEMA_VERSION}"
            )
    try:
        inputs = _rows(payload.get("inputs"), "inputs")
        outputs = _rows(payload.get("outputs"), "outputs")
        return Dataset(inputs, outputs)
    except (TypeError, ValueError) as exc:
        raise SerializationError(f"Invalid signal dataset: {exc}") from exc


def dataset_dumps(dataset: Dataset, *, indent: int | None = 2) -> str:
    return json.dumps(dataset_to_dict(dataset), allow_nan=False, indent=indent, sort_keys=True)


def dataset_loads(data: str) -> Dataset:
    try:
        payload = json.loads(data)
    except json.JSONDecodeError as exc:
        raise SerializationError(f"Invalid JSON: {exc}") from exc
    if not isinstance(payload, Mapping):
        raise SerializationError("Dataset document must be an object")
    return dataset_from_dict(payload)


def _rows(value: object, name: str) -> tuple[tuple[float, ...], ...]:
    if not isinstance(value, list):
        raise SerializationError(f"Dataset {name} must be an array")
    rows: list[tuple[float, ...]] = []
    for row in value:
        if not isinstance(row, list):
            raise SerializationError(f"Dataset {name} rows must be arrays")
        if any(isinstance(item, bool) or not isinstance(item, (int, float)) for item in row):
            raise SerializationError(f"Dataset {name} values must be numeric")
        rows.append(tuple(float(item) for item in row))
    return tuple(rows)

