"""Integer-gene encoding of AMEBA graphs, searched by a classical GA.

A model is a flat sequence of integers. Each integer is a key into a fixed,
append-only gene dictionary (:mod:`.dictionary`); a deterministic, total
decoder turns any such sequence into a valid signal graph (:mod:`.decoding`);
mutation and crossover act on the integers alone (:mod:`.variation`); and a
generational GA searches over them (:mod:`.ga`). See docs/genome-encoding.md.
"""

from .codebook import CODEBOOKS, Codebook, dense, hashed, ordered
from .decoding import Genome, Grammar, decode, expressed_nodes, parse
from .dictionary import DICTIONARY_VERSION, GENES, Gene, describe, label
from .fitness import GenomeFitness, model_size, phenotype_key
from .numeric import DEFAULT_NUMERIC_WEIGHTS, NUMERIC_MUTATIONS, numeric_mutate, one_point
from .ga import GAConfig, GAResult, GeneticAlgorithm
from .variation import (
    DEFAULT_MUTATION_WEIGHTS,
    MUTATIONS,
    allowed_at,
    boundaries,
    mutate,
    random_genome,
    random_record,
    splice,
)

__all__ = [
    "CODEBOOKS",
    "Codebook",
    "DEFAULT_NUMERIC_WEIGHTS",
    "NUMERIC_MUTATIONS",
    "dense",
    "hashed",
    "numeric_mutate",
    "one_point",
    "ordered",
    "DEFAULT_MUTATION_WEIGHTS",
    "DICTIONARY_VERSION",
    "GAConfig",
    "GAResult",
    "GENES",
    "Gene",
    "Genome",
    "GenomeFitness",
    "GeneticAlgorithm",
    "Grammar",
    "MUTATIONS",
    "allowed_at",
    "boundaries",
    "decode",
    "describe",
    "expressed_nodes",
    "label",
    "model_size",
    "mutate",
    "parse",
    "phenotype_key",
    "random_genome",
    "random_record",
    "splice",
]
