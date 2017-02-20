package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.blocks.Signal;

/**
 * Created by marko on 12/27/16.
 */
public class InverseValue implements IMutate {
    private Signal minValue;
    private Signal maxValue;

    public InverseValue(Signal minValue, Signal maxValue) {
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
    public Signal mutate(Signal par) throws Exception {
        if (par.gettClass().isAssignableFrom(Double.class)) {
            par.setValueDouble(1.0 / par.getValueDouble());
            if (par.getValueDouble() > maxValue.getValueDouble()) {
                par.setValueDouble(maxValue.getValueDouble());
            }
            if (par.getValueDouble() < minValue.getValueDouble()) {
                par.setValueDouble(minValue.getValueDouble());
            }
            return par;
        }
        throw new Exception("Input parameter not of allowed type.");
    }
}
