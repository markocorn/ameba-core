package ameba.core.evolution;

import ameba.core.blocks.Cell;
import ameba.core.evolution.fitness.IFitness;

/**
 * Created by marko on 3/31/17.
 */
public class SimCell {
    DataEvo data;
    Cell cell;
    IFitness fitness;
    int prefCellSize;
    double weightDown;
    double weightUp;

    public SimCell(IFitness fitness, DataEvo data, Cell cell, int prefCellSize, double weightDown, double weightUp) {
        this.data = data;
        this.fitness = fitness;
        this.prefCellSize = prefCellSize;
        this.weightDown = weightDown;
        this.weightUp = weightUp;
        this.cell = cell;
    }

    public void simulate() {
        cell.clearCell();
        cell.setFitnessValue(0.0);
        boolean[] c = new boolean[]{cell.getOutNodesDec().isEmpty(), cell.getOutNodesInt().isEmpty(), cell.getOutNodesBin().isEmpty()};
        int max = Math.max(data.getInpDec().length, Math.max(data.getInpInt().length, data.getInpBin().length));

        for (int i = 0; i < max; i++) {
            if (!c[0]) {
                cell.importSignals(data.getInpDec()[i]);
            }
            if (!c[1]) {
                cell.importSignals(data.getInpInt()[i]);
            }
            if (!c[2]) {
                cell.importSignals(data.getInpBin()[i]);
            }
            try {
                cell.runEvent();
            } catch (Exception ex) {
                ex.printStackTrace();
                cell.checkCellPrint();
                cell.runEvent();
            }
            if (!c[0]) {
                cell.addFitnessValue(fitness.clcFitnessDec(cell.getExportedValuesDec(), data.getOutDec()[i]));
            }
            if (!c[1]) {
                cell.addFitnessValue(fitness.clcFitnessInt(cell.getExportedValuesInt(), data.getOutInt()[i]));
            }
            if (!c[2]) {
                cell.addFitnessValue(fitness.clcFitnessBin(cell.getExportedValuesBin(), data.getOutBin()[i]));
            }
        }
        double fit;
        int diff = cell.getInnerNodes().size() - prefCellSize;
        if (diff > 0) {
            fit = diff * weightUp;
        } else {
            fit = -diff * weightDown;
        }
        cell.addFitnessValue(fit);
    }

    public Cell getCell() {
        return cell;
    }
}
