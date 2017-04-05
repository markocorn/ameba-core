package ameba.core.evolution.fitness;

/**
 * Created by marko on 3/31/17.
 */
public class Fitness implements IFitness {
    double weightDec;
    double weightInt;
    double weightBin;

    public Fitness(double weightDec, double weightInt, double weightBin) {
        this.weightDec = weightDec;
        this.weightInt = weightInt;
        this.weightBin = weightBin;
    }

    @Override
    public double clcFitnessDec(double[] results, double[] resultsRef) {
        return 0;
    }

    @Override
    public double clcFitnessInt(int[] results, int[] resultsRef) {
        return 0;
    }

    @Override
    public double clcFitnessBin(boolean[] results, boolean[] resultsRef) {
        return 0;
    }
}
