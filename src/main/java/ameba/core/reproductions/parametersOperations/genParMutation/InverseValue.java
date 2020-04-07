package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

/**
 * Created by marko on 12/27/16.
 */
public class InverseValue extends ParOperation implements IOperationDec {

    public InverseValue(ParOperationSettings parOperationSettings) {
        super(parOperationSettings);
    }

    /**
     * Mutation changes value of parameter by calculating it's inverse value.
     *
     * @param par Parameter to be changed.
     * @return Mutated parameter
     */
    @Override
    public double mutate(double par) throws Exception {
        par = 1.0 / par;
        return limitDouble(par);
    }
}
