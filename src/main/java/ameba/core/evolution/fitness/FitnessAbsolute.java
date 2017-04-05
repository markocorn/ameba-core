package ameba.core.evolution.fitness;

/**
 * Created by marko on 3/31/17.
 */
public class FitnessAbsolute extends Fitness {

    public FitnessAbsolute(double weightDec, double weightInt, double weightBin) {
        super(weightDec, weightInt, weightBin);
    }

    @Override
    public double clcFitnessDec(double[] results, double[] resultsRef) {
        double fit = 0.0;
        for (int i = 0; i < results.length; i++) {
            fit += Math.abs(results[i] - resultsRef[i]);
            }
        return fit * weightDec;
    }

    @Override
    public double clcFitnessInt(int[] results, int[] resultsRef) {
        double fit = 0.0;
        for (int i = 0; i < results.length; i++) {
            fit += Math.abs(results[i] - resultsRef[i]);
        }
        return fit * weightDec;
    }

    @Override
    public double clcFitnessBin(boolean[] results, boolean[] resultsRef) {
        double fit = 0.0;
        for (int i = 0; i < results.length; i++) {
            if (results[i] ^ resultsRef[i]) fit += 1;
            }
        return fit * weightBin;
    }
}
