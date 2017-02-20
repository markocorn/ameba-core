package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.blocks.Signal;

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
    public Signal mutate(Signal par) throws Exception {
        //Add random value with constrains to parameter
        if (par.gettClass().isAssignableFrom(Double.class)) {
            double p = random.nextDouble() * (maxChange.getValueDouble() - minChange.getValueDouble()) + minChange.getValueDouble();
            p += par.getValueDouble();
            if (p > maxValue.getValueDouble()) {
                p = maxValue.getValueDouble();
            }
            if (p < minValue.getValueDouble()) {
                p = minValue.getValueDouble();
            }
            return Signal.createDouble(p);
        }
        if (par.gettClass().isAssignableFrom(Integer.class)) {
            int p = random.nextInt(maxChange.getValueInteger() - minChange.getValueInteger()) + minChange.getValueInteger();
            p += par.getValueInteger();
            if (p > maxValue.getValueInteger()) {
                p = maxValue.getValueInteger();
            }
            if (p < minValue.getValueInteger()) {
                p = minValue.getValueInteger();
            }
            return Signal.createInteger(p);
        }
        if (par.gettClass().isAssignableFrom(Boolean.class)) {
            if (maxValue.getValueDouble().equals(false)) {
                return Signal.createBoolean(false);
            }
            if (minValue.getValueBoolean().equals(true)) {
                return Signal.createBoolean(true);
            }
            return Signal.createBoolean(!par.getValueBoolean());
        }
        throw new Exception("Input parameter not of allowed type.");

    }
}
