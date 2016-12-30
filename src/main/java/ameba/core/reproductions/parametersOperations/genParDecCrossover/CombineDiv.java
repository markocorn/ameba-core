package ameba.core.reproductions.parametersOperations.genParDecCrossover;

/**
 * Created by marko on 12/27/16.
 */
public class CombineDiv implements ICrossDec {
    /**
     * Crossover that divide value of first and second parameter.
     *
     * @param par1     Parameter to be changed.
     * @param par2     Parameter to be used for change.
     * @param minValue Minimum value of the parameter.
     * @param maxValue Maximum value od the parameter.
     * @return Crossed parameter
     */
    @Override
    public double crossover(double par1, double par2, double minValue, double maxValue) {
        if (par2 == 0.0) {
            par1 = 0.0;
        } else {
            par1 /= par2;
        }
        if (par1 > maxValue) {
            par1 = maxValue;
        }
        if (par1 < minValue) {
            par1 = minValue;
        }
        return par1;
    }
}
