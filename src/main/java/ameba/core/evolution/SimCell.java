package ameba.core.evolution;

import ameba.core.blocks.Cell;
import ameba.core.evolution.fitness.IFitness;

/**
 * Created by marko on 3/31/17.
 */
public class SimCell {
    double[][] dataInpDec;
    int[][] dataInpInt;
    boolean[][] dataInpBin;
    double[][] dataOutDec;
    int[][] dataOutInt;
    boolean[][] dataOutBin;
    Cell cell;
    IFitness fitness;
    int prefCellSize;
    double weightDown;
    double weightUp;

    public SimCell(IFitness fitness, double[][] dataInpDec, int[][] dataInpInt, boolean[][] dataInpBin, Cell cell, int prefCellSize, double weightDown, double weightUp) {
        this.dataInpDec = dataInpDec;
        this.dataInpInt = dataInpInt;
        this.dataInpBin = dataInpBin;
        this.dataOutDec = new double[dataInpDec.length][cell.getOutNodesDec().size()];
        this.dataOutInt = new int[dataInpInt.length][cell.getOutNodesInt().size()];
        this.dataOutBin = new boolean[dataInpBin.length][cell.getOutNodesBin().size()];
        this.fitness = fitness;
        this.prefCellSize = prefCellSize;
        this.weightDown = weightDown;
        this.weightUp = weightUp;

        this.cell = cell;
    }

    public void simulate() {
        for (int i = 0; i < dataInpDec.length; i++) {
            cell.importSignals(dataInpDec[i]);
            cell.runEvent();
            if (dataOutBin.length > 0) {
                dataOutBin[i] = cell.getExportedValuesBin();
            }
            if (dataOutInt.length < 0) {
                dataOutInt[i] = cell.getExportedValuesInt();
            }
            if (dataOutDec.length > 0) {
                dataOutDec[i] = cell.getExportedValuesDec();
            }
        }
        cell.clearCell();
    }

    public void fitness() {
        double fit = 0.0;
        int diff = cell.getInnerNodes().size() - prefCellSize;
        if (diff > 0) {
            fit = diff * weightUp;
        } else {
            fit = -diff * weightDown;
        }
        cell.setFitnessValue(fit);
        if (dataOutDec.length > 0) {
            cell.setFitnessValue(cell.getFitnessValue() + fitness.clcFitnessDec(dataOutDec));
        }
        if (dataOutInt.length > 0) {
            cell.setFitnessValue(cell.getFitnessValue() + fitness.clcFitnessInt(dataOutInt));
        }
        if (dataOutBin.length > 0) {
            cell.setFitnessValue(cell.getFitnessValue() + fitness.clcFitnessBin(dataOutBin));
        }
    }

    public double[][] getDataOutDec() {
        return dataOutDec;
    }

    public int[][] getDataOutInt() {
        return dataOutInt;
    }

    public boolean[][] getDataOutBin() {
        return dataOutBin;
    }

    public Cell getCell() {
        return cell;
    }
}
