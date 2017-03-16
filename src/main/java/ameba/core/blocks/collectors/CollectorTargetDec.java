package ameba.core.blocks.collectors;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.blocks.nodes.Node;

import java.util.ArrayList;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorTargetDec extends CollectorTarget {
    ArrayList<EdgeDec> edges;

    public CollectorTargetDec(Node node) {
        super(node);
        edges = new ArrayList<>();
    }

    public void addEdge(EdgeDec edge) throws Exception {
        edges.clear();
        edges.add(edge);
        getEdges().clear();
        getEdges().add(edge);
    }

    @Override
    public void removeEdge(Edge edge) throws Exception {
        super.removeEdge(edge);
        edges.remove(edge);
    }

    public double getSignal() {
        return edges.get(0).getSignal();
    }

    @Override
    public Cell.Signal getType() {
        return Cell.Signal.DECIMAL;
    }
}
