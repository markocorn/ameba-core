package ameba.evolution.fitness;

/**
 * Created by marko on 3/31/17.
 */
public class FitnessSquare extends Fitness {

    public FitnessSquare(Double[] weight) {
        super(weight);
    }

    @Override
    public double clcFitness(double[] results, double[] resultsRef) {
        double fit = 0.0;
        for (int i = 0; i < results.length; i++) {
            fit += Math.pow(results[i] - resultsRef[i], 2) * weight[i];
        }
        return fit;
    }
}
