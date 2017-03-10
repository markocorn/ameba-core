package ameba.core.reproductions.parametersOperations.genParCrossover;

import ameba.core.blocks.Signal;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

/**
 * Created by marko on 12/27/16.
 */
public class CombineOr implements ICrossover {
    ParOperationSettings parOperationSettings;

    public CombineOr(ParOperationSettings parOperationSettings) {
        this.parOperationSettings = parOperationSettings;
    }

    @Override
    public Signal crossover(Signal par1, Signal par2) throws Exception {
        if (par1.gettClass().equals(par2.gettClass())) {
            if (par1.gettClass().isAssignableFrom(Boolean.class)) {
                if (parOperationSettings.getValueLimitBin()[1].equals(false)) {
                    return Signal.createBoolean(false);
                }
                if (parOperationSettings.getValueLimitBin()[0].equals(true)) {
                    return Signal.createBoolean(true);
                }
                return Signal.createBoolean(par1.getValueBoolean() || par2.getValueBoolean());
            }
            throw new Exception("InputDec parameter not of allowed type.");
        } else throw new Exception("Parameter1 and parameter2 not same type");
    }

    @Override
    public ParOperationSettings getSettings() {
        return parOperationSettings;
    }
}
