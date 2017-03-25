package ameba.core.reproductions.parametersOperations.genParCrossover;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

/**
 * Created by marko on 12/27/16.
 */
public class CopyValueBin extends ParOperation implements ICrossoverBin {

    public CopyValueBin(ParOperationSettings parOperationSettings) {
        super(parOperationSettings);
    }

    @Override
    public boolean crossover(boolean par1, boolean par2) {
        par1 = par2;
        return limitBoolean(par1);
    }
}