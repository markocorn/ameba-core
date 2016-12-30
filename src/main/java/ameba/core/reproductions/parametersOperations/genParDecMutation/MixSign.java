package ameba.core.reproductions.parametersOperations.genParDecMutation;

/**
 * Created by marko on 12/27/16.
 */
public class MixSign implements IMutateDec {
    private double minValue;
    private double maxValue;

    public MixSign(double minValue, double maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Mutation changes sign of parameter.
     *
     * @param par Parameter to be changed.
     * @return Mutated parameter
     */
    @Override
    public double mutate(double par) {
        par *= -1.0;
        if (par > maxValue) {
            par = maxValue;
        }
        if (par < minValue) {
            par = minValue;
        }
        return par;
    }
}
