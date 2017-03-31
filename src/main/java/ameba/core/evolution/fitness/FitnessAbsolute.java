package ameba.core.evolution.fitness;

/**
 * Created by marko on 3/31/17.
 */
public class FitnessAbsolute extends Fitness {

    public FitnessAbsolute(double[][] dataRefDec, int[][] dataRefInt, boolean[][] dataRefBin, double weightDec, double weightInt, double weightBin) {
        super(dataRefDec, dataRefInt, dataRefBin, weightDec, weightInt, weightBin);
    }

    @Override
    public double clcFitnessDec(double[][] results) {
        double fit = 0.0;
        for (int i = 0; i < results.length; i++) {
            for (int j = 0; j < results[i].length; j++) {
                fit += Math.abs(results[i][j] - dataRefDec[i][j]);
            }
        }
        return fit * weightDec;
    }

    @Override
    public double clcFitnessInt(int[][] results) {
        double fit = 0.0;
        for (int i = 0; i < results.length; i++) {
            for (int j = 0; j < results[i].length; j++) {
                fit += Math.abs(results[i][j] - dataRefInt[i][j]);
            }
        }
        return fit * weightInt;
    }

    @Override
    public double clcFitnessBin(boolean[][] results) {
        double fit = 0.0;
        for (int i = 0; i < results.length; i++) {
            for (int j = 0; j < results[i].length; j++) {
                if (results[i][j] ^ dataRefBin[i][j]) fit += 1;
            }
        }
        return fit * weightBin;
    }
}
