package ameba.core.blocks;

import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorInp extends Collector implements ICollector {

    public CollectorInp(Signal signal, Node node) {
        super(signal, node);
    }

    @Override
    public Signal getSignal() throws Exception {
        if (getEdges().size() > 0) {
            return getEdges().get(0).getSignal();
        } else return null;

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
