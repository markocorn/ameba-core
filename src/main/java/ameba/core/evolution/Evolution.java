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
    ObjectMapper mapper;
    DataEvo dataEvo;
    int generation;

    public Evolution(String dataPathName) throws Exception {
        mapper = new ObjectMapper();
        generation = 0;
        ClassLoader classLoader = getClass().getClassLoader();
        JsonNode jsonSettings = mapper.readTree(new File(classLoader.getResource(dataPathName).getFile()));
        dataEvo = DataEvo.create(jsonSettings.toString());
        loadResourcesDefault();
    }

    public void loadResourcesDefault() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        JsonNode jsonSettings = mapper.readTree(new File(classLoader.getResource("nodeFactorySettings.json").getFile()));
        factoryNode = new FactoryNode();
        factoryNode.loadSettings(jsonSettings.get("nodeFactorySettings").toString());
        jsonSettings = mapper.readTree(new File(classLoader.getResource("edgeFactorySettings.json").getFile()));
        factoryEdge = new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class));
        jsonSettings = mapper.readTree(new File(classLoader.getResource("cellFactorySettings.json").getFile()));
        factoryCell = new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), factoryNode, factoryEdge);
        factoryReproduction = new FactoryReproduction(factoryEdge, factoryNode, factoryCell);
        jsonSettings = mapper.readTree(new File(classLoader.getResource("factoryFactorySettings.json").getFile()));
        factoryReproduction.loadSettings(jsonSettings.get("reproductionSettings").toString());
        jsonSettings = mapper.readTree(new File(classLoader.getResource("evolutionSettings.json").getFile()));
        this.evolutionSettings = EvolutionSettings.create(jsonSettings.get("evolutionSettings.json").toString());
        incubator = new Incubator(factoryCell, factoryReproduction, mapper.readValue(jsonSettings.get("incubatorSettings").toString(), IncubatorSettings.class), new BestOf(), new FitnessAbsolute(10.0, 10.0, 10.0), dataEvo);
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
            if (evolutionSettings.getSavePopulationPeriod() > 0 && generation % evolutionSettings.getSavePopulationPeriod() == 0) {
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

    private void exitRun() {
        System.out.println("Main ended");
        saveGeneration();
    }
}
