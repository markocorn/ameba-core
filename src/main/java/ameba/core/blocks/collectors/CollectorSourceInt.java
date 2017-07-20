package ameba.core.blocks.collectors;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.blocks.nodes.Node;

import java.util.ArrayList;

/**
 * CollectorSourceInt is class that defines collector as integer type.
 * <p>
 * Which means that collector can handle only integer type of signals.
 * Collector serves as storage for the node's output calculation.
 * In this collectors the result of nodes mapping function are stored.
 * Number of this type of collector defines the number of integer outputs of the node.
 */
public class CollectorSourceInt extends CollectorSource {
    /**
     * Variable for the result of the node mapping function.
     */
    int signal;
    /**
     * List of integer type of edges through which signal is transferred to next node.
     */
    ArrayList<EdgeInt> edges;

    /**
     * Construct collector with it's node.
     *
     * @param node to which collector belongs.
     */
    public CollectorSourceInt(Node node) {
        super(node);
        signal = 0;
        edges = new ArrayList<>();
    }

    /**
     * Override method for adding edge.
     * <br>
     * Only EdgeInt type of edge can be added.
     *
     * @param edge to be added.
     * @throws Exception if edge is not of EdgeInt type.
     */
    @Override
    public void addEdge(Edge edge) throws Exception {
        if (!(edge instanceof EdgeInt)) throw new Exception("Edge not of proper type");
        addEdgeInt((EdgeInt) edge);
    }

    /**
     * Add edge of type EdgeInt.
     *
     * @param edge to be added
     * @throws Exception if edge to be added is already a part of the edge list of the collector.
     */
    public void addEdgeInt(EdgeInt edge) throws Exception {
        if (!edges.contains(edge)) {
            edges.add(edge);
            getEdges().add(edge);
        } else throw new Exception("Edge to be added already contained in source collector");
    }

    /**
     * Override parent method that removes edges from parent and child edge list.
     *
     * @param edge to be removed.
     */
    @Override
    public void removeEdge(Edge edge) {
        super.removeEdge(edge);
        edges.remove(edge);
    }

    /**
     * Get signal of the collector.
     *
     * @return
     */
    public int getSignal() {
        return signal;
    }

    /**
     * Set signal of the collector.
     *
     * @param signal
     */
    public void setSignal(int signal) {
        this.signal = signal;
    }

    /**
     * Override method to return binary type of signal.
     *
     * @return integer type of signal.
     */
    @Override
    public Cell.Signal getType() {
        return Cell.Signal.INTEGER;
    }
}
