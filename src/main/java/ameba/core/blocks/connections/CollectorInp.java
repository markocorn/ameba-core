package ameba.core.blocks.connections;

import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorInp extends Collector implements ICollector {

    public CollectorInp(Signal signal, Node node) {
        super(signal, 1, 1, node);
    }

    @Override
    public Signal getSignal() throws Exception {
        return getEdges().get(0).getSignal();
    }

    @Override
    public boolean isSignalReady() {
        for (Edge edge : getEdges()) {
            if (!edge.isSignalReady()) {
                return false;
            }
        }
        return true;
    }
}
