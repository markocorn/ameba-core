package ameba.core.blocks.collectors;

import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;

/**
 * CollectorSource is class that is defined as output collector of the node which means that it loads signal
 * from the node output calculation and transmit it to the connected edges.
 */
public class CollectorSource extends Collector {
    /**
     * Variable for the result of the node mapping function.
     */
    double signal;

    /**
     * Construct source collector with it's node.
     *
     * @param node to which collector belongs.
     */
    public CollectorSource(Node node) {
        super(node);
        signal = 0.0;
    }

    /**
     * Override parent method of checking if signal is ready.
     * <br>
     * As for the source type of collector the readiness of the signal is determent by the node calculation.
     * If the node has successfully calculated its output signal then source collector
     * can collect it and transfer it forward to the edge.
     *
     * @return state of the signal. False/True ... Signal ready/Signal not ready.
     */
    @Override
    public boolean isSignalReady() {
        return getNodeAttached().isSignalReady();
    }

    /**
     * Get signal of the collector.
     *
     * @return
     */
    @Override
    public double getSignal() {
        return signal;
    }

    /**
     * Set signal of the collector.
     *
     * @param signal
     */
    @Override
    public void setSignal(double signal) {
        this.signal = signal;
    }

    @Override
    public void addEdge(Edge edge) throws Exception {
        if (!getEdges().contains(edge)) {
            getEdges().add(edge);
        } else throw new Exception("Edge to be added already contained in source collector");
    }

}
