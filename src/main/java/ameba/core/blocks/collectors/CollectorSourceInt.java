package ameba.core.blocks.collectors;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeBin;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.blocks.nodes.Node;

import java.util.ArrayList;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorSourceInt extends CollectorSource {
    int signal;
    ArrayList<EdgeInt> edges;

    public CollectorSourceInt(Node node) {
        super(node);
        signal = 0;
        edges = new ArrayList<>();
    }

    @Override
    public void addEdge(Edge edge) throws Exception {
        if (!(edge instanceof EdgeBin)) throw new Exception("Edge not of proper type");
        addEdgeInt((EdgeInt) edge);
    }

    public void addEdgeInt(EdgeInt edge) throws Exception {
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

    public int getSignal() {
        return getSignal();
    }

    public void setSignal(int signal) {
        this.signal = signal;
    }

    @Override
    public Cell.Signal getType() {
        return Cell.Signal.INTEGER;
    }
}
