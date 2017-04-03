package ameba.core.blocks.collectors;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by marko on 1/10/17.
 */
public class Collector implements Serializable {
    private ArrayList<Edge> edges;
    private Node nodeAttached;


    public Collector(Node node) {
        edges = new ArrayList<>();
        nodeAttached = node;
    }

    public Node getNodeAttached() {
        return nodeAttached;
    }

    public void setNodeAttached(Node nodeAttached) {
        this.nodeAttached = nodeAttached;
    }


    public ArrayList<Edge> getEdges() {
        return edges;
    }


    public void removeEdge(Edge edge) throws Exception {
        edges.remove(edge);
    }

    public void removeEdges() throws Exception {
        for (Edge edge : edges) {
            removeEdge(edge);
        }
    }

    public void addEdge(Edge edge) throws Exception {
    }

    public boolean isSignalReady() {
        return false;
    }

    public Cell.Signal getType() {
        return null;
    }
}
