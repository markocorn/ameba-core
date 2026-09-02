import math
import unittest

from ameba.cli import example_graph
from ameba_graph import EvolutionConfig, EvolutionEngine, OscillatingParsimony
from ameba_graph.checkpoint import checkpoint_dumps, checkpoint_loads
from ameba_graph.crossover import UniformGraphCrossover
from ameba_graph.mutation import SplitEdge
from ameba_graph.serialization import SerializationError, graph_dumps, graph_loads
from ameba_signal import Dataset, SignalEvaluator, SignalGraphPolicy
from ameba_signal.serialization import dataset_dumps, dataset_loads


class SerializationTests(unittest.TestCase):
    def test_graph_round_trip_is_deterministic_and_independent(self) -> None:
        original = example_graph()
        encoded = graph_dumps(original)
        restored = graph_loads(encoded)
        self.assertEqual(encoded, graph_dumps(restored))
        restored.nodes["x"].attributes["index"] = 9
        self.assertEqual(0, original.nodes["x"].attributes["index"])

    def test_graph_rejects_unknown_schema_version(self) -> None:
        encoded = graph_dumps(example_graph()).replace('"version": 1', '"version": 999')
        with self.assertRaisesRegex(SerializationError, "Unsupported"):
            graph_loads(encoded)

    def test_dataset_reads_legacy_shape_and_writes_versioned_shape(self) -> None:
        dataset = dataset_loads('{"inputs": [[1, 2]], "outputs": [[3]]}')
        self.assertEqual(((1.0, 2.0),), dataset.inputs)
        self.assertIn('"schema": "ameba.signal-dataset"', dataset_dumps(dataset))

    def test_checkpoint_round_trip_resumes_exact_random_sequence(self) -> None:
        dataset = Dataset(
            inputs=((0.0,), (1.0,), (2.0,)),
            outputs=((1.0,), (3.0,), (5.0,)),
        )
        policy = SignalGraphPolicy(weight_range=(0.5, 1.5))
        config = EvolutionConfig(
            population_size=4,
            elite_size=1,
            tournament_size=2,
            mutation_rate=1.0,
            crossover_rate=0.5,
            topology_protection_generations=3,
            topology_protection_size=1,
            topology_parent_rate=0.25,
        )
        initial = [example_graph() for _ in range(4)]

        def engine(seed: int) -> EvolutionEngine:
            return EvolutionEngine(
                SignalEvaluator(dataset),
                policy,
                mutations=[SplitEdge()],
                crossover=UniformGraphCrossover(),
                config=config,
                seed=seed,
            )

        uninterrupted = engine(23).run(initial, generations=5)
        first_engine = engine(23)
        partial = first_engine.run(initial, generations=2)
        encoded = checkpoint_dumps(first_engine.checkpoint(partial))
        restored = checkpoint_loads(encoded)
        resumed = engine(999).resume(restored, generations=3)

        self.assertEqual(5, resumed.generations)
        self.assertEqual(
            [item.score for item in uninterrupted.population],
            [item.score for item in resumed.population],
        )
        self.assertEqual(
            [graph_dumps(item.graph) for item in uninterrupted.population],
            [graph_dumps(item.graph) for item in resumed.population],
        )
        self.assertEqual(
            [item.topology_age for item in uninterrupted.population],
            [item.topology_age for item in resumed.population],
        )

    def test_checkpoint_supports_infinite_invalid_scores(self) -> None:
        config = EvolutionConfig(population_size=1, tournament_size=1)
        policy = SignalGraphPolicy()
        dataset = Dataset(inputs=((1.0,),), outputs=((1.0,),))
        engine = EvolutionEngine(SignalEvaluator(dataset), policy, [], config=config, seed=1)
        result = engine.run([example_graph()], generations=0)
        checkpoint = engine.checkpoint(result)
        invalid = type(checkpoint)(
            (type(checkpoint.population[0])(checkpoint.population[0].graph, math.inf),),
            checkpoint.generation,
            checkpoint.random_state,
        )
        restored = checkpoint_loads(checkpoint_dumps(invalid))
        self.assertEqual(math.inf, restored.best.score)

    def test_island_checkpoint_resume_matches_uninterrupted_run(self) -> None:
        dataset = Dataset(inputs=((0.0,), (1.0,)), outputs=((1.0,), (3.0,)))
        policy = SignalGraphPolicy(weight_range=(0.5, 1.5))
        config = EvolutionConfig(
            population_size=6,
            island_count=2,
            elite_size=1,
            tournament_size=2,
            mutation_rate=1.0,
            crossover_rate=0.0,
            migration_interval=2,
            migration_size=1,
        )
        initial = [example_graph() for _ in range(6)]

        def engine(seed: int) -> EvolutionEngine:
            return EvolutionEngine(
                SignalEvaluator(dataset), policy, [SplitEdge()], config=config, seed=seed,
                fitness_shaper=OscillatingParsimony(
                    expansion_generations=2,
                    compression_generations=2,
                    compression_node_weight=0.05,
                ),
            )

        uninterrupted = engine(13).run(initial, 5)
        first_engine = engine(13)
        partial = first_engine.run(initial, 3)
        restored = checkpoint_loads(
            checkpoint_dumps(first_engine.checkpoint(partial))
        )
        resumed = engine(999).resume(restored, 2)

        self.assertEqual((3, 3), restored.island_sizes)
        self.assertTrue(all(item.raw_score is not None for item in restored.population))
        self.assertEqual(
            [graph_dumps(item.graph) for island in uninterrupted.islands for item in island],
            [graph_dumps(item.graph) for island in resumed.islands for item in island],
        )
        self.assertEqual(
            [item.score for island in uninterrupted.islands for item in island],
            [item.score for island in resumed.islands for item in island],
        )


if __name__ == "__main__":
    unittest.main()
