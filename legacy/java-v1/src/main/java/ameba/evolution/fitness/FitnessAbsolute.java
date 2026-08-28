package ameba.evolution.fitness;

/**
 * Created by marko on 3/31/17.
 */
public class FitnessAbsolute extends Fitness {

    public FitnessAbsolute(Double[] weight) {
        super(weight);
    }

    @Override
    public double clcFitness(double[] results, double[] resultsRef) {
        double fit = 0.0;
        for (int i = 0; i < results.length; i++) {
            fit += Math.abs(results[i] - resultsRef[i]) * weight[i];
        }
        return fit;
    }
}
