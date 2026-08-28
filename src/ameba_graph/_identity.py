"""Deterministic identity helpers owned by the generic graph layer."""

from collections.abc import Container


def next_id(prefix: str, existing: Container[str]) -> str:
    """Return the first unused `<prefix><integer>` identifier."""
    index = 0
    while f"{prefix}{index}" in existing:
        index += 1
    return f"{prefix}{index}"

