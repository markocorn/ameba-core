package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

import java.util.Random;

/**
 * Created by marko on 12/21/16.
 */
public class AddValueInt extends ParOperation implements IOperationInt {
    private Random random;

    public AddValueInt(ParOperationSettings parOperationSettings) {
        super(parOperationSettings);
        random = new Random();
    }

    @Override
    public int mutate(int par) throws Exception {
        int p = random.nextInt(getParOperationSettings().getChangeLimitInt()[1] - getParOperationSettings().getChangeLimitInt()[0]) + getParOperationSettings().getChangeLimitInt()[0];
        p += par;
        return limitInteger(p);
    }

}
