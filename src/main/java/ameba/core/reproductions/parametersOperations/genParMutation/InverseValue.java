package ameba.core.reproductions.parametersOperations.genParMutation;

/**
 * Created by marko on 12/27/16.
 */
public class InverseValue implements IMutate {
    private double minValue;
    private double maxValue;

    public InverseValue(double minValue, double maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Mutation changes value of parameter by calculating it's inverse value.
     *
     * @param par Parameter to be changed.
     * @return Mutated parameter
     */
    @Override
    public double mutate(double par) {
        par = 1.0 / par;
        if (par > maxValue) {
            par = maxValue;
        }
        if (par < minValue) {
            par = minValue;
        }
        return par;
    }
}
