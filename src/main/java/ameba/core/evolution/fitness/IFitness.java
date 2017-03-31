package ameba.core.evolution.fitness;

/**
 * Created by marko on 3/31/17.
 */
public interface IFitness {
    double clcFitnessDec(double[][] results);

    double clcFitnessInt(int[][] results);

    double clcFitnessBin(boolean[][] results);
}
