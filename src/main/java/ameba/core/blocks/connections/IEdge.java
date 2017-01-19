package ameba.core.blocks.connections;

/**
 * Created by marko on 1/19/17.
 */
public interface IEdge {
    <T> T getWeight();

    <T> void setWeight(T weight);

    <T> T getSignal(Class<T> tClass);
}
