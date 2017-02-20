package ameba.core.reproductions.parametersOperations.genParCrossover;

import ameba.core.blocks.Signal;

/**
 * Created by marko on 12/27/16.
 */
public class CombineAnd implements ICrossover {
    private Signal minValue;
    private Signal maxValue;

    public CombineAnd(Signal minValue, Signal maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public Signal crossover(Signal par1, Signal par2) throws Exception {
        if (par1.gettClass().equals(par2.gettClass())) {
            if (par1.gettClass().isAssignableFrom(Boolean.class)) {
                if (maxValue.getValueDouble().equals(false)) {
                    return Signal.createBoolean(false);
                }
                if (minValue.getValueBoolean().equals(true)) {
                    return Signal.createBoolean(true);
                }
                return Signal.createBoolean(par1.getValueBoolean() && par2.getValueBoolean());
            }
            throw new Exception("Input parameter not of allowed type.");
        } else throw new Exception("Parameter1 and parameter2 not same type");
    }
}
