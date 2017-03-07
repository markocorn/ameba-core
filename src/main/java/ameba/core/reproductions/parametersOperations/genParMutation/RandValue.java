package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.blocks.Signal;

import java.util.Random;

/**
 * Created by marko on 12/27/16.
 */
public class RandValue implements IMutate {
    private Random random;
    private Signal minValue;
    private Signal maxValue;

    public RandValue(Signal minValue, Signal maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        random = new Random();
    }

    @Override
    public Signal mutate(Signal par) throws Exception {
        if (par.gettClass().isAssignableFrom(Double.class)) {
            return Signal.createDouble(random.nextDouble() * (maxValue.getValueDouble() - minValue.getValueDouble()) + minValue.getValueDouble());
        }
        if (par.gettClass().isAssignableFrom(Integer.class)) {
            return Signal.createInteger(random.nextInt(maxValue.getValueInteger() - minValue.getValueInteger()) + minValue.getValueInteger());
        }
        if (par.gettClass().isAssignableFrom(Boolean.class)) {
            if (maxValue.getValueBoolean().equals(false)) {
                return Signal.createBoolean(false);
            }
            if (minValue.getValueBoolean().equals(true)) {
                return Signal.createBoolean(true);
            }
            return Signal.createBoolean(!par.getValueBoolean());
        }
        throw new Exception("InputDec parameter not of allowed type.");

    }
}
