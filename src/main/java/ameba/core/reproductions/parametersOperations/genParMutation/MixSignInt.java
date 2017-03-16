package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

/**
 * Created by marko on 12/27/16.
 */
public class MixSignInt extends ParOperation implements IOperationInt {

    public MixSignInt(ParOperationSettings parOperationSettings) {
        super(parOperationSettings);
    }

    /**
     * Mutation changes sign of parameter.
     *
     * @param par Parameter to be changed.
     * @return Mutated parameter
     */
    @Override
    public int mutate(int par) throws Exception {
        par = -par;
        return limitInteger(par);
    }
}
