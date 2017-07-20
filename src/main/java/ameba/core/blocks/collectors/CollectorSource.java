package ameba.core.blocks.collectors;

import ameba.core.blocks.nodes.Node;

/**
 * CollectorSource is class that is defined as output collector of the node which means that it loads signal
 * from the node output calculation and transmit it to the connected edges.
 *
 */
public class CollectorSource extends Collector {
    /**
     * Construct source collector with it's node.
     *
     * @param node to which collector belongs.
     */
    public CollectorSource(Node node) {
        super(node);
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

}
