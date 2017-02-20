package ameba.core.reproductions.parametersOperations.genParCrossover;

import ameba.core.blocks.Signal;

/**
 * Created by marko on 12/27/16.
 */
public class CopyValue implements ICrossover {
    private Signal minValue;
    private Signal maxValue;

    public CopyValue(Signal minValue, Signal maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public Signal crossover(Signal par1, Signal par2) throws Exception {
        if (par1.gettClass().equals(par2.gettClass())) {
            //Add random value with constrains to parameter
            if (par1.gettClass().isAssignableFrom(Double.class)) {
                par1.setValue(par2);
                if (par1.getValueDouble() > maxValue.getValueDouble()) {
                    par1.setValueDouble(maxValue.getValueDouble());
                }
                if (par1.getValueDouble() < minValue.getValueDouble()) {
                    par1.setValueDouble(minValue.getValueDouble());
                }
                return par1;
            }
            if (par1.gettClass().isAssignableFrom(Integer.class)) {
                par1.setValue(par2);
                if (par1.getValueInteger() > maxValue.getValueInteger()) {
                    par1.setValueInteger(maxValue.getValueInteger());
                }
                if (par1.getValueInteger() < minValue.getValueInteger()) {
                    par1.setValueInteger(minValue.getValueInteger());
                }
                return par1;
            }
            if (par1.gettClass().isAssignableFrom(Boolean.class)) {
                if (maxValue.getValueDouble().equals(false)) {
                    return Signal.createBoolean(false);
                }
                if (minValue.getValueBoolean().equals(true)) {
                    return Signal.createBoolean(true);
                }
                return par2;
            }
            throw new Exception("Input parameter not of allowed type.");
        } else throw new Exception("Parameter1 and parameter2 not same type");
    }
}
