package ameba.core.blocks;

import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;

import java.util.ArrayList;

/**
 * Created by marko on 1/10/17.
 */
public class Collector<T> {
    ArrayList<Edge<T>> inpEdges;
    ArrayList<Edge<T>> outEdges;
    private int minInpEdges;
    private int maxInpEdges;
    private int minOutEdges;
    private int maxOutEdges;
    private boolean signalReady;
    private Node nodeAttached;


    public Collector(int minInpEdges, int maxInpEdges, int minOutEdges, int maxOutEdges, Node node) {
        this.minInpEdges = minInpEdges;
        this.maxInpEdges = maxInpEdges;
        this.minOutEdges = minOutEdges;
        this.maxOutEdges = maxOutEdges;
        nodeAttached = node;
    }

    public Node getNodeAttached() {
        return nodeAttached;
    }

    public void setNodeAttached(Node nodeAttached) {
        this.nodeAttached = nodeAttached;
    }

    public void addInpEdge(Edge edge) throws Exception {
        if (maxInpEdges > inpEdges.size()) {
            inpEdges.add(edge);
        } else {
            throw new Exception("Input edge can't be added.");
        }
    }

    public void addOutEdge(Edge edge) throws Exception {
        if (maxOutEdges > outEdges.size()) {
            outEdges.add(edge);
        } else {
            throw new Exception("Output edge can't be added.");
        }
    }

    public ArrayList<Edge<T>> getInpEdges() {
        return inpEdges;
    }

    public void setInpEdges(ArrayList<Edge<T>> inpEdges) throws Exception {
        if (inpEdges.size() > maxInpEdges) {
            throw new Exception("Points number is greater than allowed");
        }
        if (inpEdges.size() < minInpEdges) {
            throw new Exception("Points number is less than allowed");
        }
        this.inpEdges = inpEdges;
    }

    public void setOutEdges(ArrayList<Edge<T>> outEdges) throws Exception {
        if (outEdges.size() > maxOutEdges) {
            throw new Exception("Points number is greater than allowed");
        }
        if (outEdges.size() < minOutEdges) {
            throw new Exception("Points number is less than allowed");
        }
        this.outEdges = outEdges;
    }

    public void removeInpEdge(Edge edge) throws Exception {
        if (inpEdges.size() >= minInpEdges) {
            inpEdges.remove(edge);
        } else throw new Exception("Can't remove edge. Violation of minimum number of inpEdges.");
    }

    public void removeOutEdge(Edge edge) throws Exception {
        if (outEdges.size() >= minOutEdges) {
            outEdges.remove(edge);
        } else throw new Exception("Can't remove edge. Violation of minimum number of inpEdges.");
    }

    public void setInpEdge(Edge<T> edge) {
        inpEdges.set(inpEdges.indexOf(edge), edge);
    }

    public void setOutEdge(Edge<T> edge) {
        outEdges.set(outEdges.indexOf(edge), edge);
    }

    public boolean isSignalReady() {
        return source.isSignalReady();
    }
}
