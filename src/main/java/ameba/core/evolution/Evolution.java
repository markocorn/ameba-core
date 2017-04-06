package ameba.core.evolution;

import ameba.core.blocks.Cell;
import ameba.core.evolution.fitness.FitnessAbsolute;
import ameba.core.evolution.selections.BestOf;
import ameba.core.factories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
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

    public Evolution(DataEvo data, String settingsPathName) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File(settingsPathName));
        factoryNode = new FactoryNode();
        factoryNode.loadSettings(jsonSettings.get("nodeFactorySettings").toString());
        factoryEdge = new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class));
        factoryCell = new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), factoryNode, factoryEdge);
        factoryReproduction = new FactoryReproduction(factoryEdge, factoryNode, factoryCell);
        factoryReproduction.loadSettings(jsonSettings.get("reproductionSettings").toString());
        dataEvo = data;
        IncubatorSettings settings = mapper.readValue(jsonSettings.get("incubatorSettings").toString(), IncubatorSettings.class);
        incubator = new Incubator(
                factoryCell,
                factoryReproduction,
                settings,
                new BestOf(),
                new FitnessAbsolute(
                        settings.getFitWeightDec(),
                        settings.getFitWeightInt(),
                        settings.getFitWeightBin()),
                dataEvo);
        generation = 0;
        evolutionSettings = EvolutionSettings.create(jsonSettings.get("evolutionSettings").toString());
    }

    public static Evolution build(DataEvo data) throws Exception {
        Evolution evo = new Evolution();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File(Evolution.class.getClassLoader().getResource("evolutionSettings.json").getFile()));

        evo.evolutionSettings = EvolutionSettings.create(jsonSettings.get("evolutionSettings").toString());
        evo.factoryCell = FactoryCell.build();
        evo.factoryNode = evo.factoryCell.getNodeFactory();
        evo.factoryEdge = evo.factoryCell.getEdgeFactory();
        evo.factoryReproduction = FactoryReproduction.build();
        jsonSettings = mapper.readTree(new File(IncubatorSettings.class.getClassLoader().getResource("incubatorSettings.json").getFile()));
        evo.dataEvo = data;
        IncubatorSettings settings = IncubatorSettings.create(jsonSettings.get("incubatorSettings").toString());
        evo.incubator = new Incubator(
                evo.factoryCell,
                evo.factoryReproduction,
                settings,
                new BestOf(),
                new FitnessAbsolute(
                        settings.getFitWeightDec(),
                        settings.getFitWeightInt(),
                        settings.getFitWeightBin()),
                evo.dataEvo);

        evo.generation = 0;
        return evo;
    }

    public void initRun() throws Exception {
        //Load or generate population
        if (evolutionSettings.getInitialPopulationPathFile().equals("")) {
            incubator.populateInitial();
        } else {
            FileInputStream inputFileStream = new FileInputStream(evolutionSettings.getInitialPopulationPathFile());
            ObjectInputStream objectInputStream = new ObjectInputStream(inputFileStream);
            ArrayList<Cell> popLoaded = (ArrayList<Cell>) objectInputStream.readObject();
            int diff = popLoaded.size() - incubator.incubatorSettings.getPopulationSize();
            int same = Math.min(popLoaded.size(), incubator.incubatorSettings.getPopulationSize());
            ArrayList<Cell> pop = new ArrayList<>();
            for (int i = 0; i < same; i++) {
                pop.add(popLoaded.get(i));
            }
            if (diff < 0) {
                for (int i = 0; i < -diff; i++) {
                    pop.add(factoryCell.genCellRnd());
                }
            }
            incubator.setPopulation(pop);
            objectInputStream.close();
            inputFileStream.close();
            System.out.println("Initial population loaded from file.");
        }
    }

    public void run() {
        generation = 0;
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                exitRun();
                System.out.println("Stop evolution by user.");
                break;
            }
            generation++;

            incubator.simPopulation();

            try {
                incubator.reproduce();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (evolutionSettings.getSavePopulationPeriod() > 0
                    && generation % evolutionSettings.getSavePopulationPeriod() == 0
                    && !evolutionSettings.getSavePopulationPathFile().equals("")) {
                saveGeneration();
            }
            System.out.println(generation + ": size: " + incubator.getPopulation().get(0).getInnerNodes().size() + " ; fitness: " + incubator.getPopulation().get(0).getFitnessValue());
            if (evolutionSettings.getEvolutionExitType() == 0) {
                if (generation > evolutionSettings.getMaxGenerations()) {
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
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(evolutionSettings.getSavePopulationPathFile());
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            ArrayList<Cell> p = new ArrayList<>();
            for (int i = 0; i < evolutionSettings.getSavePopulationSize(); i++) {
                p.add(incubator.population.get(i));
            }
            oos.writeObject(p);
            System.out.println("Saved population at generation: " + generation);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveCellBackup() {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(System.getProperty("user.dir") + "/cellBackup");
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            ArrayList<Cell> p = new ArrayList<>();
            p.add(incubator.population.get(0));
            oos.writeObject(p);
            System.out.println("Saved cell backup at generation: " + generation);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exitRun() {
        System.out.println("Main ended");
        if (evolutionSettings.getSavePopulationPathFile().equals("")) {
            saveCellBackup();
        } else {
            saveGeneration();
        }
    }
}
