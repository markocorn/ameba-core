package ameba.core.blocks;


import ameba.core.blocks.collectors.Collector;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeBin;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.blocks.nodes.*;
import ameba.core.blocks.nodes.types.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rits.cloning.Cloner;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Cell implements Serializable {
    public String lastRep = "";
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

    private int maxNodes;

    /**
     *
     */
    public Cell(int maxNodes) {
        fitnessValue = 0.0;
        nodes = new ArrayList<>();
        inpNodesDec = new ArrayList<>();
        inpNodesInt = new ArrayList<>();
        inpNodesBin = new ArrayList<>();
        outNodesDec = new ArrayList<>();
        outNodesInt = new ArrayList<>();
        outNodesBin = new ArrayList<>();
        edges = new ArrayList<>();
        this.maxNodes = maxNodes;

        double[][] outDataDec = new double[][]{{0.0}};
        int[][] outDataInt = new int[][]{{1}};
        boolean[][] outDataBin = new boolean[][]{{false}};
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

    public ArrayList<Edge> getEdgesUnlockedWeight() {
        ArrayList<Edge> out = new ArrayList<>();
        for (Edge edge : edges) {
            if (!edge.isLockWeight()) {
                out.add(edge);
            }
        }
        return out;
    }

    public ArrayList<Edge> getEdgesUnlockedSource() {
        ArrayList<Edge> out = new ArrayList<>();
        for (Edge edge : edges) {
            if (!edge.isLockSource()) {
                out.add(edge);
            }
        }
        return out;
    }

    public ArrayList<Edge> getEdgesUnlockedTarget() {
        ArrayList<Edge> out = new ArrayList<>();
        for (Edge edge : edges) {
            if (!edge.isLockTarget()) {
                out.add(edge);
            }
        }
        return out;
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
        ArrayList<EdgeDec> edge1 = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge instanceof EdgeDec) {
                edge1.add((EdgeDec) edge);
            }
        }
        return edge1;
    }

    public ArrayList<EdgeDec> getEdgesUnlockedSourceDec() {
        ArrayList<EdgeDec> edge1 = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge instanceof EdgeDec && !edge.isLockSource()) {
                edge1.add((EdgeDec) edge);
            }
        }
        return edge1;
    }

    public ArrayList<EdgeDec> getEdgesUnlockedTargetDec() {
        ArrayList<EdgeDec> edge1 = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge instanceof EdgeDec && !edge.isLockTarget()) {
                edge1.add((EdgeDec) edge);
            }
        }
        return edge1;
    }

    public ArrayList<EdgeDec> getEdgesUnlockedWeightDec() {
        ArrayList<EdgeDec> edge1 = new ArrayList<>();
        for (Edge edge : getEdgesUnlockedWeight()) {
            if (edge instanceof EdgeDec) {
                edge1.add((EdgeDec) edge);
            }
        }
        return edge1;
    }

    public ArrayList<EdgeDec> getEdgesDec(Edge edge) {
        ArrayList<EdgeDec> edges1 = getEdgesDec();
        edges1.remove(edge);
        return edges1;
    }

    public ArrayList<EdgeDec> getEdgesUnlockedSourceDec(Edge edge) {
        ArrayList<EdgeDec> edges1 = getEdgesUnlockedSourceDec();
        edges1.remove(edge);
        return edges1;
    }

    public ArrayList<EdgeDec> getEdgesUnlockedTargetDec(Edge edge) {
        ArrayList<EdgeDec> edges1 = getEdgesUnlockedTargetDec();
        edges1.remove(edge);
        return edges1;
    }

    public ArrayList<EdgeDec> getEdgesUnlockedWeightDec(Edge edge) {
        ArrayList<EdgeDec> edges1 = getEdgesUnlockedWeightDec();
        edges1.remove(edge);
        return edges1;
    }


    public ArrayList<EdgeInt> getEdgesInt() {
        ArrayList<EdgeInt> edges1 = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge instanceof EdgeInt) {
                edges1.add((EdgeInt) edge);
            }
        }
        return edges1;
    }

    public ArrayList<EdgeInt> getEdgesUnlockedSourceInt() {
        ArrayList<EdgeInt> edges1 = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge instanceof EdgeInt && !edge.isLockSource()) {
                edges1.add((EdgeInt) edge);
            }
        }
        return edges1;
    }

    public ArrayList<EdgeInt> getEdgesUnlockedTargetInt() {
        ArrayList<EdgeInt> edges1 = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge instanceof EdgeInt && !edge.isLockTarget()) {
                edges1.add((EdgeInt) edge);
            }
        }
        return edges1;
    }

    public ArrayList<EdgeInt> getEdgesUnlockedWeightInt() {
        ArrayList<EdgeInt> edges1 = new ArrayList<>();
        for (Edge edge : getEdgesUnlockedWeight()) {
            if (edge instanceof EdgeInt) {
                edges1.add((EdgeInt) edge);
            }
        }
        return edges1;
    }

    public ArrayList<EdgeInt> getEdgesInt(Edge edge) {
        ArrayList<EdgeInt> edges1 = getEdgesInt();
        edges1.remove(edge);
        return edges1;
    }

    public ArrayList<EdgeInt> getEdgesUnlockedSourceInt(Edge edge) {
        ArrayList<EdgeInt> edges1 = getEdgesUnlockedSourceInt();
        edges1.remove(edge);
        return edges1;
    }

    public ArrayList<EdgeInt> getEdgesUnlockedTargetInt(Edge edge) {
        ArrayList<EdgeInt> edges1 = getEdgesUnlockedTargetInt();
        edges1.remove(edge);
        return edges1;
    }

    public ArrayList<EdgeInt> getEdgesUnlockedWeightInt(Edge edge) {
        ArrayList<EdgeInt> edges1 = getEdgesUnlockedWeightInt();
        edges1.remove(edge);
        return edges1;
    }

    public ArrayList<EdgeBin> getEdgesBin() {
        ArrayList<EdgeBin> edge1 = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge instanceof EdgeBin) {
                edge1.add((EdgeBin) edge);
            }
        }
        return edge1;
    }

    public ArrayList<EdgeBin> getEdgesUnlockedSourceBin() {
        ArrayList<EdgeBin> edge1 = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge instanceof EdgeBin && !edge.isLockSource()) {
                edge1.add((EdgeBin) edge);
            }
        }
        return edge1;
    }

    public ArrayList<EdgeBin> getEdgesUnlockedTargetBin() {
        ArrayList<EdgeBin> edge1 = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge instanceof EdgeBin && !edge.isLockTarget()) {
                edge1.add((EdgeBin) edge);
            }
        }
        return edge1;
    }

    public ArrayList<EdgeBin> getEdgesUnlockedWeightBin() {
        ArrayList<EdgeBin> edge1 = new ArrayList<>();
        for (Edge edge : getEdgesUnlockedWeight()) {
            if (edge instanceof EdgeBin) {
                edge1.add((EdgeBin) edge);
            }
        }
        return edge1;
    }

    public ArrayList<EdgeBin> getEdgesBin(Edge edge) {
        ArrayList<EdgeBin> edges1 = getEdgesBin();
        edges1.remove(edge);
        return edges1;
    }

    public ArrayList<EdgeBin> getEdgesUnlockedSourceBin(Edge edge) {
        ArrayList<EdgeBin> edges1 = getEdgesUnlockedSourceBin();
        edges1.remove(edge);
        return edges1;
    }

    public ArrayList<EdgeBin> getEdgesUnlockedTargetBin(Edge edge) {
        ArrayList<EdgeBin> edges1 = getEdgesUnlockedTargetBin();
        edges1.remove(edge);
        return edges1;
    }

    public ArrayList<EdgeBin> getEdgesUnlockedWeightBin(Edge edge) {
        ArrayList<EdgeBin> edges1 = getEdgesUnlockedWeightBin();
        edges1.remove(edge);
        return edges1;
    }

    public ArrayList<? extends Edge> getEdges(Signal type) {
        switch (type) {
            case DECIMAL:
                return getEdgesDec();
            case INTEGER:
                return getEdgesInt();
            case BOOLEAN:
                return getEdgesBin();
            default:
                return new ArrayList<>();
        }
    }

    public ArrayList<? extends Edge> getEdgesUnlockedWeight(Signal type) {
        switch (type) {
            case DECIMAL:
                return getEdgesUnlockedWeightDec();
            case INTEGER:
                return getEdgesUnlockedWeightInt();
            case BOOLEAN:
                return getEdgesUnlockedWeightBin();
            default:
                return new ArrayList<>();
        }
    }


    public ArrayList<? extends Edge> getEdges(Signal type, Edge edge) {
        switch (type) {
            case DECIMAL:
                return getEdgesDec(edge);
            case INTEGER:
                return getEdgesInt(edge);
            case BOOLEAN:
                return getEdgesBin(edge);
            default:
                return new ArrayList<>();
        }
    }

    public ArrayList<? extends Edge> getEdgesUnlockedWeight(Signal type, Edge edge) {
        switch (type) {
            case DECIMAL:
                return getEdgesUnlockedWeightDec(edge);
            case INTEGER:
                return getEdgesUnlockedWeightInt(edge);
            case BOOLEAN:
                return getEdgesUnlockedWeightBin(edge);
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
        } else throw new Exception("Maximum number of nodes exceeded");
    }

    public ArrayList<Node> getInnerNodes() {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof InputDec || node instanceof InputInt || node instanceof InputBin || node instanceof OutputDec || node instanceof OutputInt || node instanceof OutputBin)) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }

    public ArrayList<Node> getInnerNodesUnlocked() {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof InputDec || node instanceof InputInt || node instanceof InputBin || node instanceof OutputDec || node instanceof OutputInt || node instanceof OutputBin) && !node.isLock()) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }

    public ArrayList<Node> getInnerNodesFullUnlocked() {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof InputDec || node instanceof InputInt || node instanceof InputBin || node instanceof OutputDec || node instanceof OutputInt || node instanceof OutputBin)
                    && !node.isLock()
                    && !node.hasLockedEdgesSource()
                    && !node.hasLockedEdgesTarget()) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }

    public ArrayList<Node> getInnerNodesParType(Signal type) {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof InputDec || node instanceof InputInt || node instanceof InputBin || node instanceof OutputDec || node instanceof OutputInt || node instanceof OutputBin) && node.getParTypes().contains(type)) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }

    public ArrayList<Node> getInnerNodesParTypeUnlocked(Signal type) {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof InputDec || node instanceof InputInt || node instanceof InputBin || node instanceof OutputDec || node instanceof OutputInt || node instanceof OutputBin) && node.getParTypes().contains(type) && node.hasParamsUnlocked(type)) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }


    public ArrayList<Node> getInnerNodesParType(Signal type, Node nodeNot) {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof InputDec || node instanceof InputInt || node instanceof InputBin || node instanceof OutputDec || node instanceof OutputInt || node instanceof OutputBin) && node.getParTypes().contains(type) && !node.equals(nodeNot)) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }

    public ArrayList<Node> getInnerNodesParTypeUnlocked(Signal type, Node nodeNot) {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof InputDec || node instanceof InputInt || node instanceof InputBin || node instanceof OutputDec || node instanceof OutputInt || node instanceof OutputBin) && node.getParTypes().contains(type) && !node.equals(nodeNot) && node.hasParamsUnlocked(type)) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }

    public boolean hasLockedNodes() {
        for (Node node : nodes) {
            if (node.isLock()) {
                return true;
            }
        }
        return false;
    }

    /**
     * AddDec edge to the cell.
     *
     * @param edge
     */
    public void addEdge(Edge edge) throws Exception {
        if (!edges.contains(edge)) {
            edge.getSource().addEdge(edge);
            edge.getTarget().addEdge(edge);
            edges.add(edge);
        } else throw new Exception("Edge to be added already contained");
    }

    public void addEdgeNotSafe(Edge edge) throws Exception {
        edges.add(edge);
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

    public void removeEdge(ArrayList<Edge> edges) throws Exception {
        for (Edge edge : edges) {
            removeEdge(edge);
        }
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
            collector.clearEdges();
        }
        for (Collector collector : node.getCollectorsSource()) {
            collector.clearEdges();
        }
        //Remove node from the cell
        nodes.remove(node);
    }

    public void removeNode(Node node) throws Exception {
        //Remove node from the cell
        nodes.remove(node);
    }

    public void runEvent() {
        clcCell();
        exportSignals();
    }

    public void importSignals(double[] signalsDec, int[] signalsInt, boolean[] signalsBin) {
        for (int i = 0; i < inpNodesDec.size(); i++) {
            inpNodesDec.get(i).importSignal(signalsDec[i]);
        }
        for (int i = 0; i < inpNodesInt.size(); i++) {
            inpNodesInt.get(i).importSignal(signalsInt[i]);
        }
        for (int i = 0; i < inpNodesBin.size(); i++) {
            inpNodesBin.get(i).importSignal(signalsBin[i]);
        }
    }

    public void importSignals(double[] signalsDec) {
        for (int i = 0; i < inpNodesDec.size(); i++) {
            inpNodesDec.get(i).importSignal(signalsDec[i]);
        }
    }

    public void importSignals(int[] signalsInt) {
        for (int i = 0; i < inpNodesInt.size(); i++) {
            inpNodesInt.get(i).importSignal(signalsInt[i]);
        }
    }

    public void importSignals(boolean[] signalsBin) {
        for (int i = 0; i < inpNodesBin.size(); i++) {
            inpNodesBin.get(i).importSignal(signalsBin[i]);
        }
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
    private void clcCell() {
        int n = 0;
        while (n < edges.size()) {
            n = isCellClcDone();
            for (Node node : nodes) {
                node.processNode();
            }
            if (n == isCellClcDone()) break;
        }
        rstCell();
    }

    private int isCellClcDone() {
        //n number of edges that has transmitted + not transited but they have conditions
        int n = 0;
        for (Edge edge : getEdges()) {
            if (edge.isSignalTransmitted() || edge.getSource().isSignalReady()) {
                n++;
            }
        }
        return n;
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
    public void rstCell() {
        //Reset all nodes
        for (Node node : nodes) {
            node.rstNode();
        }
        //Reset all edges
        for (Edge edge : edges) {
            edge.rstEdge();
        }
    }

    /**
     * Clear cell's nodes.
     */
    public void clearCell() {

        rstCell();
        for (Node node : nodes) {
            node.clearNode();
            node.simOptimization();
        }
    }

    public double getFitnessValue() {
        return fitnessValue;
    }

    /**
     * Set cell's fitness function value.
     *
     * @param fitnessValue value of the fitness function.
     */
    public void setFitnessValue(double fitnessValue) {
        this.fitnessValue = fitnessValue;
    }

    public void addFitnessValue(double fitnessValue) {
        this.fitnessValue += fitnessValue;
    }

    public Cell clone() {
        Cloner cloner = new Cloner();
        clearCell();
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
                    if (containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached()) && !edgesDec.contains(collectorInp.getEdges().get(0))) {
                        edgesDec.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorTarget collectorInp : node.getCollectorsTargetConnected(Signal.INTEGER)) {
                    if (containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached()) && !edgesInt.contains(collectorInp.getEdges().get(0))) {
                        edgesInt.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorTarget collectorInp : node.getCollectorsTargetConnected(Signal.BOOLEAN)) {
                    if (containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached()) && !edgesBin.contains(collectorInp.getEdges().get(0))) {
                        edgesBin.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorSource collectorOut : node.getCollectorsSourceConnected(Signal.DECIMAL)) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (containsNodeGroup(group, edge.getTarget().getNodeAttached()) && !edgesDec.contains(edge)) {
                            edgesDec.add(edge);
                        }
                    }
                }
                for (CollectorSource collectorOut : node.getCollectorsSourceConnected(Signal.INTEGER)) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (containsNodeGroup(group, edge.getTarget().getNodeAttached()) && !edgesInt.contains(edge)) {
                            edgesInt.add(edge);
                        }
                    }
                }
                for (CollectorSource collectorOut : node.getCollectorsSourceConnected(Signal.BOOLEAN)) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (containsNodeGroup(group, edge.getTarget().getNodeAttached()) && !edgesBin.contains(edge)) {
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
        //Check edges for missing connectors
        for (Edge edge : getEdges()) {
            //Check edge connection and sources
            if (!edge.getSource().getEdges().contains(edge))
                out.add("Source collector:" + edge.getSource().toString() + "of node: " + edge.getSource().getNodeAttached().toString() + " not connected to the source of edge: " + edge.toString());
            if (!edge.getTarget().getEdges().contains(edge))
                out.add("Target collector:" + edge.getTarget().toString() + "of node: " + edge.getTarget().getNodeAttached().toString() + " not connected to the target of edge: " + edge.toString());
            //Check for missing nodes
            if (!nodes.contains(edge.getSource().getNodeAttached())) {
                out.add("Node: " + edge.getSource().getNodeAttached().toString() + " is not par of the cell from edges source");
            }
            if (!nodes.contains(edge.getTarget().getNodeAttached())) {
                out.add("Node: " + edge.getTarget().getNodeAttached().toString() + " is not par of the cell from edges target");
            }
        }
        //Check for missing edges
        for (Node node : getNodes()) {
            //Check collectors sources
            for (CollectorSource source : node.getCollectorsSource()) {
                for (Edge edge : source.getEdges()) {
                    if (!getEdges().contains(edge)) {
                        out.add("Missing Edge: " + edge.toString() + "from source collector.");
                    }
                }
            }
            //Check collectors targets
            for (CollectorTarget target : node.getCollectorsTarget()) {
                for (Edge edge : target.getEdges()) {
                    if (!getEdges().contains(edge)) {
                        out.add("Missing Edge: " + edge.toString() + "from target collector.");
                    }
                }
            }
        }
        //Check for minimum connections
        for (Node node : nodes) {
            for (CollectorTarget t : node.getCollectorsTargetToConnect(Signal.DECIMAL)) {
                if (t.getEdges().size() == 0) {
                    out.add("Nodes: " + node.toString() + " target collector Dec: " + t.toString() + "not connected.");
                }
            }
            for (CollectorTarget t : node.getCollectorsTargetToConnect(Signal.INTEGER)) {
                if (t.getEdges().size() == 0) {
                    out.add("Nodes: " + node.toString() + " target collector Int: " + t.toString() + "not connected.");
                }
            }
            for (CollectorTarget t : node.getCollectorsTargetToConnect(Signal.BOOLEAN)) {
                if (t.getEdges().size() == 0) {
                    out.add("Nodes: " + node.toString() + " target collector Bin: " + t.toString() + "not connected.");
                }
            }
        }
        return out;
    }

    public void checkCellPrint() {
        ArrayList<String> error = checkCell();
        for (String s : error) {
            System.out.println(s);
        }
    }

    public void writeToFile(String filepath) throws IOException {
        FileOutputStream fout = new FileOutputStream(filepath);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(this);
    }

    public String toJsonString() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode cell = mapper.createObjectNode();
        ArrayNode nodes = cell.putArray("nodes");
        HashMap<CollectorSource, Integer> sourceId = new HashMap<>();
        HashMap<CollectorTarget, Integer> targetId = new HashMap<>();

        int ind = 0;
        for (int i = 0; i < getNodes().size(); i++) {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", ind);
            node.put("lock", getNodes().get(i).isLock());
            ind++;
            node.put("type", getNodes().get(i).getClass().getSimpleName());
            ArrayNode inpColDec = node.putArray("sourceCol");
            for (int j = 0; j < getNodes().get(i).getCollectorsSourceConnected().size(); j++) {
                inpColDec.add(mapper.createObjectNode().put("id", ind).put("type", getNodes().get(i).getCollectorsSourceConnected().get(j).getType().toString()));
                sourceId.put(getNodes().get(i).getCollectorsSourceConnected().get(j), ind);
                ind++;
            }
            ArrayNode outColDec = node.putArray("targetCol");
            for (int j = 0; j < getNodes().get(i).getCollectorsTargetConnected().size(); j++) {
                outColDec.add(mapper.createObjectNode().put("id", ind).put("type", getNodes().get(i).getCollectorsTargetConnected().get(j).getType().toString()));
                targetId.put(getNodes().get(i).getCollectorsTargetConnected().get(j), ind);
                ind++;
            }
            ArrayNode paramsDec = node.putArray("paramsDec");
            for (int j = 0; j < getNodes().get(i).getParamsDec().size(); j++) {
                paramsDec.add(getNodes().get(i).getParamsDec().get(j));
            }
            ArrayNode lockDec = node.putArray("lockDec");
            for (int j = 0; j < getNodes().get(i).getLockDec().length; j++) {
                lockDec.add(getNodes().get(i).getLockDec()[j]);
            }
            ArrayNode paramsInt = node.putArray("paramsInt");
            for (int j = 0; j < getNodes().get(i).getParamsInt().size(); j++) {
                paramsInt.add(getNodes().get(i).getParamsInt().get(j));
            }
            ArrayNode lockInt = node.putArray("lockInt");
            for (int j = 0; j < getNodes().get(i).getLockInt().length; j++) {
                lockInt.add(getNodes().get(i).getLockInt()[j]);
            }
            ArrayNode paramsBin = node.putArray("paramsBin");
            for (int j = 0; j < getNodes().get(i).getParamsBin().size(); j++) {
                paramsBin.add(getNodes().get(i).getParamsBin().get(j));
            }
            ArrayNode lockBin = node.putArray("lockBin");
            for (int j = 0; j < getNodes().get(i).getLockBin().length; j++) {
                lockBin.add(getNodes().get(i).getLockBin()[j]);
            }

            nodes.add(node);
        }

        ArrayNode edges = cell.putArray("edges");
        for (int i = 0; i < getEdges().size(); i++) {
            ObjectNode edge = mapper.createObjectNode();
            switch (getEdges().get(i).getType()) {
                case DECIMAL: {
                    edge.put("id", ind)
                            .put("type", getEdges().get(i).getType().toString())
                            .put("weight", ((EdgeDec) getEdges().get(i)).getWeight())
                            .put("sourceCol", sourceId.get(getEdges().get(i).getSource()))
                            .put("targetCol", targetId.get(getEdges().get(i).getTarget()))
                            .put("lockTarget", getEdges().get(i).isLockTarget())
                            .put("lockSource", getEdges().get(i).isLockSource())
                            .put("lockWeight", getEdges().get(i).isLockWeight());
                }
                break;
                case INTEGER: {
                    edge.put("id", ind)
                            .put("type", getEdges().get(i).getType().toString())
                            .put("weight", ((EdgeInt) getEdges().get(i)).getWeight())
                            .put("sourceCol", sourceId.get(getEdges().get(i).getSource()))
                            .put("targetCol", targetId.get(getEdges().get(i).getTarget()))
                            .put("lockTarget", getEdges().get(i).isLockTarget())
                            .put("lockSource", getEdges().get(i).isLockSource())
                            .put("lockWeight", getEdges().get(i).isLockWeight());
                }
                break;
                case BOOLEAN: {
                    edge.put("id", ind)
                            .put("type", getEdges().get(i).getType().toString())
                            .put("weight", ((EdgeBin) getEdges().get(i)).getWeight())
                            .put("sourceCol", sourceId.get(getEdges().get(i).getSource()))
                            .put("targetCol", targetId.get(getEdges().get(i).getTarget()))
                            .put("lockTarget", getEdges().get(i).isLockTarget())
                            .put("lockSource", getEdges().get(i).isLockSource())
                            .put("lockWeight", getEdges().get(i).isLockWeight());
                }
                break;
            }
            edges.add(edge);
            ind++;
        }
        ObjectNode out = mapper.createObjectNode();
        out.put("cell", cell);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out);
    }

    public enum Signal {DECIMAL, INTEGER, BOOLEAN}

}

