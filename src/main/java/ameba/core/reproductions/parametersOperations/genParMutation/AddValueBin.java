package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

import java.util.Random;

/**
 * Created by marko on 12/21/16.
 */
public class AddValueBin extends ParOperation implements IOperationBin {
    private Random random;

    public AddValueBin(ParOperationSettings parOperationSettings) {
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
    public Boolean mutate(Boolean par) throws Exception {
        par = !par;
        return limitBoolean(par);

    }
}
