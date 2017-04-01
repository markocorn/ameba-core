package ameba.core.evolution;

import ameba.core.blocks.Cell;
import ameba.core.evolution.fitness.Fitness;
import ameba.core.evolution.selections.ISelect;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryReproduction;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by marko on 3/31/17.
 */
public class Incubator {
    IncubatorSettings incubatorSettings;
    ISelect selection;
    Fitness fitness;
    ArrayList<Cell> population;
    ArrayList<SimCell> sims;
    FactoryCell factoryCell;
    FactoryReproduction factoryReproduction;
    double[][] dataDec;
    int[][] dataInt;
    boolean[][] dataBin;
    Random random;


    public Incubator(FactoryCell factoryCell, FactoryReproduction factoryReproduction, IncubatorSettings incubatorSettings, ISelect selection, Fitness fitness) {
        this.factoryCell = factoryCell;
        this.factoryReproduction = factoryReproduction;
        this.incubatorSettings = incubatorSettings;
        this.selection = selection;
        this.fitness = fitness;
        population = new ArrayList<>();
        dataDec = new double[0][0];
        dataInt = new int[0][0];
        dataBin = new boolean[0][0];
        this.random = new Random();

    }

    public void populateInitial() throws Exception {
        //Populate population
        if (incubatorSettings.getInitialPopulation().equals("")) {
            for (int i = 0; i < incubatorSettings.getPopulationSize(); i++) {
                population.add(factoryCell.genCellRnd());
            }
        } else {
        }
    }

    public void simPopulation() {
        ArrayList<SimCell> sims = new ArrayList<>();
        for (Cell cell : population) {
            sims.add(new SimCell(fitness, dataDec, dataInt, dataBin, cell, incubatorSettings.getPrefCellSize(), incubatorSettings.getWeightDown(), incubatorSettings.getWeightUp()));
        }
        sims.parallelStream().forEach(SimCell::simulate);
        sims.parallelStream().forEach(SimCell::fitness);
    }

    public void reproduce() throws Exception {
        ArrayList<Cell> chosen = selection.select(population, incubatorSettings.getChosen());
        ArrayList<Cell> newGeneration = new ArrayList<>();
        for (int i = 0; i < incubatorSettings.getEliteSize(); i++) {
            newGeneration.add(chosen.get(i).clone());
        }
        for (int i = incubatorSettings.getEliteSize(); i < incubatorSettings.getPopulationSize(); i++) {
            try {
                newGeneration.add(factoryReproduction.repCell(chosen.get(random.nextInt(chosen.size())), chosen.get(random.nextInt(chosen.size()))));
            } catch (Exception ex) {
                i--;
                ex.printStackTrace();
            }
        }
        population = newGeneration;
    }

    public void importData(double[][] data) {
        dataDec = data;
    }

    public void importData(int[][] data) {
        dataInt = data;
    }

    public void importData(boolean[][] data) {
        dataBin = data;
    }

    public void checkDataMatch() throws Exception {
        Cell cell = population.get(0);
        if (dataDec.length > 0) {
            if (cell.getInpNodesDec().size() != dataDec[0].length)
                throw new Exception("Number of decimal input nodes not equal to the number of input decimal signals");
        }
        if (dataInt.length > 0) {
            if (cell.getInpNodesInt().size() != dataInt[0].length)
                throw new Exception("Number of integer input nodes not equal to the number of input integer signals");
        }
        if (dataBin.length > 0) {
            if (cell.getInpNodesBin().size() != dataBin[0].length)
                throw new Exception("Number of boolean input nodes not equal to the number of input boolean signals");
        }
    }

    public ArrayList<Cell> getPopulation() {
        return population;
    }
}
