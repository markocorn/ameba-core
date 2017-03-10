package ameba.core.reproductions.parametersOperations.genParCrossover;

import ameba.core.blocks.Signal;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

/**
 * Created by marko on 12/27/16.
 */
public class CombineDiv implements ICrossover {
    ParOperationSettings parOperationSettings;

    public CombineDiv(ParOperationSettings parOperationSettings) {
        this.parOperationSettings = parOperationSettings;
    }

    @Override
    public Signal crossover(Signal par1, Signal par2) throws Exception {
        if (par1.gettClass().equals(par2.gettClass())) {
            //AddDec random value with constrains to parameter
            if (par1.gettClass().isAssignableFrom(Double.class)) {
                par1.setValue(Signal.divSignal(par1, par2));
                if (par1.getValueDouble() > parOperationSettings.getValueLimitDec()[1]) {
                    par1.setValueDouble(parOperationSettings.getValueLimitDec()[1]);
                }
                if (par1.getValueDouble() < parOperationSettings.getValueLimitDec()[0]) {
                    par1.setValueDouble(parOperationSettings.getValueLimitDec()[0]);
                }
                return par1;
            }
            throw new Exception("Input parameter not of allowed type.");
        } else throw new Exception("Parameter1 and parameter2 not same type");
    }

    @Override
    public ParOperationSettings getSettings() {
        return getSettings();
    }
}
