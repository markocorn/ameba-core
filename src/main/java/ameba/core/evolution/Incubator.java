package ameba.core.evolution;

import ameba.core.blocks.Cell;
import ameba.core.evolution.fitness.Fitness;
import ameba.core.evolution.selections.ISelect;
import ameba.core.factories.FactoryCell;

import java.util.ArrayList;

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
    double[][] dataDec;
    int[][] dataInt;
    boolean[][] dataBin;


    public Incubator(FactoryCell factoryCell, IncubatorSettings incubatorSettings, ISelect selection, Fitness fitness) {
        this.factoryCell = factoryCell;
        this.incubatorSettings = incubatorSettings;
        this.selection = selection;
        this.fitness = fitness;
        population = new ArrayList<>();

    }

    public void populateInitial() throws Exception {
        //Populate population
        if (incubatorSettings.initialPopulation.equals("")) {
            for (int i = 0; i < incubatorSettings.populationSize; i++) {
                population.add(factoryCell.genCellRnd());
            }
        } else {
        }
    }

    public void simPopulation() {
        ArrayList<SimCell> sims = new ArrayList<>();
        for (Cell cell : population) {
            sims.add(new SimCell(dataDec, dataInt, dataBin, cell));
        }
        sims.stream().forEach(SimCell::simulate);
    }

    public void clcFitness() {
        for (SimCell sim : sims) {
            sim.getCell().setFitnessValue(sim.getCell().getFitnessValue() + fitness.clcFitnessDec(sim.dataOutDec));
            sim.getCell().setFitnessValue(sim.getCell().getFitnessValue() + fitness.clcFitnessInt(sim.dataOutInt));
            sim.getCell().setFitnessValue(sim.getCell().getFitnessValue() + fitness.clcFitnessBin(sim.dataOutBin));
        }
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
        if (cell.getInpNodesDec().size() != dataDec[0].length)
            throw new Exception("Number of decimal input nodes not equal to the number of input decimal signals");
        if (cell.getInpNodesInt().size() != dataInt[0].length)
            throw new Exception("Number of integer input nodes not equal to the number of input integer signals");
        if (cell.getInpNodesBin().size() != dataBin[0].length)
            throw new Exception("Number of boolean input nodes not equal to the number of input boolean signals");
        if (dataDec.length != dataInt.length || dataDec.length != dataBin.length || dataInt.length != dataBin.length)
            throw new Exception("input data not of the same length");
    }
}
