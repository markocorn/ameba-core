"""Validated TOML configuration for AMEBA application workflows."""

from __future__ import annotations

import tomllib
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Mapping

from ameba_graph import EvolutionConfig
from ameba_signal.operators import EVOLVABLE_OPERATORS

CONFIG_SCHEMA_VERSION = 1


@dataclass(frozen=True, slots=True)
class SignalConfig:
    evolvable_kinds: tuple[str, ...] = EVOLVABLE_OPERATORS
    weight_min: float = -2.0
    weight_max: float = 2.0


@dataclass(frozen=True, slots=True)
class IslandConfig:
    name: str
    evolvable_kinds: tuple[str, ...]


@dataclass(frozen=True, slots=True)
class AppConfig:
    generations: int
    seed: int
    simulation_workers: int
    evolution: EvolutionConfig
    signal: SignalConfig
    islands: tuple[IslandConfig, ...] = ()

    @classmethod
    def from_mapping(cls, payload: Mapping[str, Any]) -> AppConfig:
        version = _integer(payload.get("schema_version"), "schema_version")
        if version != CONFIG_SCHEMA_VERSION:
            raise ValueError(
                f"Unsupported configuration version {version}; expected {CONFIG_SCHEMA_VERSION}"
            )
        generations = _integer(payload.get("generations", 100), "generations")
        if generations < 0:
            raise ValueError("generations cannot be negative")
        seed = _integer(payload.get("seed", 0), "seed")
        simulation_workers = _integer(
            payload.get("simulation_workers", 1), "simulation_workers"
        )
        if simulation_workers < 1:
            raise ValueError("simulation_workers must be positive")

        evo = _mapping(payload.get("evolution", {}), "evolution")
        evolution = EvolutionConfig(
            population_size=_integer(evo.get("population_size", 20), "population_size"),
            elite_size=_integer(evo.get("elite_size", 1), "elite_size"),
            tournament_size=_integer(evo.get("tournament_size", 3), "tournament_size"),
            mutation_rate=_number(evo.get("mutation_rate", 1.0), "mutation_rate"),
            crossover_rate=_number(evo.get("crossover_rate", 0.5), "crossover_rate"),
            topology_protection_generations=_integer(
                evo.get("topology_protection_generations", 0),
                "topology_protection_generations",
            ),
            topology_protection_size=_integer(
                evo.get("topology_protection_size", 0), "topology_protection_size"
            ),
            topology_parent_rate=_number(
                evo.get("topology_parent_rate", 0.0), "topology_parent_rate"
            ),
            island_count=_integer(evo.get("island_count", 1), "island_count"),
            migration_interval=_integer(
                evo.get("migration_interval", 0), "migration_interval"
            ),
            migration_size=_integer(evo.get("migration_size", 0), "migration_size"),
        )

        signal_data = _mapping(payload.get("signal", {}), "signal")
        kinds_data = signal_data.get("evolvable_kinds", SignalConfig().evolvable_kinds)
        if not isinstance(kinds_data, (list, tuple)) or not all(
            isinstance(item, str) and item for item in kinds_data
        ):
            raise ValueError("signal.evolvable_kinds must be an array of non-empty strings")
        signal = SignalConfig(
            evolvable_kinds=tuple(kinds_data),
            weight_min=_number(signal_data.get("weight_min", -2.0), "weight_min"),
            weight_max=_number(signal_data.get("weight_max", 2.0), "weight_max"),
        )
        if signal.weight_min > signal.weight_max:
            raise ValueError("signal weight_min cannot exceed weight_max")

        raw_islands = payload.get("islands", [])
        if not isinstance(raw_islands, (list, tuple)):
            raise ValueError("islands must be an array of tables")
        islands = []
        for index, raw_island in enumerate(raw_islands):
            data = _mapping(raw_island, f"islands[{index}]")
            name = data.get("name", f"Island {index + 1}")
            if not isinstance(name, str) or not name:
                raise ValueError(f"islands[{index}].name must be a non-empty string")
            island_kinds = data.get("evolvable_kinds", signal.evolvable_kinds)
            if not isinstance(island_kinds, (list, tuple)) or not all(
                isinstance(item, str) and item for item in island_kinds
            ) or not island_kinds:
                raise ValueError(
                    f"islands[{index}].evolvable_kinds must be a non-empty string array"
                )
            islands.append(IslandConfig(name, tuple(island_kinds)))
        if islands and len(islands) != evolution.island_count:
            raise ValueError("islands must contain one table per configured island")
        return cls(
            generations, seed, simulation_workers, evolution, signal, tuple(islands)
        )


def load_config(path: str | Path) -> AppConfig:
    with Path(path).open("rb") as stream:
        payload = tomllib.load(stream)
    return AppConfig.from_mapping(payload)


def _mapping(value: object, name: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        raise ValueError(f"{name} must be a table")
    return value


def _integer(value: object, name: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int):
        raise ValueError(f"{name} must be an integer")
    return value


def _number(value: object, name: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"{name} must be numeric")
    return float(value)

