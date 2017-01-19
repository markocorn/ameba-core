package ameba.core.blocks.connections;

import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorOut extends Collector implements ICollector {

    public CollectorOut(int minEdges, int maxEdges, Node node) {
        super(minEdges, maxEdges, node);
    }

    @Override
    public Object getSignal(Class aClass) {
        return getNodeAttached().getSignal(aClass);
    }

    @Override
    public boolean isSignalReady() {
        return getNodeAttached().isSignalReady();
    }

}
