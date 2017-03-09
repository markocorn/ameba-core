package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.blocks.Signal;
import ameba.core.reproductions.parametersOperations.RepParSettings;

/**
 * Created by marko on 12/27/16.
 */
public class InverseValue implements IMutate {
    RepParSettings repParSettings;

    public InverseValue(RepParSettings repParSettings) {
        this.repParSettings = repParSettings;
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
            if (par.getValueDouble() > repParSettings.getValueLimitDec()[1]) {
                par.setValueDouble(repParSettings.getValueLimitDec()[1]);
            }
            if (par.getValueDouble() < repParSettings.getValueLimitDec()[0]) {
                par.setValueDouble(repParSettings.getValueLimitDec()[0]);
            }
            return par;
        }
        throw new Exception("Input parameter not of allowed type.");
    }

    @Override
    public RepParSettings getSettings() {
        return repParSettings;
    }
}
