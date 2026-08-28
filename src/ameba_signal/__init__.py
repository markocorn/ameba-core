"""Mathematical-signal interpretation of AMEBA graphs."""

from .evaluator import CRITERIA, Dataset, SignalEvaluator
from .policy import SignalGraphPolicy, interface_scaffold
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
    "interface_scaffold",
    "simulates",
    "dataset_dumps",
    "dataset_loads",
]
