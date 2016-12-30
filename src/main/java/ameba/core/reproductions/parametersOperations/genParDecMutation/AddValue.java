package ameba.core.reproductions.parametersOperations.genParDecMutation;

import java.util.Random;

/**
 * Created by marko on 12/21/16.
 */
public class AddValue implements IMutateDec {
    private Random random;
    private double maxChange;
    private double minChange;
    private double minValue;
    private double maxValue;

    public AddValue(double maxChange, double minChange, double minValue, double maxValue) {
        this.maxChange = maxChange;
        this.minChange = minChange;
        this.minValue = minValue;
        this.maxValue = maxValue;
        random = new Random();
    }

    /**
     * Mutation that add random value to the parameter par.
     *
     * @param par Parameter to be changed.
     * @return Mutated parameter
     */
    @Override
    public double mutate(double par) {
        //Add random value with constrains to parameter
        par += random.nextDouble() * (maxChange - minChange) + minChange;
        if (par > maxValue) {
            par = maxValue;
        }
        if (par < minValue) {
            par = minValue;
        }
        return par;
    }
}
