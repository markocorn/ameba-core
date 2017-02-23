package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.blocks.Signal;

/**
 * Created by marko on 12/27/16.
 */
public class MixSign implements IMutate {
    private Signal minValue;
    private Signal maxValue;

    public MixSign(Signal minValue, Signal maxValue) {
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
    public Signal mutate(Signal par) throws Exception {
        if (par.gettClass().isAssignableFrom(Double.class)) {
            par.setValueDouble(-par.getValueDouble());
            if (par.getValueDouble() > maxValue.getValueDouble()) {
                par.setValueDouble(maxValue.getValueDouble());
            }
            if (par.getValueDouble() < minValue.getValueDouble()) {
                par.setValueDouble(minValue.getValueDouble());
            }
            return par;
        }
        if (par.gettClass().isAssignableFrom(Integer.class)) {
            par.setValueInteger(-par.getValueInteger());
            if (par.getValueInteger() > maxValue.getValueInteger()) {
                par.setValueInteger(maxValue.getValueInteger());
            }
            if (par.getValueInteger() < minValue.getValueInteger()) {
                par.setValueInteger(minValue.getValueInteger());
            }
            return par;
        }
        throw new Exception("InputDec parameter not of allowed type.");
    }
}
