package ameba.core.blocks.collectors;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;

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
public class Collector implements Serializable {
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
     * Dummy method that is overridden by implementation class.
     * <br>
     * To add and edge to the collector the limitations of the node type must be meet. Limitations are specified for each node separately.
     *
     * @param edge
     * @throws Exception if edge can't be added.Violation of limitations.
     */
    public void addEdge(Edge edge) throws Exception {
    }

    /**
     * Dummy method that is overridden by implementation class.
     * <br>
     * Collector checks if the signal is ready for the transmission to the edge or to the node which depends on the collator implementation.
     *
     * @return state of the signal. False/True ... Signal ready/Signal not ready.
     */
    public boolean isSignalReady() {
        return false;
    }

    /**
     * Dummy method that is overridden by implementation class.
     * <br>
     * Collectors signal that flows trough it can be of different data type which depends of the collector implementation.
     *
     * @return type of the Signal.
     */
    public Cell.Signal getType() {
        return null;
    }
}
