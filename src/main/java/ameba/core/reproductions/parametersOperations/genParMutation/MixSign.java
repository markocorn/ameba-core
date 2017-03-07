package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.blocks.Signal;
import ameba.core.reproductions.parametersOperations.RepParSettings;

/**
 * Created by marko on 12/27/16.
 */
public class MixSign implements IMutate {
    RepParSettings repParSettings;

    public MixSign(RepParSettings repParSettings) {
        this.repParSettings = repParSettings;
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
            if (par.getValueDouble() > repParSettings.getValueLimitDec()[1]) {
                par.setValueDouble(repParSettings.getValueLimitDec()[1]);
            }
            if (par.getValueDouble() < repParSettings.getValueLimitDec()[0]) {
                par.setValueDouble(repParSettings.getValueLimitDec()[0]);
            }
        }
        if (par.gettClass().isAssignableFrom(Integer.class)) {
            par.setValueInteger(-par.getValueInteger());
            if (par.getValueInteger() > repParSettings.getValueLimitInt()[1]) {
                par.setValueInteger(repParSettings.getValueLimitInt()[1]);
            }
            if (par.getValueInteger() < repParSettings.getValueLimitInt()[0]) {
                par.setValueInteger(repParSettings.getValueLimitInt()[0]);
            }
            return par;
        }
        throw new Exception("Input parameter not of allowed type.");
    }
}
