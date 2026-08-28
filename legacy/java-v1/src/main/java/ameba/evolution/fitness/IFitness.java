package ameba.evolution.fitness;

/**
 * Created by marko on 3/31/17.
 */
public interface IFitness {
    double clcFitness(double[] results, double[] resultsRef);
}
