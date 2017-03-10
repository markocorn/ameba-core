package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

import java.util.Random;

/**
 * Created by marko on 12/21/16.
 */
public class AddValueDec extends ParOperation implements IOperationDec {
    private Random random;

    public AddValueDec(ParOperationSettings parOperationSettings) {
        super(parOperationSettings);
        random = new Random();
    }

    /**
     * Mutation that add random value to the parameter par.
     *
     * @param par Parameter to be changed.
     * @return Mutated parameter
     */
    @Override
    public Double mutate(Double par) throws Exception {
        double p = random.nextDouble() * (getParOperationSettings().getChangeLimitDec()[1] - getParOperationSettings().getChangeLimitDec()[0]) + getParOperationSettings().getChangeLimitDec()[0];
        p += par;
        return limitDouble(p);
    }
}
