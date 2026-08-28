package ameba.evolution.incubator;

import ameba.core.blocks.Cell;
import ameba.evolution.fitness.IFitness;

/**
 * Created by marko on 3/31/17.
 */
public class SimulateCell {
    DataEvo data;
    Cell cell;
    IFitness fitness;
    int prefCellSize;
    double weightDown;
    double weightUp;
    double weightEdges;


    public SimulateCell(IFitness fitness, DataEvo data, Cell cell, int prefCellSize, double weightDown, double weightUp, double weightEdges) {
        this.data = data;
        this.fitness = fitness;
        this.prefCellSize = prefCellSize;
        this.weightDown = weightDown;
        this.weightUp = weightUp;
        this.cell = cell;
        this.weightEdges = weightEdges;
    }

    public void simulate() {
        cell.clearCell();
        cell.setFitnessValue(0.0);
        boolean[] cInp = new boolean[]{cell.getInpNodes().isEmpty()};
        boolean[] cOut = new boolean[]{cell.getOutNodes().isEmpty()};
//        double out=0;
        for (int i = 0; i < data.getInputs().length; i++) {
            if (!cInp[0]) {
                cell.importSignals(data.getInputs()[i]);
            }
            try {
                cell.runEvent();
            } catch (Exception ex) {
                ex.printStackTrace();
                cell.runEvent();
            }
            if (!cOut[0]) {
                cell.addFitnessValue(fitness.clcFitness(cell.getExportedValues(), data.getOutputs()[i]));
            }

//            for(double value : cell.getExportedValues()){
//               out+=value;
//            }
        }
//        System.out.print("Sum: "+out);
        double fit;
        int diff = cell.getInnerNodes().size() - prefCellSize;
        if (diff > 0) {
            fit = diff * weightUp;
        } else {
            fit = -diff * weightDown;
        }
        cell.addFitnessValue(fit);

        cell.addFitnessValue(cell.getEdges().size() * weightEdges);

//        System.out.println(" Fit: "+cell.getFitnessValue());
    }

    public Cell getCell() {
        return cell;
    }
}
