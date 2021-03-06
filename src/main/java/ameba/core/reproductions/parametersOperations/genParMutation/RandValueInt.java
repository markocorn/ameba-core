package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

import java.util.Random;

/**
 * Created by marko on 12/27/16.
 */
public class RandValueInt extends ParOperation implements IOperationInt {
    private Random random;

    public RandValueInt(ParOperationSettings parOperationSettings, long seed) {
        super(parOperationSettings);
        this.random = new Random(seed);
    }

    @Override
    public int mutate(int par) throws Exception {
        return random.nextInt(getParOperationSettings().getValueLimitInt()[1] - getParOperationSettings().getValueLimitInt()[0]) + getParOperationSettings().getValueLimitInt()[0];
    }
}