import contextlib
import io
import json
import unittest

from ameba.cli import main
from ameba.config import AppConfig


class ConfigAndCliTests(unittest.TestCase):
    def test_configuration_maps_to_domain_objects(self) -> None:
        config = AppConfig.from_mapping(
            {
                "schema_version": 1,
                "generations": 7,
                "seed": 99,
                "simulation_workers": 3,
                "evolution": {
                    "population_size": 6,
                    "elite_size": 2,
                    "tournament_size": 3,
                    "mutation_rate": 0.8,
                    "crossover_rate": 0.4,
                    "island_count": 2,
                    "migration_interval": 5,
                    "migration_size": 1,
                    "island_exchange": "crossover",
                },
                "signal": {
                    "weight_min": -1.0,
                    "weight_max": 1.0,
                    "evolvable_kinds": ["add", "delay"],
                },
                "islands": [
                    {"name": "static", "evolvable_kinds": ["add", "constant"]},
                    {"name": "dynamic", "evolvable_kinds": ["delay", "add"]},
                ],
            }
        )
        self.assertEqual(7, config.generations)
        self.assertEqual(6, config.evolution.population_size)
        self.assertEqual(3, config.simulation_workers)
        self.assertEqual(2, config.evolution.island_count)
        self.assertEqual(5, config.evolution.migration_interval)
        self.assertEqual("crossover", config.evolution.island_exchange)
        self.assertEqual(("add", "delay"), config.signal.evolvable_kinds)
        self.assertEqual(("static", "dynamic"), tuple(i.name for i in config.islands))
        self.assertEqual(("delay", "add"), config.islands[1].evolvable_kinds)

    def test_cli_evaluates_versioned_example_files(self) -> None:
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            code = main(
                [
                    "evaluate",
                    "examples/linear.graph.json",
                    "examples/linear.dataset.json",
                ]
            )
        self.assertEqual(0, code)
        self.assertEqual(0.0, json.loads(output.getvalue())["score"])


if __name__ == "__main__":
    unittest.main()
