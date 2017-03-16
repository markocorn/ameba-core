package ameba.core.blocks.collectors;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.blocks.nodes.Node;

import java.util.ArrayList;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorSourceDec extends CollectorSource {
    double signal;
    ArrayList<EdgeDec> edges;

    public CollectorSourceDec(Node node) {
        super(node);
        signal = 0.0;
        edges = new ArrayList<>();

    }

    public void addEdge(EdgeDec edge) throws Exception {
        if (!edges.contains(edge)) {
            edges.add(edge);
            getEdges().add(edge);
        } else throw new Exception("Edge to be added already contained in source collector");
    }

    @Override
    public void removeEdge(Edge edge) throws Exception {
        super.removeEdge(edge);
        edges.remove(edge);
    }

    public double getSignal() {
        return signal;
    }

    public void setSignal(double signal) {
        this.signal = signal;
    }

    @Override
    public Cell.Signal getType() {
        return Cell.Signal.DECIMAL;
    }
}
