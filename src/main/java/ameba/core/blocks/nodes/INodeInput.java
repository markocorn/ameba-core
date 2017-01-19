package ameba.core.blocks.nodes;

/**
 * Created by marko on 1/17/17.
 */
public interface INodeInput {
    <T> void importSignal(Class<T> tClass, Object signal);
}
