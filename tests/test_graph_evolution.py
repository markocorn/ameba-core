import unittest
from random import Random

from ameba_graph import (
    Edge,
    EvolutionConfig,
    EvolutionEngine,
    GenerationConfig,
    Graph,
    GraphGenerator,
    Node,
    OscillatingParsimony,
)
from ameba_graph.crossover import UniformGraphCrossover
from ameba_graph.mutation import AddEdge, AddNode, RemoveEdge, RemoveNode
from ameba_graph.serialization import graph_dumps


class ArbitraryPolicy:
    """A deliberately non-signal domain used to test layer independence."""

    def create_node(self, rng: Random) -> Node:
        return Node("ignored", rng.choice(("red", "blue")), {"payload": rng.randrange(10)})

    def create_edge(self, source: str, target: str, rng: Random) -> Edge:
        return Edge("ignored", source, target, {"label": rng.choice(("a", "b"))})

    def can_connect(self, graph: Graph, source: str, target: str) -> bool:
        return source != target and not any(
            edge.source == source and edge.target == target for edge in graph.edges.values()
        )

    def validate(self, graph: Graph) -> None:
        graph.validate_structure()
        if not graph.nodes:
            raise ValueError("At least one node is required")


class TargetSizeEvaluator:
    def evaluate(self, graph: Graph) -> float:
        return float(abs(len(graph.nodes) - 4) + abs(len(graph.edges) - 3))


class PreferredSizeEvaluator:
    def __init__(self, size: int) -> None:
        self.size = size

    def evaluate(self, graph: Graph) -> float:
        return float(abs(len(graph.nodes) - self.size))


class GraphEvolutionTests(unittest.TestCase):
    def setUp(self) -> None:
        self.policy = ArbitraryPolicy()

    def test_generation_is_seeded_and_domain_neutral(self) -> None:
        generator = GraphGenerator(
            self.policy,
            GenerationConfig(min_nodes=3, max_nodes=3, edge_probability=0.5),
        )
        first = generator.generate(Random(42))
        second = generator.generate(Random(42))
        self.assertEqual(first.nodes, second.nodes)
        self.assertEqual(first.edges, second.edges)
        self.assertEqual(3, len(first.nodes))

    def test_structural_mutations_preserve_policy(self) -> None:
        rng = Random(3)
        graph = Graph(nodes=[Node("n0", "red"), Node("n1", "blue")])
        graph = AddEdge().mutate(graph, self.policy, rng)
        self.assertEqual(1, len(graph.edges))
        graph = AddNode().mutate(graph, self.policy, rng)
        self.assertEqual(3, len(graph.nodes))
        # AddNode connects what it adds, so the new node arrives with an edge.
        self.assertEqual(2, len(graph.edges))
        graph = RemoveEdge().mutate(graph, self.policy, rng)
        self.assertEqual(1, len(graph.edges))
        graph = RemoveNode().mutate(graph, self.policy, rng)
        self.policy.validate(graph)

    def test_uniform_crossover_returns_valid_independent_child(self) -> None:
        left = Graph(nodes=[Node("n0", "red"), Node("n1", "red")])
        right = Graph(nodes=[Node("n0", "blue"), Node("n2", "blue")])
        child = UniformGraphCrossover().cross(left, right, self.policy, Random(9))
        self.policy.validate(child)
        child.nodes[next(iter(child.nodes))].attributes["changed"] = True
        self.assertFalse(any("changed" in node.attributes for node in left.nodes.values()))
        self.assertFalse(any("changed" in node.attributes for node in right.nodes.values()))

    def test_evolution_is_reproducible_and_preserves_elite(self) -> None:
        initial = [
            Graph(nodes=[Node(f"n{i}", "red") for i in range(count)])
            for count in (1, 2, 3, 4, 5, 6)
        ]
        config = EvolutionConfig(
            population_size=6,
            elite_size=1,
            tournament_size=2,
            mutation_rate=1.0,
            crossover_rate=0.5,
        )

        def evolve():
            return EvolutionEngine(
                TargetSizeEvaluator(),
                self.policy,
                mutations=[AddNode(), RemoveNode(), AddEdge(), RemoveEdge()],
                crossover=UniformGraphCrossover(),
                config=config,
                seed=17,
            ).run(initial, generations=5)

        first = evolve()
        second = evolve()
        initial_best = min(TargetSizeEvaluator().evaluate(graph) for graph in initial)
        self.assertLessEqual(first.best.score, initial_best)
        self.assertEqual(
            [individual.score for individual in first.population],
            [individual.score for individual in second.population],
        )
        scores = [individual.score for individual in first.population]
        self.assertEqual(sorted(scores), scores)

    def test_topology_protection_keeps_distinct_young_lineages(self) -> None:
        initial = [
            Graph(nodes=[Node(f"n{i}", "red") for i in range(count)])
            for count in (1, 2, 3, 4)
        ]

        class PreferSmall:
            def evaluate(self, graph: Graph) -> float:
                return float(len(graph.nodes))

        engine = EvolutionEngine(
            PreferSmall(),
            self.policy,
            mutations=[],
            config=EvolutionConfig(
                population_size=4,
                elite_size=1,
                tournament_size=2,
                mutation_rate=0.0,
                crossover_rate=0.0,
                topology_protection_generations=3,
                topology_protection_size=2,
                topology_parent_rate=1.0,
            ),
            seed=5,
        )
        protected = list(engine.run(initial, 0).population)
        for expected_age in (1, 2, 3):
            protected = engine.step(protected)
            self.assertTrue(
                {1, 2, 3}.issubset({len(item.graph.nodes) for item in protected})
            )
            self.assertIn(expected_age, {item.topology_age for item in protected})

    def test_structural_change_resets_age_and_parameter_change_does_not(self) -> None:
        config = EvolutionConfig(
            population_size=1,
            elite_size=0,
            tournament_size=1,
            mutation_rate=1.0,
            crossover_rate=0.0,
        )
        initial = Graph(nodes=[Node("n0", "red")])
        structural = EvolutionEngine(
            TargetSizeEvaluator(), self.policy, [AddNode()], config=config, seed=2
        )
        self.assertEqual(0, structural.step(structural.run([initial], 0).population)[0].topology_age)

        class ChangePayload:
            def mutate(self, graph, policy, rng):
                child = graph.copy()
                child.nodes["n0"].attributes["payload"] = rng.random()
                return child

        parameter = EvolutionEngine(
            TargetSizeEvaluator(), self.policy, [ChangePayload()], config=config, seed=2
        )
        self.assertEqual(1, parameter.step(parameter.run([initial], 0).population)[0].topology_age)

    def test_invalid_topology_protection_configuration_is_rejected(self) -> None:
        invalid = (
            {"topology_protection_generations": -1},
            {"topology_protection_generations": 3, "topology_protection_size": 0},
            {"topology_protection_size": 1},
            {"topology_parent_rate": 0.2},
            {
                "topology_protection_generations": 3,
                "topology_protection_size": 1,
                "topology_parent_rate": 1.2,
            },
        )
        for values in invalid:
            with self.assertRaises(ValueError, msg=str(values)):
                EvolutionConfig(population_size=4, elite_size=1, **values)

    def test_parallel_simulation_matches_serial_simulation_exactly(self) -> None:
        initial = [
            Graph(nodes=[Node(f"n{i}", "red") for i in range(count)])
            for count in (1, 2, 3, 4, 5, 6)
        ]
        config = EvolutionConfig(
            population_size=6,
            elite_size=1,
            tournament_size=2,
            mutation_rate=1.0,
            crossover_rate=0.5,
            topology_protection_generations=3,
            topology_protection_size=1,
            topology_parent_rate=0.2,
        )

        def run(workers: int):
            with EvolutionEngine(
                TargetSizeEvaluator(),
                self.policy,
                [AddNode(), RemoveNode(), AddEdge(), RemoveEdge()],
                UniformGraphCrossover(),
                config,
                seed=31,
                simulation_workers=workers,
            ) as engine:
                return engine.run(initial, 4)

        serial, parallel = run(1), run(2)
        self.assertEqual(
            [(item.score, item.topology_age) for item in serial.population],
            [(item.score, item.topology_age) for item in parallel.population],
        )
        self.assertEqual(
            [graph_dumps(item.graph) for item in serial.population],
            [graph_dumps(item.graph) for item in parallel.population],
        )

    def test_parallel_worker_count_must_be_positive(self) -> None:
        with self.assertRaises(ValueError):
            EvolutionEngine(
                TargetSizeEvaluator(), self.policy, [], simulation_workers=0
            )

    def test_islands_evolve_independently_and_migrate_around_ring(self) -> None:
        initial = [
            Graph(nodes=[Node(f"n{i}", "red") for i in range(count)])
            for count in (1, 2, 3, 10, 11, 12)
        ]
        engine = EvolutionEngine(
            TargetSizeEvaluator(),
            self.policy,
            [],
            config=EvolutionConfig(
                population_size=6,
                island_count=2,
                elite_size=1,
                tournament_size=1,
                mutation_rate=0.0,
                crossover_rate=0.0,
                migration_interval=1,
                migration_size=1,
            ),
            seed=7,
        )
        result = engine.run(initial, 1)

        self.assertEqual(2, len(result.islands))
        self.assertTrue(any(len(item.graph.nodes) >= 10 for item in result.islands[0]))
        self.assertTrue(any(len(item.graph.nodes) <= 3 for item in result.islands[1]))
        self.assertEqual(min(item.score for item in result.population), result.best.score)

    def test_cross_island_exchange_creates_children_without_moving_parents(self) -> None:
        class MarkDonor:
            def cross(self, left, right, policy, rng):
                child = left.copy()
                first = next(iter(child.nodes.values()))
                first.attributes["donor_size"] = len(right.nodes)
                return child

        initial = [
            Graph(nodes=[Node(f"n{i}", "red") for i in range(count)])
            for count in (1, 2, 3, 10, 11, 12)
        ]
        result = EvolutionEngine(
            TargetSizeEvaluator(), self.policy, [], crossover=MarkDonor(),
            config=EvolutionConfig(
                population_size=6, island_count=2, elite_size=1,
                tournament_size=1, mutation_rate=0.0, crossover_rate=0.0,
                migration_interval=1, migration_size=1,
                island_exchange="crossover",
            ),
            seed=8,
        ).run(initial, 1)

        first_markers = [
            node.attributes["donor_size"]
            for item in result.islands[0]
            for node in item.graph.nodes.values()
            if "donor_size" in node.attributes
        ]
        second_markers = [
            node.attributes["donor_size"]
            for item in result.islands[1]
            for node in item.graph.nodes.values()
            if "donor_size" in node.attributes
        ]
        self.assertTrue(first_markers and min(first_markers) >= 10)
        self.assertTrue(second_markers and max(second_markers) <= 3)
        self.assertTrue(all(len(item.graph.nodes) <= 3 for item in result.islands[0]))
        self.assertTrue(all(len(item.graph.nodes) >= 10 for item in result.islands[1]))

    def test_invalid_island_configuration_is_rejected(self) -> None:
        invalid = (
            {"island_count": 0},
            {"population_size": 5, "island_count": 2},
            {"population_size": 6, "island_count": 2, "tournament_size": 4},
            {"population_size": 6, "island_count": 2, "migration_interval": 2},
            {"migration_interval": 2, "migration_size": 1},
        )
        for values in invalid:
            with self.assertRaises(ValueError, msg=str(values)):
                EvolutionConfig(**values)

    def test_each_island_can_use_a_different_node_policy(self) -> None:
        class ColorPolicy(ArbitraryPolicy):
            def __init__(self, color: str) -> None:
                self.color = color

            def create_node(self, rng: Random) -> Node:
                return Node("ignored", self.color)

            def validate(self, graph: Graph) -> None:
                super().validate(graph)
                if any(node.kind != self.color for node in graph.nodes.values()):
                    raise ValueError("Foreign node kind")

        red, blue = ColorPolicy("red"), ColorPolicy("blue")
        initial = [
            Graph(nodes=[Node("r0", "red")]),
            Graph(nodes=[Node("r1", "red")]),
            Graph(nodes=[Node("b0", "blue")]),
            Graph(nodes=[Node("b1", "blue")]),
        ]
        result = EvolutionEngine(
            TargetSizeEvaluator(),
            red,
            [AddNode()],
            config=EvolutionConfig(
                population_size=4,
                island_count=2,
                elite_size=0,
                tournament_size=1,
                crossover_rate=0.0,
                migration_interval=1,
                migration_size=1,
            ),
            seed=4,
            island_policies=(red, blue),
        ).run(initial, 1)

        self.assertTrue(all(
            node.kind == "red"
            for item in result.islands[0]
            for node in item.graph.nodes.values()
        ))
        self.assertTrue(all(
            node.kind == "blue"
            for item in result.islands[1]
            for node in item.graph.nodes.values()
        ))

    def test_each_island_can_use_a_different_evaluator(self) -> None:
        initial = [
            Graph(nodes=[Node(f"n{i}", "red") for i in range(count)])
            for count in (1, 2, 9, 10)
        ]
        config = EvolutionConfig(
            population_size=4,
            island_count=2,
            elite_size=1,
            tournament_size=1,
            mutation_rate=0.0,
            crossover_rate=0.0,
            migration_interval=1,
            migration_size=1,
        )
        evaluators = (PreferredSizeEvaluator(1), PreferredSizeEvaluator(10))
        with EvolutionEngine(
            TargetSizeEvaluator(),
            self.policy,
            [],
            config=config,
            seed=7,
            simulation_workers=2,
            island_evaluators=evaluators,
        ) as engine:
            result = engine.run(initial, 1)

        for index, island in enumerate(result.islands):
            self.assertEqual(
                [evaluators[index].evaluate(item.graph) for item in island],
                [item.raw_score for item in island],
            )

    def test_island_evaluator_count_must_match_islands(self) -> None:
        with self.assertRaises(ValueError):
            EvolutionEngine(
                TargetSizeEvaluator(),
                self.policy,
                [],
                config=EvolutionConfig(
                    population_size=4, island_count=2, tournament_size=1
                ),
                island_evaluators=(TargetSizeEvaluator(),),
            )

    def test_result_level_advance_matches_an_uninterrupted_island_run(self) -> None:
        initial = [
            Graph(nodes=[Node(f"n{i}", "red") for i in range(count)])
            for count in (1, 2, 3, 4, 5, 6)
        ]
        config = EvolutionConfig(
            population_size=6,
            island_count=2,
            elite_size=1,
            tournament_size=2,
            migration_interval=2,
            migration_size=1,
        )

        def engine() -> EvolutionEngine:
            return EvolutionEngine(
                TargetSizeEvaluator(), self.policy, [AddNode(), RemoveNode()],
                config=config, seed=19,
            )

        uninterrupted = engine().run(initial, 4)
        incremental_engine = engine()
        incremental = incremental_engine.run(initial, 0)
        for _ in range(4):
            incremental = incremental_engine.advance(incremental)

        self.assertEqual(
            [graph_dumps(item.graph) for island in uninterrupted.islands for item in island],
            [graph_dumps(item.graph) for island in incremental.islands for item in island],
        )

    def test_oscillating_pressure_rescores_parents_without_resimulation(self) -> None:
        initial = [
            Graph(nodes=[Node("small", "red")]),
            Graph(nodes=[Node(f"n{i}", "red") for i in range(3)]),
        ]
        engine = EvolutionEngine(
            TargetSizeEvaluator(), self.policy, [],
            config=EvolutionConfig(
                population_size=2, elite_size=2, tournament_size=1,
                mutation_rate=0.0, crossover_rate=0.0,
            ),
            fitness_shaper=OscillatingParsimony(
                expansion_generations=1,
                compression_generations=1,
                compression_node_weight=0.1,
            ),
        )
        expanded = engine.run(initial, 0)
        compressed = engine.advance(expanded)
        expanded_again = engine.advance(compressed)

        self.assertTrue(all(item.raw_score is not None for item in compressed.population))
        self.assertNotEqual(
            [item.score for item in expanded.population],
            [item.score for item in compressed.population],
        )
        self.assertEqual(
            [item.raw_score for item in expanded_again.population],
            [item.score for item in expanded_again.population],
        )


if __name__ == "__main__":
    unittest.main()
