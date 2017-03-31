package ameba.core.evolution.fitness;

/**
 * Created by marko on 3/31/17.
 */
public class Fitness implements IFitness {
    double[][] dataRefDec;
    int[][] dataRefInt;
    boolean[][] dataRefBin;
    double weightDec;
    double weightInt;
    double weightBin;

    public Fitness(double[][] dataRefDec, int[][] dataRefInt, boolean[][] dataRefBin, double weightDec, double weightInt, double weightBin) {
        this.dataRefDec = dataRefDec;
        this.dataRefInt = dataRefInt;
        this.dataRefBin = dataRefBin;
        this.weightDec = weightDec;
        this.weightInt = weightInt;
        this.weightBin = weightBin;
    }

    @Override
    public double clcFitnessDec(double[][] results) {
        return 0.0;
    }

    @Override
    public double clcFitnessInt(int[][] results) {
        return 0.0;
    }

    @Override
    public double clcFitnessBin(boolean[][] results) {
        return 0.0;
    }
}
