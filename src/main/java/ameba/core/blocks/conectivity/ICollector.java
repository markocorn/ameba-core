package ameba.core.blocks.conectivity;

/**
 * Created by marko on 1/19/17.
 */
public interface ICollector {
    Signal getSignal() throws Exception;
    boolean isSignalReady();
}
