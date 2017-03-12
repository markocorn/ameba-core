package ameba.core.blocks.collectors;

import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeBin;
import ameba.core.blocks.nodes.Node;

import java.util.ArrayList;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorSourceBin extends CollectorSource {
    boolean signal;
    ArrayList<EdgeBin> edges;

    public CollectorSourceBin(Node node) {
        super(node);
        signal = false;
        edges = new ArrayList<>();
    }


    public void addEdge(EdgeBin edge) throws Exception {
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

    public void setSignal(boolean signal) {
        this.signal = signal;
    }

    public boolean getSignal() {
        return signal;
    }
}
