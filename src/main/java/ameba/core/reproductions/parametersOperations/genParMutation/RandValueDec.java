package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.reproductions.parametersOperations.ParOperation;
import ameba.core.reproductions.parametersOperations.ParOperationSettings;

import java.util.Random;

/**
 * Created by marko on 12/27/16.
 */
public class RandValueDec extends ParOperation implements IOperationDec {
    private Random random;

    public RandValueDec(ParOperationSettings parOperationSettings) {
        super(parOperationSettings);
        this.random = new Random();
    }

    @Override
    public double mutate(double par) throws Exception {
        return random.nextDouble() * (getParOperationSettings().getValueLimitDec()[1] - getParOperationSettings().getValueLimitDec()[0]) + getParOperationSettings().getValueLimitDec()[0];
    }

}
