package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.blocks.conectivity.Signal;

import java.util.Random;

/**
 * Created by marko on 12/21/16.
 */
public class AddValue implements IMutate {
    private Random random;
    private Signal maxChange;
    private Signal minChange;
    private Signal minValue;
    private Signal maxValue;

    public AddValue(Signal maxChange, Signal minChange, Signal minValue, Signal maxValue) {
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
    public double mutate(Signal par) {
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
