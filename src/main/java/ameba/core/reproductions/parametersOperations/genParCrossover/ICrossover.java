package ameba.core.reproductions.parametersOperations.genParCrossover;

import ameba.core.blocks.Signal;

/**
 * Created by marko on 12/27/16.
 */
public interface ICrossover {
    Signal crossover(Signal par1, Signal par2) throws Exception;
}
