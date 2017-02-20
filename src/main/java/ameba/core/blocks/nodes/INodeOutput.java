package ameba.core.blocks.nodes;

import ameba.core.blocks.Signal;

/**
 * Created by marko on 1/17/17.
 */
public interface INodeOutput {
    Signal exportSignal() throws Exception;
}
