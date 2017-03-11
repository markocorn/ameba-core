package ameba.core.blocks.collectors;

import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorTarget extends Collector implements ICollector {

    public CollectorTarget(Node node) {
        super(node);
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

    public void addEdge(Edge edge) throws Exception {
        getEdges().clear();
        getEdges().add(edge);
    }
}
