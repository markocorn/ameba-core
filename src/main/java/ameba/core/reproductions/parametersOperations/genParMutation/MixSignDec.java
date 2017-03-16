package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

/**
 * Created by marko on 12/27/16.
 */
public class MixSignDec extends ParOperation implements IOperationDec {

    public MixSignDec(ParOperationSettings parOperationSettings) {
        super(parOperationSettings);
    }

    /**
     * Mutation changes sign of parameter.
     *
     * @param par Parameter to be changed.
     * @return Mutated parameter
     */
    @Override
    public double mutate(double par) throws Exception {
        par = -par;
        return limitDouble(par);
    }
}
