package ameba.core.blocks.connections;

/**
 * Created by marko on 1/19/17.
 */
public interface ICollector<T> {
    T getSignal(Class<T> tClass);

    boolean isSignalReady();
}
