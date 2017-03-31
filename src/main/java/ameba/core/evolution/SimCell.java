package ameba.core.evolution;

import ameba.core.blocks.Cell;

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

    public SimCell(double[][] dataInpDec, int[][] dataInpInt, boolean[][] dataInpBin, Cell cell) {
        this.dataInpDec = dataInpDec;
        this.dataInpInt = dataInpInt;
        this.dataInpBin = dataInpBin;
        this.dataOutDec = new double[dataInpDec.length][cell.getOutNodesDec().size()];
        this.dataOutInt = new int[dataInpInt.length][cell.getOutNodesInt().size()];
        this.dataOutBin = new boolean[dataInpBin.length][cell.getOutNodesBin().size()];

        this.cell = cell;
    }

    public void simulate() {
        for (int i = 0; i < dataInpDec.length; i++) {
            cell.runEvent(dataInpDec[i], dataInpInt[i], dataInpBin[i]);
            dataOutBin[i] = cell.getExportedValuesBin();
            dataOutInt[i] = cell.getExportedValuesInt();
            dataOutDec[i] = cell.getExportedValuesDec();
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
