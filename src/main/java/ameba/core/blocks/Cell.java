package ameba.core.blocks;


import ameba.core.blocks.nodes.INodeInput;
import ameba.core.blocks.nodes.INodeOutput;
import ameba.core.blocks.nodes.IntegralBin;
import ameba.core.blocks.nodes.Node;
import com.rits.cloning.Cloner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Cell {
    /**
     * Fitness value of last cell simulation run.
     */
    private Double fitnessValue;
    /**
     * List of nodes without InputDec and OutputDec type.
     */
    private ArrayList<Node> nodes;
    private ArrayList<Node> innerNodes;
    /**
     * List of input nodes interfaces.
     */
    private ArrayList<INodeInput> inpNodes;
    /**
     * List of input nodes.
     */
    private ArrayList<INodeOutput> outNodes;

    private ArrayList<Edge> edges;

    private int maxNodes;

    private ArrayList<Signal> importedValues;
    private ArrayList<Signal> exportedValues;


    /**
     *
     */
    public Cell(int maxNodes) {
        fitnessValue = 0.0;
        nodes = new ArrayList<>();
        innerNodes = new ArrayList<>();
        inpNodes = new ArrayList<>();
        outNodes = new ArrayList<>();
        edges = new ArrayList<>();
        this.maxNodes = maxNodes;
        importedValues = new ArrayList<>();
        exportedValues = new ArrayList<>();
    }

    /**
     * Set cell's fitness function value.
     *
     * @param fitnessValue value of the fitness function.
     */
    public void setFitnessValue(Double fitnessValue) {
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
     * Get cell's nodes of class InputDec
     *
     * @return
     */


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

    public ArrayList<Edge> getEdges(Class type) {
        ArrayList<Edge> edges = new ArrayList<>();
        for (Edge edge : getEdges()) {
            if (edge.getType().equals(type)) {
                edges.add(edge);
            }
        }
        return edges;
    }

    /**
     * AddDec node to the cell.
     *
     * @param node
     */
    public void addNode(Node node) throws Exception {
        if (nodes.size() < maxNodes) {
            nodes.add(node);
            if (node instanceof INodeInput) {
                inpNodes.add((INodeInput) node);
                return;
            }
            if (node instanceof INodeOutput) {
                outNodes.add((INodeOutput) node);
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
        edges.add(edge);
        edge.getSource().addEdge(edge);
        edge.getTarget().addEdge(edge);
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


    /**
     * Get nodes of type InputDec.
     *
     * @return
     */
    public ArrayList<INodeInput> getInputs() {
        return inpNodes;
    }

    /**
     * Get nodes of type OutputDec.
     *
     * @return
     */
    public ArrayList<INodeOutput> getOutputs() {
        return outNodes;
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
        for (Collector collector : node.getInpCollectors()) {
            collector.removeEdges();
        }
        for (Collector collector : node.getOutCollectorsDec()) {
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


    public void runEvent(ArrayList<Signal> values) throws Exception {
        importSignals(values);
        clcCell();
    }

    public ArrayList<ArrayList<Signal>> run(ArrayList<ArrayList<Signal>> inputs) throws Exception {
        clearCell();
        ArrayList<ArrayList<Signal>> out = new ArrayList<>();
        for (ArrayList<Signal> data : inputs) {
            rstCell();
            runEvent(data);
            out.add((ArrayList<Signal>) exportedValues.clone());
        }
        return out;
    }


    private void importSignals(ArrayList<Signal> values) throws Exception {
        if (values.size() == inpNodes.size()) {
            importedValues = values;
            for (int i = 0; i < values.size(); i++) {
                inpNodes.get(i).importSignal(values.get(i));
            }

        } else throw new Exception("InputDec array not equal to the number of input nodes");
    }

    /**
     * Execute calculation process of data transition trough nodes and connectivity of the cell.
     */
    private void clcCell() throws Exception {
        int sumStateOld;
        int sumState = 0;
        while (!isNodesEndState()) {
            sumStateOld = sumState;
            sumState = 0;
            for (Node node : nodes) {
                node.processNode();
                sumState += node.getState();
            }
            if (sumStateOld == sumState) {
                break;
            }
        }
        exportedValues.clear();
        for (INodeOutput node : outNodes) {
            exportedValues.add(node.exportSignal());
        }
    }

    public ArrayList<Signal> getExportedValues() {
        return exportedValues;
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

    public boolean isNodesEndState() {
        for (Node node : nodes) {
            if (node.getState() != 4) {
                return false;
            }
        }
        return true;
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
                for (CollectorInp collectorInp : node.getInpCollectorsConnected()) {
                    Node candidate = collectorInp.getEdges().get(0).getSource().getNodeAttached();
                    if (!containsNodeGroup(group, candidate) && nodes.contains(candidate) && !nextLevel.contains(candidate))
                        nextLevel.add(candidate);
                }
                for (CollectorOut collectorOut : node.getOutCollectorsConnected()) {
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

    public HashMap<String, ArrayList<Edge>> getGroupBorderEdges(ArrayList<ArrayList<Node>> group) {
        ArrayList<Edge> edgesInpBin = new ArrayList<>();
        ArrayList<Edge> edgesInpInt = new ArrayList<>();
        ArrayList<Edge> edgesInpDec = new ArrayList<>();
        ArrayList<Edge> edgesOutBin = new ArrayList<>();
        ArrayList<Edge> edgesOutInt = new ArrayList<>();
        ArrayList<Edge> edgesOutDec = new ArrayList<>();

        for (ArrayList<Node> levels : group) {
            for (Node node : levels) {
                for (CollectorInp collectorInp : node.getInpCollectorsConnected(Double.class)) {
                    if (!containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached())) {
                        edgesInpDec.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorInp collectorInp : node.getInpCollectorsConnected(Integer.class)) {
                    if (!containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached())) {
                        edgesInpInt.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorInp collectorInp : node.getInpCollectorsConnected(Boolean.class)) {
                    if (!containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached())) {
                        edgesInpBin.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorOut collectorOut : node.getOutCollectorsConnected(Double.class)) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (!containsNodeGroup(group, edge.getTarget().getNodeAttached())) {
                            edgesOutDec.add(edge);
                        }
                    }
                }
                for (CollectorOut collectorOut : node.getOutCollectorsConnected(Integer.class)) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (!containsNodeGroup(group, edge.getTarget().getNodeAttached())) {
                            edgesOutInt.add(edge);
                        }
                    }
                }
                for (CollectorOut collectorOut : node.getOutCollectorsConnected(Boolean.class)) {
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

    private int countGroup(ArrayList<ArrayList<Node>> group) {
        int size = 0;
        for (ArrayList<Node> list : group) {
            size += list.size();
        }
        return size;
    }

    private boolean containsNodeGroup(ArrayList<ArrayList<Node>> group, Node node) {
        for (ArrayList<Node> list : group) {
            if (list.contains(node)) return true;
        }
        return false;
    }

    public String checkCell() {
        //Check edges
        for (Edge edge : getEdges()) {
            //Check edges types and sources
            if (!edge.getSource().getType().equals(edge.getWeight().gettClass())) {
                return "Node: " + edge.getSource().getNodeAttached().getClass().getSimpleName() + " out collector of type: " + edge.getSource().getType().getSimpleName() + " not matched with edge weight type: " + edge.getWeight().gettClass().getSimpleName();
            }
            //Check edges types and targets
            if (!edge.getTarget().getType().equals(edge.getWeight().gettClass())) {
                return "Node: " + edge.getTarget().getNodeAttached().getClass().getSimpleName() + " inp collector of type: " + edge.getTarget().getType().getSimpleName() + " not matched with edge weight type: " + edge.getWeight().gettClass().getSimpleName();
            }
            //Check edge connection and sources
            if (!edge.getSource().getEdges().contains(edge))
                return "Output collector:" + edge.getSource().getClass().getSimpleName() + " not connected to the source of edge: " + edge.getClass().getSimpleName();
            if (!edge.getTarget().getEdges().contains(edge))
                return "Input collector:" + edge.getSource().getClass().getSimpleName() + " not connected to the target of edge: " + edge.getClass().getSimpleName();

        }
        return "";
    }


}

