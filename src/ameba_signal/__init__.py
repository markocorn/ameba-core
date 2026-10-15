"""Mathematical-signal interpretation of AMEBA graphs."""

from .evaluator import CRITERIA, Dataset, SignalEvaluator
from .policy import (
    COARSE_ALLELES,
    PARAMETER_ALPHABETS,
    SignalGraphPolicy,
    interface_scaffold,
)
from .serialization import dataset_dumps, dataset_loads
from .simulator import SignalSession, SignalSimulationError, SignalSimulator, simulates

__all__ = [
    "CRITERIA",
    "Dataset",
    "SignalEvaluator",
    "SignalGraphPolicy",
    "SignalSession",
    "SignalSimulationError",
    "SignalSimulator",
    "COARSE_ALLELES",
    "PARAMETER_ALPHABETS",
    "interface_scaffold",
    "simulates",
    "dataset_dumps",
    "dataset_loads",
]
