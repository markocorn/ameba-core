package ameba.evolution.evolution;

import ameba.core.blocks.Cell;
import ameba.core.factories.*;
import ameba.evolution.blocks.StateModel;
import ameba.evolution.fitness.Fitness;
import ameba.evolution.fitness.FitnessAbsolute;
import ameba.evolution.fitness.FitnessSquare;
import ameba.evolution.incubator.DataEvo;
import ameba.evolution.incubator.Incubator;
import ameba.evolution.incubator.IncubatorSettings;
import ameba.evolution.selection.BestOf;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by marko on 4/3/17.
 */
public class Evolution extends Thread {
    EvolutionSettings evolutionSettings;
    FactoryCell factoryCell;
    FactoryNode factoryNode;
    FactoryEdge factoryEdge;
    FactoryReproduction factoryReproduction;
    Incubator incubator;
    DataEvo dataEvo;
    int generation;

    private Evolution() {
    }

    public Evolution(DataEvo data, String settingsPathName, long seed) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File(settingsPathName));
        factoryNode = new FactoryNode(seed);
        factoryNode.loadSettings(jsonSettings.get("nodeFactorySettings").toString());
        factoryEdge = new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class), seed);
        factoryCell = new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), factoryNode, factoryEdge, seed);
        factoryReproduction = new FactoryReproduction(factoryEdge, factoryNode, factoryCell, seed);
        factoryReproduction.loadSettings(jsonSettings.get("reproductionFactorySettings").toString(), seed);


        dataEvo = data;
        IncubatorSettings settings = mapper.readValue(jsonSettings.get("incubatorSettings").toString(), IncubatorSettings.class);
        Fitness fitness;
        switch (settings.getFitnessType()) {
            //Absolute type
            case 0: {
                fitness = new FitnessAbsolute(settings.getFitWeight());
            }
            break;
            //Square type
            case 1: {
                fitness = new FitnessSquare(settings.getFitWeight());
            }
            break;
            default: {
                fitness = new FitnessAbsolute(settings.getFitWeight());
            }

        }
        incubator = new Incubator(
                factoryCell,
                factoryReproduction,
                settings,
                new BestOf(),
                fitness,
                dataEvo, seed);
        generation = 0;
        evolutionSettings = EvolutionSettings.create(jsonSettings.get("evolutionSettings").toString());

        if (evolutionSettings.getLoadModel()) {
            incubator.setModel(StateModel.create(jsonSettings.get("stateModelSettings").toString()));
        }
    }

    public static Evolution build(DataEvo data, long seed) throws Exception {
        Evolution evo = new Evolution();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(Evolution.class.getClassLoader().getResourceAsStream("evolution/evolutionSettings.json"));

        evo.evolutionSettings = EvolutionSettings.create(jsonSettings.get("evolutionSettings").toString());
        evo.factoryCell = FactoryCell.build(seed);
        evo.factoryNode = evo.factoryCell.getNodeFactory();
        evo.factoryEdge = evo.factoryCell.getEdgeFactory();
        evo.factoryReproduction = FactoryReproduction.build(seed);
        jsonSettings = mapper.readTree(Evolution.class.getClassLoader().getResourceAsStream("evolution/incubatorSettings.json"));
        evo.dataEvo = data;
        IncubatorSettings settings = IncubatorSettings.create(jsonSettings.get("incubatorSettings").toString());
        evo.incubator = new Incubator(
                evo.factoryCell,
                evo.factoryReproduction,
                settings,
                new BestOf(),
                new FitnessSquare(settings.getFitWeight()),
                evo.dataEvo, seed);

        evo.generation = 0;
        return evo;
    }

    public void initRun() throws Exception {
        //Load or generate population
        if (!evolutionSettings.getLoadInitialPopulation()) {
            incubator.populateInitial();
        } else {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(new File(evolutionSettings.getInitialPopulationPathFile()));

            int diff = node.size() - incubator.getIncubatorSettings().getPopulationSize();
            int same = Math.min(node.size(), incubator.getIncubatorSettings().getPopulationSize());
            ArrayList<Cell> pop = new ArrayList<>();
            for (int i = 0; i < same; i++) {
                pop.add(factoryCell.getCellJson(mapper.writeValueAsString(node.get(0))));
            }
            if (diff < 0) {
                for (int i = 0; i < -diff; i++) {
                    pop.add(factoryCell.genCellRnd());
                }
            }
            incubator.setPopulation(pop);
            System.out.println("Initial population loaded from file.");
        }
    }

    public void run() {
        generation = 0;
        double fit_old = 1e6;
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                exitRun();
                System.out.println("Stop evo by user.");
                break;
            }
            generation++;

            incubator.simPopulation();

            incubator.sort();

            System.out.println(generation +
                    " nodes: " + incubator.getPopulation().get(0).getInnerNodes().size() +
                    " edges: " + incubator.getPopulation().get(0).getEdges().size() +
                    " fitness: " + incubator.getPopulation().get(0).getFitnessValue() +
                    " rep: " + incubator.getPopulation().get(0).lastRep);

            if (incubator.getPopulation().get(0).getFitnessValue() > fit_old) {
                System.out.println("NAPAKA!!!!");
                try {
                    System.out.println("Hash1: " + incubator.getPopulation().get(0).toJsonString().hashCode());
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

            }
            fit_old = incubator.getPopulation().get(0).getFitnessValue();

            if (evolutionSettings.getSavePopulationPeriod() > 0
                    && generation % evolutionSettings.getSavePopulationPeriod() == 0
                    && evolutionSettings.getSavePopulation()) {
                saveGeneration();

            }
            if (evolutionSettings.getSaveModelOutputs()
                    && evolutionSettings.getSaveModelOutputsPeriod() > 0
                    && generation % evolutionSettings.getSaveModelOutputsPeriod() == 0) {
                incubator.simBestPrint(evolutionSettings.getSaveModelOutputsPathFile());
                System.out.println("Output results saved to: " + evolutionSettings.getSaveModelOutputsPathFile());
            }

            try {
                incubator.reproduce();

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (evolutionSettings.getEvolutionExitType() == 0) {
                if (generation >= evolutionSettings.getMaxGenerations()) {
                    exitRun();
                    break;
                }
            } else {
                exitRun();
                break;
            }
        }
    }

    public void saveGeneration() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode pop = mapper.createArrayNode();

        try {
            for (int i = 0; i < evolutionSettings.getSavePopulationSize(); i++) {
                pop.add(mapper.readTree(incubator.getPopulation().get(i).toJsonString()));
            }
            mapper.writeValue(new File(evolutionSettings.getSavePopulationPathFile()), pop);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Generation saved.");
    }

    public void saveCellBackup() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode pop = mapper.createArrayNode();
        try {
            pop.add(mapper.readTree(incubator.getPopulation().get(0).toJsonString()));
            mapper.writeValue(new File(System.getProperty("user.dir") + "/cellBackup"), pop);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exitRun() {
        System.out.println("ameba.evo.Main ended");
        if (evolutionSettings.getSavePopulationPathFile().equals("")) {
            saveCellBackup();
        } else {
            saveGeneration();
        }
    }

    public EvolutionSettings getEvolutionSettings() {
        return evolutionSettings;
    }

    public void setEvolutionSettings(EvolutionSettings evolutionSettings) {
        this.evolutionSettings = evolutionSettings;
    }

    public FactoryCell getFactoryCell() {
        return factoryCell;
    }

    public void setFactoryCell(FactoryCell factoryCell) {
        this.factoryCell = factoryCell;
    }

    public FactoryNode getFactoryNode() {
        return factoryNode;
    }

    public void setFactoryNode(FactoryNode factoryNode) {
        this.factoryNode = factoryNode;
    }

    public FactoryEdge getFactoryEdge() {
        return factoryEdge;
    }

    public void setFactoryEdge(FactoryEdge factoryEdge) {
        this.factoryEdge = factoryEdge;
    }

    public FactoryReproduction getFactoryReproduction() {
        return factoryReproduction;
    }

    public void setFactoryReproduction(FactoryReproduction factoryReproduction) {
        this.factoryReproduction = factoryReproduction;
    }

    public Incubator getIncubator() {
        return incubator;
    }

    public void setIncubator(Incubator incubator) {
        this.incubator = incubator;
    }

    public DataEvo getDataEvo() {
        return dataEvo;
    }

    public void setDataEvo(DataEvo dataEvo) {
        this.dataEvo = dataEvo;
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }
}
