package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.Signal;

/**
 * Created by marko on 1/17/17.
 */
public interface INodeInput {
    void importSignal(Signal signal);
}
