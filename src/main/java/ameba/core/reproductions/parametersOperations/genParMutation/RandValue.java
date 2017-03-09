package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.blocks.Signal;
import ameba.core.reproductions.parametersOperations.RepParSettings;

import java.util.Random;

/**
 * Created by marko on 12/27/16.
 */
public class RandValue implements IMutate {
    RepParSettings repParSettings;
    private Random random;

    public RandValue(RepParSettings repParSettings) {
        this.repParSettings = repParSettings;
        this.random = new Random();
    }

    @Override
    public Signal mutate(Signal par) throws Exception {
        if (par.gettClass().isAssignableFrom(Double.class)) {
            return Signal.createDouble(random.nextDouble() * (repParSettings.getValueLimitDec()[1] - repParSettings.getValueLimitDec()[0]) + repParSettings.getValueLimitDec()[0]);
        }
        if (par.gettClass().isAssignableFrom(Integer.class)) {
            return Signal.createInteger(random.nextInt(repParSettings.getValueLimitInt()[1] - repParSettings.getValueLimitInt()[0]) + repParSettings.getValueLimitInt()[0]);
        }
        if (par.gettClass().isAssignableFrom(Boolean.class)) {
            if (repParSettings.getValueLimitBin()[1].equals(false)) {
                return Signal.createBoolean(false);
            }
            if (repParSettings.getValueLimitBin()[0].equals(true)) {
                return Signal.createBoolean(true);
            }
            return Signal.createBoolean(!par.getValueBoolean());
        }
        throw new Exception("Input parameter not of allowed type.");
    }

    @Override
    public RepParSettings getSettings() {
        return repParSettings;
    }
}
