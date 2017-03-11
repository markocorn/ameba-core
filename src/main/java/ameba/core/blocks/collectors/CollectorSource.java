package ameba.core.blocks.collectors;

import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorSource extends Collector implements ICollector {

    public CollectorSource(Node node) {
        super(node);
    }

    @Override
    public boolean isSignalReady() {
        return getNodeAttached().isSignalReady();
    }

    public void addEdge(Edge edge) throws Exception {
        if (!getEdges().contains(edge)) {
            getEdges().add(edge);
        } else throw new Exception("Edge to be added already contained in out collector");
    }
}
