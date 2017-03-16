package ameba.core.reproductions.parametersOperations.genParCrossover;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

/**
 * Created by marko on 12/27/16.
 */
public class CombineMulDec extends ParOperation implements ICrossoverDec {
    ParOperationSettings parOperationSettings;

    public CombineMulDec(ParOperationSettings parOperationSettings) {
        super(parOperationSettings);
    }

    @Override
    public double crossover(double par1, double par2) {
        par1 *= par2;
        return limitDouble(par1);
    }
}
