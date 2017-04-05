package ameba.core.evolution.fitness;

/**
 * Created by marko on 3/31/17.
 */
public interface IFitness {
    double clcFitnessDec(double[] results, double[] resultsRef);

    double clcFitnessInt(int[] results, int[] resultsRef);

    double clcFitnessBin(boolean[] results, boolean[] resultsRef);
}
