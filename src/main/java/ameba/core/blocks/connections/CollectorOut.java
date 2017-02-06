package ameba.core.blocks.connections;

import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorOut extends Collector implements ICollector {


    public CollectorOut(Signal signal, int minEdges, int maxEdges, Node node) {
        super(signal, minEdges, maxEdges, node);
    }

    @Override
    public Signal getSignal() throws Exception {
        return super.getSignal();
    }

    @Override
    public boolean isSignalReady() {
        return getNodeAttached().isSignalReady();
    }

}
