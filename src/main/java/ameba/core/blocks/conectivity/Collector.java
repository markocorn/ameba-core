package ameba.core.blocks.conectivity;

import ameba.core.blocks.nodes.Node;

import java.util.ArrayList;

/**
 * Created by marko on 1/10/17.
 */
public class Collector {
    private ArrayList<Edge> edges;
    private Node nodeAttached;
    private Signal signal;


    public Collector(Signal signal, Node node) {
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
        edges.add(edge);
    }

    public ArrayList<Edge> getEdges() {
        return edges;
    }

    public void setEdges(ArrayList<Edge> edges) throws Exception {
        this.edges = edges;
    }

    public void removeEdge(Edge edge) throws Exception {
        edges.remove(edge);
    }

    public void setEdge(Edge edge) {
        edges.set(edges.indexOf(edge), edge);
    }

    public void removeEdges() throws Exception {
        for (Edge edge : edges) {
            removeEdge(edge);
        }
    }
}
