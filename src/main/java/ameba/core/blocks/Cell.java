package ameba.core.blocks;


import ameba.core.blocks.collectors.Collector;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeBin;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.blocks.nodes.*;
import com.rits.cloning.Cloner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Cell {
    double[] exportsDec;
    int[] exportsInt;
    boolean[] exportsBin;
    /**
     * Fitness value of last cell simulation run.
     */
    private double fitnessValue;
    /**
     * List of nodes without InputDec and OutputDec type.
     */
    private ArrayList<Node> nodes;
    private ArrayList<Node> innerNodes;
    private ArrayList<Node> inputNodes;
    private ArrayList<Node> outputNodes;
    /**
     * List of input nodes interfaces.
     */
    private ArrayList<INodeInputDec> inpNodesDec;
    private ArrayList<INodeInputInt> inpNodesInt;
    private ArrayList<INodeInputBin> inpNodesBin;
    /**
     * List of input nodes.
     */
    private ArrayList<INodeOutputDec> outNodesDec;
    private ArrayList<INodeOutputInt> outNodesInt;
    private ArrayList<INodeOutputBin> outNodesBin;
    private ArrayList<Edge> edges;
    private ArrayList<EdgeDec> edgesDec;
    private ArrayList<EdgeInt> edgesInt;
    private ArrayList<EdgeBin> edgesBin;
    private int maxNodes;

    /**
     *
     */
    public Cell(int maxNodes) {
        fitnessValue = 0.0;
        nodes = new ArrayList<>();
        innerNodes = new ArrayList<>();
        inpNodesDec = new ArrayList<>();
        inpNodesInt = new ArrayList<>();
        inpNodesBin = new ArrayList<>();
        outNodesDec = new ArrayList<>();
        outNodesInt = new ArrayList<>();
        outNodesBin = new ArrayList<>();
        edges = new ArrayList<>();
        edgesDec = new ArrayList<>();
        edgesInt = new ArrayList<>();
        edgesBin = new ArrayList<>();
        this.maxNodes = maxNodes;

        double[][] outDataDec = new double[][]{{0.0}};
        int[][] outDataInt = new int[][]{{1}};
        boolean[][] outDataBin = new boolean[][]{{false}};

        this.inputNodes = new ArrayList<>();
        this.outputNodes = new ArrayList<>();
    }

    public static int countGroup(ArrayList<ArrayList<Node>> group) {
        int size = 0;
        for (ArrayList<Node> list : group) {
            size += list.size();
        }
        return size;
    }

    public static boolean containsNodeGroup(ArrayList<ArrayList<Node>> group, Node node) {
        for (ArrayList<Node> list : group) {
            if (list.contains(node)) return true;
        }
        return false;
    }

    public static Class getSignalClass(Signal signal) {
        if (signal == Signal.DECIMAL) return Double.class;
        if (signal == Signal.INTEGER) return Integer.class;
        if (signal == Signal.BOOLEAN) return Boolean.class;
        return null;
    }

    /**
     * Get cell's nodes of class InputDec
     *
     * @return
     */

    /**
     * Set cell's fitness function value.
     *
     * @param fitnessValue value of the fitness function.
     */
    public void setFitnessValue(double fitnessValue) {
        this.fitnessValue = fitnessValue;
    }

    /**
     * Get cell's nodes
     *
     * @return
     */
    public ArrayList<Node> getNodes() {
        return nodes;
    }

    /**
     * Set cell's nodes.
     *
     * @param nodes
     */
    public void setNodes(ArrayList<Node> nodes) {
        this.nodes = nodes;
    }

    /**
     * Get cell's conectivity.
     *
     * @return
     */
    public ArrayList<Edge> getEdges() {
        return edges;
    }

    /**
     * Set cell's conectivity.
     *
     * @param edges
     */
    public void setEdges(ArrayList<Edge> edges) {
        this.edges = edges;
    }

    public ArrayList<EdgeDec> getEdgesDec() {
        return edgesDec;
    }

    public void setEdgesDec(ArrayList<EdgeDec> edgesDec) {
        this.edgesDec = edgesDec;
    }

    public ArrayList<EdgeInt> getEdgesInt() {
        return edgesInt;
    }

    public void setEdgesInt(ArrayList<EdgeInt> edgesInt) {
        this.edgesInt = edgesInt;
    }

    public ArrayList<EdgeBin> getEdgesBin() {
        return edgesBin;
    }

    public void setEdgesBin(ArrayList<EdgeBin> edgesBin) {
        this.edgesBin = edgesBin;
    }

    public ArrayList<? extends Edge> getEdges(Signal type) throws Exception {
        switch (type) {
            case DECIMAL:
                return edgesDec;
            case INTEGER:
                return edgesInt;
            case BOOLEAN:
                return edgesBin;
            default:
                return new ArrayList<>();
        }
    }
    /**
     * AddDec node to the cell.
     *
     * @param node
     */

    public void addNode(Node node) throws Exception {
        if (nodes.size() < maxNodes) {
            nodes.add(node);
            if (node instanceof INodeInputDec) {
                inpNodesDec.add((INodeInputDec) node);
                return;
            }
            if (node instanceof INodeInputInt) {
                inpNodesInt.add((INodeInputInt) node);
                return;
            }
            if (node instanceof INodeInputBin) {
                inpNodesBin.add((INodeInputBin) node);
                return;
            }
            if (node instanceof INodeOutputDec) {
                outNodesDec.add((INodeOutputDec) node);
                return;
            }
            if (node instanceof INodeOutputInt) {
                outNodesInt.add((INodeOutputInt) node);
                return;
            }
            if (node instanceof INodeOutputBin) {
                outNodesBin.add((INodeOutputBin) node);
                return;
            }
            innerNodes.add(node);
        } else throw new Exception("Maximum number of nodes exceeded");
    }

    public ArrayList<Node> getInnerNodes() {
        return innerNodes;
    }

    /**
     * AddDec edge to the cell.
     *
     * @param edge
     */
    public void addEdge(Edge edge) throws Exception {
        if (edge instanceof EdgeDec) {
            if (!edgesDec.contains(edge)) {
                ((EdgeDec) edge).getSource().addEdge((EdgeDec) edge);
                return;
            } else throw new Exception("Edge to be added already contained");
        }
        if (edge instanceof EdgeInt) {
            if (!edgesInt.contains(edge)) {
                ((EdgeInt) edge).getSource().addEdge((EdgeInt) edge);
                return;
            } else throw new Exception("Edge to be added already contained");
        }
        if (edge instanceof EdgeBin) {
            if (!edgesBin.contains(edge)) {
                ((EdgeBin) edge).getSource().addEdge((EdgeBin) edge);
                return;
            } else throw new Exception("Edge to be added already contained");
        }
        throw new Exception("Edge object not valid");
    }

    /**
     * Remove edge from the cell.
     *
     * @param edge Edge to be removed.
     */
    public void removeEdge(Edge edge) throws Exception {
        if (edges.contains(edge)) {
            edges.remove(edge);
            edge.getSource().removeEdge(edge);
            edge.getTarget().removeEdge(edge);
        }
    }

    public ArrayList<Node> getInputNodes() {
        return inputNodes;
    }

    public void setInputNodes(ArrayList<Node> inputNodes) {
        this.inputNodes = inputNodes;
    }

    public ArrayList<Node> getOutputNodes() {
        return outputNodes;
    }

    public void setOutputNodes(ArrayList<Node> outputNodes) {
        this.outputNodes = outputNodes;
    }

    public ArrayList<INodeInputDec> getInpNodesDec() {
        return inpNodesDec;
    }

    public ArrayList<INodeInputInt> getInpNodesInt() {
        return inpNodesInt;
    }

    public ArrayList<INodeInputBin> getInpNodesBin() {
        return inpNodesBin;
    }

    public ArrayList<INodeOutputDec> getOutNodesDec() {
        return outNodesDec;
    }

    public ArrayList<INodeOutputInt> getOutNodesInt() {
        return outNodesInt;
    }

    public ArrayList<INodeOutputBin> getOutNodesBin() {
        return outNodesBin;
    }

    /**
     * Remove the node with it's input conectivity from the cell and return nodes output edge's that has been left unconnected.
     * <p>
     * When removing node from the cell output conectivity won't be removed because they represent other nodes input conectivity. The will be left floating and they must be properly reconnected to other nodes in order for the cell to proper work.
     *
     * @param node Node to be removed.
     * @return Unconnected output conectivity of the node that has been removed.
     */
    public void removeNodeSafe(Node node) throws Exception {
        //Remove input Edges of the node from collectors
        for (Collector collector : node.getCollectorsTarget()) {
            collector.removeEdges();
        }
        for (Collector collector : node.getCollectorsSourceDec()) {
            collector.removeEdges();
        }
        //Remove node from the cell
        nodes.remove(node);
        innerNodes.remove(node);
    }

    public void removeNode(Node node) throws Exception {
        //Remove node from the cell
        nodes.remove(node);
        innerNodes.remove(node);
    }

    public void runEvent(double[] signalsDec, int[] signalsInt, boolean[] signalsBin) throws Exception {
        importSignals(signalsDec, signalsInt, signalsBin);
        runEvent();
    }

    public void runEvent() throws Exception {
        clcCell();
    }

    public void importSignals(double[] signalsDec, int[] signalsInt, boolean[] signalsBin) throws Exception {
        if (signalsDec.length == inpNodesDec.size()) {
            for (int i = 0; i < inpNodesDec.size(); i++) {
                inpNodesDec.get(i).importSignal(signalsDec[i]);
            }
        } else throw new Exception("Input array of doubles not equal to the number of input decimal nodes");
        if (signalsInt.length == inpNodesInt.size()) {
            for (int i = 0; i < inpNodesInt.size(); i++) {
                inpNodesInt.get(i).importSignal(signalsInt[i]);
            }
        } else throw new Exception("Input array of integers not equal to the number of input integer nodes");
        if (signalsBin.length == inpNodesBin.size()) {
            for (int i = 0; i < inpNodesBin.size(); i++) {
                inpNodesBin.get(i).importSignal(signalsBin[i]);
            }
        } else throw new Exception("Input array of booleans not equal to the number of input binary nodes");
    }

    public void exportSignals() {
        exportsDec = new double[getOutNodesDec().size()];
        for (int i = 0; i < getOutNodesDec().size(); i++) {
            exportsDec[i] = getOutNodesDec().get(i).exportSignal();
        }
        exportsInt = new int[getOutNodesInt().size()];
        for (int i = 0; i < getOutNodesInt().size(); i++) {
            exportsInt[i] = getOutNodesInt().get(i).exportSignal();
        }
        exportsBin = new boolean[getOutNodesBin().size()];
        for (int i = 0; i < getOutNodesBin().size(); i++) {
            exportsBin[i] = getOutNodesBin().get(i).exportSignal();
        }
    }

    /**
     * Execute calculation process of data transition trough nodes and connectivity of the cell.
     */
    private void clcCell() throws Exception {
        while (!isCellClcDone()) {
            for (Node node : nodes) {
                node.processNode();
            }
        }
    }

    private boolean isCellClcDone() {
        for (Edge edge : getEdges()) {
            if (!edge.isSignalTransmitted() && edge.getSource().isSignalReady()) {
                return false;
            }
        }
        return true;
    }

    public double[] getExportedValuesDec() {
        return exportsDec;
    }

    public int[] getExportedValuesInt() {
        return exportsInt;
    }

    public boolean[] getExportedValuesBin() {
        return exportsBin;
    }

    /**
     * @param node
     * @param newNode
     * @return
     */
    public int replaceNode(Node node, Node newNode) {
        int i = nodes.indexOf(node);
        if (i > -1) {
            nodes.set(i, newNode);
        }
        return i;
    }

    /**
     * Reset cell's nodes.
     */
    private void rstCell() {
        //Reset all nodes
        for (Node node : nodes) {
            node.rstNode();
        }
    }

    /**
     * Clear cell's nodes.
     */
    private void clearCell() {
        for (Node node : nodes) {
            node.clearNode();
        }
    }

    public Cell clone() {
        Cloner cloner = new Cloner();
        return cloner.deepClone(this);
    }

    public ArrayList<ArrayList<Node>> getGroup(Node nodeStart, int maxNum) throws Exception {
        ArrayList<Node> nodes = getInnerNodes();
        ArrayList<ArrayList<Node>> group = new ArrayList<>();
        if (nodes.size() < maxNum) maxNum = nodes.size();
        if (!nodes.contains(nodeStart)) throw new Exception("Initial node not member of cell");
        ArrayList<Node> currentLevel = new ArrayList<>();
        currentLevel.add(nodeStart);
        group.add(currentLevel);
        ArrayList<Node> nextLevel = new ArrayList<>();
        while (countGroup(group) < maxNum) {
            if (currentLevel.size() == 0) {
                break;
            }
            for (Node node : currentLevel) {
                for (CollectorTarget collectorInp : node.getCollectorsTargetConnected()) {
                    Node candidate = collectorInp.getEdges().get(0).getSource().getNodeAttached();
                    if (!containsNodeGroup(group, candidate) && nodes.contains(candidate) && !nextLevel.contains(candidate))
                        nextLevel.add(candidate);
                }
                for (CollectorSource collectorOut : node.getCollectorsSourceConnected()) {
                    for (Edge edge : collectorOut.getEdges()) {
                        Node candidate = edge.getTarget().getNodeAttached();
                        if (!containsNodeGroup(group, candidate) && nodes.contains(candidate) && !nextLevel.contains(candidate))
                            nextLevel.add(candidate);
                    }
                }
            }
            Collections.shuffle(nextLevel);
            int remain = maxNum - countGroup(group);
            if (remain == 0) break;
            if (remain >= nextLevel.size()) {
                group.add(nextLevel);
            } else {
                group.add(new ArrayList<>(nextLevel.subList(0, remain)));
            }

            currentLevel = nextLevel;
            nextLevel = new ArrayList<>();
        }
        return group;
    }

    public HashMap<String, ArrayList<Edge>> getGroupEdgesInner(ArrayList<ArrayList<Node>> group) {
        ArrayList<Edge> edgesBin = new ArrayList<>();
        ArrayList<Edge> edgesInt = new ArrayList<>();
        ArrayList<Edge> edgesDec = new ArrayList<>();

        for (ArrayList<Node> levels : group) {
            for (Node node : levels) {
                for (CollectorTarget collectorInp : node.getCollectorsTargetConnected(Signal.DECIMAL)) {
                    if (containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached())) {
                        edgesDec.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorTarget collectorInp : node.getCollectorsTargetConnected(Signal.INTEGER)) {
                    if (containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached())) {
                        edgesInt.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorTarget collectorInp : node.getCollectorsTargetConnected(Signal.BOOLEAN)) {
                    if (containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached())) {
                        edgesBin.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorSource collectorOut : node.getCollectorsSourceConnected(Signal.DECIMAL)) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (containsNodeGroup(group, edge.getTarget().getNodeAttached())) {
                            edgesDec.add(edge);
                        }
                    }
                }
                for (CollectorSource collectorOut : node.getCollectorsSourceConnected(Signal.INTEGER)) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (containsNodeGroup(group, edge.getTarget().getNodeAttached())) {
                            edgesInt.add(edge);
                        }
                    }
                }
                for (CollectorSource collectorOut : node.getCollectorsSourceConnected(Signal.BOOLEAN)) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (containsNodeGroup(group, edge.getTarget().getNodeAttached())) {
                            edgesBin.add(edge);
                        }
                    }
                }
            }
        }
        HashMap<String, ArrayList<Edge>> out = new HashMap<>();
        out.put("edgesDec", edgesDec);
        out.put("edgesInt", edgesInt);
        out.put("edgesBin", edgesBin);
        return out;
    }

    public HashMap<String, ArrayList<Edge>> getGroupEdgesBorder(ArrayList<ArrayList<Node>> group) {
        ArrayList<Edge> edgesInpBin = new ArrayList<>();
        ArrayList<Edge> edgesInpInt = new ArrayList<>();
        ArrayList<Edge> edgesInpDec = new ArrayList<>();
        ArrayList<Edge> edgesOutBin = new ArrayList<>();
        ArrayList<Edge> edgesOutInt = new ArrayList<>();
        ArrayList<Edge> edgesOutDec = new ArrayList<>();

        for (ArrayList<Node> levels : group) {
            for (Node node : levels) {
                for (CollectorTarget collectorInp : node.getCollectorsTargetConnected(Signal.DECIMAL)) {
                    if (!containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached())) {
                        edgesInpDec.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorTarget collectorInp : node.getCollectorsTargetConnected(Signal.INTEGER)) {
                    if (!containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached())) {
                        edgesInpInt.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorTarget collectorInp : node.getCollectorsTargetConnected(Signal.BOOLEAN)) {
                    if (!containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached())) {
                        edgesInpBin.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorSource collectorOut : node.getCollectorsSourceConnected(Signal.DECIMAL)) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (!containsNodeGroup(group, edge.getTarget().getNodeAttached())) {
                            edgesOutDec.add(edge);
                        }
                    }
                }
                for (CollectorSource collectorOut : node.getCollectorsSourceConnected(Signal.INTEGER)) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (!containsNodeGroup(group, edge.getTarget().getNodeAttached())) {
                            edgesOutInt.add(edge);
                        }
                    }
                }
                for (CollectorSource collectorOut : node.getCollectorsSourceConnected(Signal.BOOLEAN)) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (!containsNodeGroup(group, edge.getTarget().getNodeAttached())) {
                            edgesOutBin.add(edge);
                        }
                    }
                }
            }
        }
        HashMap<String, ArrayList<Edge>> out = new HashMap<>();
        out.put("edgesInpDec", edgesInpDec);
        out.put("edgesInpInt", edgesInpInt);
        out.put("edgesInpBin", edgesInpBin);
        out.put("edgesOutDec", edgesOutDec);
        out.put("edgesOutInt", edgesOutInt);
        out.put("edgesOutBin", edgesOutBin);
        return out;
    }

    public ArrayList<String> checkCell() {
        ArrayList<String> out = new ArrayList<>();
        //Check edges
        for (Edge edge : getEdges()) {
            //Check edge connection and sources
            if (!edge.getSource().getEdges().contains(edge))
                out.add("Output collector:" + edge.getSource().toString() + " not connected to the source of edge: " + edge.toString());
            if (!edge.getTarget().getEdges().contains(edge))
                out.add("Input collector:" + edge.getSource().toString() + " not connected to the target of edge: " + edge.toString());
        }
        return out;
    }

    public enum Signal {DECIMAL, INTEGER, BOOLEAN}

}

