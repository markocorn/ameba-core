package ameba.core.blocks.collectors;

import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Collector is class that connects nodes with edges of the cell.
 *
 * It has two major implementations:
 * <br>
 * as source class which means that it will collect output signal from the node and transmit it to the edge that is connected to it and
 * <br>
 * as target which means that it will collect signal data from connected edge and transmit it to the node as the node's input signal.
 */
strictfp public class Collector implements Serializable {
    /**
     * List of connected edges
     * <br>
     * Collector in general can have multiple edges connected to it.
     */
    private ArrayList<Edge> edges;
    /**
     * Node to which collector belongs.
     * <br>
     * Each collector must belong to exactly one Node
     */
    private Node nodeAttached;

    /**
     * Construct general collector with it's node.
     *
     * @param node to which collector belongs.
     */
    public Collector(Node node) {
        edges = new ArrayList<>();
        nodeAttached = node;
    }

    /**
     * Get attached node.
     *
     * @return node to which collector belongs.
     */
    public Node getNodeAttached() {
        return nodeAttached;
    }

    /**
     * Set attached node.
     *
     * @param nodeAttached to which collector will belong.
     */
    public void setNodeAttached(Node nodeAttached) {
        this.nodeAttached = nodeAttached;
    }

    /**
     * Get edges of the collector.
     *
     * @return list of collector's edges.
     */
    public ArrayList<Edge> getEdges() {
        return edges;
    }

    /**
     * Remove edge from collector.
     *
     * @param edge to be removed.
     */
    public void removeEdge(Edge edge) {
        edges.remove(edge);
    }

    /**
     * Remove all edges from collector.
     */
    public void clearEdges() {
        edges.clear();
    }

    /**
     * Method for adding edge.
     * <br>
     * Only EdgeDec type of edge can be added.
     *
     * @param edge to be added.
     * @throws Exception if edge is not of EdgeDec type.
     */
    public void addEdge(Edge edge) throws Exception {

    }

    /**
     * Blind method that is overridden by implementation class.
     * <br>
     * Collector checks if the signal is ready for the transmission to the edge or to the node which depends on the collator implementation.
     *
     * @return state of the signal. False/True ... Signal ready/Signal not ready.
     */
    public boolean isSignalReady() {
        return false;
    }

    /**
     * Get signal of the collector.
     *
     * @return
     */
    public double getSignal() {
        return 0.0;
    }

    /**
     * @param signal
     */
    public void setSignal(double signal) {

    }

    public Collector clone() {
        return (Collector) SerializationUtils.clone(this);
    }
}
