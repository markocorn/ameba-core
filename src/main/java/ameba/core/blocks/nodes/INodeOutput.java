package ameba.core.blocks.nodes;

/**
 * Created by marko on 1/17/17.
 */
public interface INodeOutput {
    <T> T exportSignal();
}
