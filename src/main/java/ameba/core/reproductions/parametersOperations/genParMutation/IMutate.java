package ameba.core.reproductions.parametersOperations.genParMutation;

import ameba.core.blocks.Signal;

/**
 * Created by marko on 12/21/16.
 */
public interface IMutate {
    Signal mutate(Signal par) throws Exception;
}

