package ameba.core.blocks.connections;

import ameba.core.blocks.nodes.Node;

import java.util.ArrayList;

/**
 * Created by marko on 1/10/17.
 */
public class Collector {
    private ArrayList<Edge> edges;
    private int minEdges;
    private int maxEdges;
    private Node nodeAttached;
    private Signal signal;


    public Collector(Signal signal, int minEdges, int maxEdges, Node node) {
        this.minEdges = minEdges;
        this.maxEdges = maxEdges;
        edges = new ArrayList<>();
        nodeAttached = node;
        this.signal = signal;
    }

    public <T> Class<T> getType() {
        return signal.gettClass();
    }

    public Signal getSignal() throws Exception {
        return signal;
    }

    public void setSignal(Signal signal) {
        this.signal = signal;
    }

    public Node getNodeAttached() {
        return nodeAttached;
    }

    public void setNodeAttached(Node nodeAttached) {
        this.nodeAttached = nodeAttached;
    }

    public void addEdge(Edge edge) throws Exception {
        if (maxEdges > edges.size()) {
            edges.add(edge);
        } else {
            throw new Exception("Edge can't be added.");
        }
    }

    public ArrayList<Edge> getEdges() {
        return edges;
    }

    public void setEdges(ArrayList<Edge> edges) throws Exception {
        if (edges.size() > maxEdges) {
            throw new Exception("Points number is greater than allowed");
        }
        if (edges.size() < minEdges) {
            throw new Exception("Points number is less than allowed");
        }
        this.edges = edges;
    }

    public void removeEdge(Edge edge) throws Exception {
        if (edges.size() >= minEdges) {
            edges.remove(edge);
        } else throw new Exception("Can't remove edge. Violation of minimum number of connections.");
    }

    public void setEdge(Edge edge) {
        edges.set(edges.indexOf(edge), edge);
    }

    public void removeEdges() throws Exception {
        if (edges.size() >= minEdges)
            for (Edge edge : edges) {
                removeEdge(edge);
            }
    }
}
