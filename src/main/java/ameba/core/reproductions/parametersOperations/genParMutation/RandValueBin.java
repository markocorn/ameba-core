package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

import java.util.Random;

/**
 * Created by marko on 12/27/16.
 */
public class RandValueBin extends ParOperation implements IOperationBin {
    private Random random;

    public RandValueBin(ParOperationSettings parOperationSettings) {
        super(parOperationSettings);
        this.random = new Random();
    }

    @Override
    public boolean mutate(boolean par) throws Exception {
        return limitBoolean(!par);
    }
}
