package ameba.core.blocks.collectors;

import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;

/**
 * CollectorTarget is class that is defined as input collector to the node which means that it loads signal
 * from the connected edge and transmit it to the node as node's input signal. Node than calculate outputs from this information.
 */
public class CollectorTarget extends Collector {
    /**
     * Construct target collector with it's node.
     *
     * @param node to which collector belongs.
     */
    public CollectorTarget(Node node) {
        super(node);
    }

    @Override
    public void addEdge(Edge edge) throws Exception {
        getEdges().clear();
        getEdges().add(edge);
    }

    @Override
    public double getSignal() {
        return getEdges().get(0).getSignal();
    }

    /**
     * Override parent method of checking if signal is ready.
     * <br>
     * As for the target type of collector the readiness of the signal is determent by the edges to which it is connected.
     * If the attached edges all signal for signal readies than collector can collect signals from its edges.
     *
     * @return state of the signal. False/True ... Signal ready/Signal not ready.
     */
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
