package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.blocks.Signal;
import ameba.core.reproductions.parametersOperations.RepParSettings;

import java.util.Random;

/**
 * Created by marko on 12/21/16.
 */
public class AddValue implements IMutate {
    RepParSettings repParSettings;
    private Random random;

    public AddValue(RepParSettings repParSettings) {
        this.repParSettings = repParSettings;
        random = new Random();
    }

    /**
     * Mutation that add random value to the parameter par.
     *
     * @param par Parameter to be changed.
     * @return Mutated parameter
     */
    @Override
    public Signal mutate(Signal par) throws Exception {
        //AddDec random value with constrains to parameter
        if (par.gettClass().isAssignableFrom(Double.class)) {
            double p = random.nextDouble() * (repParSettings.getChangeLimitDec()[1] - repParSettings.getChangeLimitDec()[0]) + repParSettings.getChangeLimitDec()[0];
            p += par.getValueDouble();
            if (p > repParSettings.getValueLimitDec()[1]) {
                p = repParSettings.getValueLimitDec()[1];
            }
            if (p < repParSettings.getValueLimitDec()[0]) {
                p = repParSettings.getValueLimitDec()[0];
            }
            return Signal.createDouble(p);
        }
        if (par.gettClass().isAssignableFrom(Integer.class)) {
            int p = random.nextInt(repParSettings.getChangeLimitInt()[1] - repParSettings.getChangeLimitInt()[0]) + repParSettings.getChangeLimitInt()[0];
            p += par.getValueInteger();
            if (p > repParSettings.getValueLimitInt()[1]) {
                p = repParSettings.getValueLimitInt()[1];
            }
            if (p < repParSettings.getValueLimitInt()[0]) {
                p = repParSettings.getValueLimitInt()[0];
            }
            return Signal.createInteger(p);
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
}
