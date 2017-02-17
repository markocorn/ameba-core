package ameba.core.reproductions.parametersOperations.genParMutation;

import java.util.Random;

/**
 * Created by marko on 12/27/16.
 */
public class RandValue implements IMutate {
    private Random random;
    private double maxChange;
    private double minChange;
    private double minValue;
    private double maxValue;

    public RandValue(double maxChange, double minChange, double minValue, double maxValue) {
        this.maxChange = maxChange;
        this.minChange = minChange;
        this.minValue = minValue;
        this.maxValue = maxValue;
        random = new Random();
    }

    /**
     * Mutation that generate new random value of the parameter par.
     *
     * @param par Parameter to be changed.
     * @return Mutated parameter
     */
    @Override
    public double mutate(double par) {
        par = random.nextDouble() * (maxChange - minChange) + minChange;
        if (par > maxValue) {
            par = maxValue;
        }
        if (par < minValue) {
            par = minValue;
        }
        return par;
    }
}
