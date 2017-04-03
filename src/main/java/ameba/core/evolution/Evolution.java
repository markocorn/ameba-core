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
public class Evolution {
    EvolutionSettings evolutionSettings;
    FactoryCell factoryCell;
    FactoryNode factoryNode;
    FactoryEdge factoryEdge;
    FactoryReproduction factoryReproduction;
    Incubator incubator;
    ObjectMapper mapper;
    double[][] inpDec;
    double[][] outDec;
    int[][] inpInt;
    int[][] outInt;
    boolean[][] inpBin;
    boolean[][] outBin;
    int generation;

    public Evolution(String settingsFilePath) throws IOException {
        mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File(settingsFilePath));
        this.evolutionSettings = EvolutionSettings.create(jsonSettings.get("evolutionSettings").toString());
        this.evolutionSettings.setSettingsPathFile(settingsFilePath);
        inpDec = new double[0][0];
        outDec = new double[0][0];
        inpInt = new int[0][0];
        outInt = new int[0][0];
        inpBin = new boolean[0][0];
        outBin = new boolean[0][0];
        generation = 0;
    }

    public void init() throws Exception {
        JsonNode jsonSettings = mapper.readTree(new File(evolutionSettings.getSettingsPathFile()));
        factoryNode = new FactoryNode();
        factoryNode.loadSettings(jsonSettings.get("nodeFactorySettings").toString());
        factoryEdge = new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class));
        factoryCell = new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), factoryNode, factoryEdge);
        factoryReproduction = new FactoryReproduction(factoryEdge, factoryNode, factoryCell);
        factoryReproduction.loadSettings(jsonSettings.get("reproductionSettings").toString());
        //Prepare data
        JsonNode dataJson = mapper.readTree(new File(evolutionSettings.getDataPathFile()));
        if (dataJson.has("inpDec")) {
            inpDec = new double[dataJson.get("inpDec").size()][dataJson.get("inpDec").get(0).size()];
        }
        if (dataJson.has("outDec")) {
            outDec = new double[dataJson.get("outDec").size()][dataJson.get("outDec").get(0).size()];
        }
        if (dataJson.has("inpInt")) {
            inpInt = new int[dataJson.get("inpInt").size()][dataJson.get("inpInt").get(0).size()];
        }
        if (dataJson.has("outInt")) {
            outInt = new int[dataJson.get("outInt").size()][dataJson.get("outInt").get(0).size()];
        }
        if (dataJson.has("inpBin")) {
            inpBin = new boolean[dataJson.get("inpBin").size()][dataJson.get("inpBin").get(0).size()];
        }
        if (dataJson.has("outBin")) {
            outBin = new boolean[dataJson.get("outBin").size()][dataJson.get("outBin").get(0).size()];
        }
        for (int i = 0; i < inpDec.length; i++) {
            for (int j = 0; j < inpDec[i].length; j++) {
                inpDec[i][j] = dataJson.get("inpDec").get(i).get(j).asDouble();
            }
            for (int j = 0; j < outDec[i].length; j++) {
                outDec[i][j] = dataJson.get("outDec").get(i).get(j).asDouble();
            }
        }
        for (int i = 0; i < inpInt.length; i++) {
            for (int j = 0; j < inpInt[i].length; j++) {
                inpInt[i][j] = dataJson.get("inpInt").get(i).get(j).asInt();
            }
            for (int j = 0; j < outInt[i].length; j++) {
                outInt[i][j] = dataJson.get("outInt").get(i).get(j).asInt();
            }
        }
        for (int i = 0; i < inpBin.length; i++) {
            for (int j = 0; j < inpBin[i].length; j++) {
                inpBin[i][j] = dataJson.get("inpBin").get(i).get(j).asBoolean();
            }
            for (int j = 0; j < outBin[i].length; j++) {
                outBin[i][j] = dataJson.get("outBin").get(i).get(j).asBoolean();
            }
        }
        if (evolutionSettings.getEnableGPU()) {
            incubator = new IncubatorGPU(factoryCell, factoryReproduction, mapper.readValue(jsonSettings.get("incubatorSettings").toString(), IncubatorSettings.class), new BestOf(), new FitnessAbsolute(outDec, null, null, 10.0, 10.0, 10.0));
        } else
            incubator = new Incubator(factoryCell, factoryReproduction, mapper.readValue(jsonSettings.get("incubatorSettings").toString(), IncubatorSettings.class), new BestOf(), new FitnessAbsolute(outDec, null, null, 10.0, 10.0, 10.0));
        incubator.importData(inpDec);
        incubator.importData(inpInt);
        incubator.importData(inpBin);
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

    public void run() throws Exception {
        generation = 0;
        while (true) {
            generation++;
            incubator.simPopulation();
            incubator.reproduce();
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

    public void saveGeneration() throws IOException {
        FileOutputStream fout = new FileOutputStream(evolutionSettings.getSavePopulationPathFile());
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(incubator.population);
        System.out.println("Saved population at generation: " + generation);
    }

    private void exitRun() throws IOException {
        System.out.println("Evolution ended");
        saveGeneration();

    }
}
