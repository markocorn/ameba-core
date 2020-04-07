package ameba.core.reproductions.parametersOperations.genParCrossover;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

/**
 * Created by marko on 12/27/16.
 */
public class CombineAddInt extends ParOperation implements ICrossoverInt {

    public CombineAddInt(ParOperationSettings parOperationSettings) {
        super(parOperationSettings);
    }

    @Override
    public int crossover(int par1, int par2) {
        par1 = par1 + par2;
        return limitInteger(par1);
    }
}
