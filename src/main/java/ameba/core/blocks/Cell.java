package ameba.core.blocks;


import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.InputDec;
import ameba.core.blocks.nodes.Node;
import ameba.core.blocks.nodes.OutputDec;
import com.rits.cloning.Cloner;

import java.util.ArrayList;

public class Cell {
    /**
     * Fitness value of last cell simulation run.
     */
    private Double fitnessValue;
    /**
     * List of nodes without InputDec and OutputDec type.
     */
    private ArrayList<Node> nodes;
    /**
     * List of input nodes.
     */
    private ArrayList<InputDec> inpNodes;
    /**
     * List of output nodes.
     */
    private ArrayList<OutputDec> outNodes;
    /**
     * List of inpEdges.
     */
    private ArrayList<Edge> edges;

    /**
     *
     */
    public Cell() {
        fitnessValue=0.0;
        nodes = new ArrayList<Node>();
        inpNodes = new ArrayList<InputDec>();
        outNodes = new ArrayList<OutputDec>();
        edges = new ArrayList<Edge>();
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
    public ArrayList<InputDec> getInputNodes() {
        return inpNodes;
    }

    /**
     * Set cell's input nodes.
     *
     * @param inpNodes
     */
    public void setInputNodes(ArrayList<InputDec> inpNodes) {
        this.inpNodes = inpNodes;
    }

    /**
     * Get cell's output nodes.
     *
     * @return
     */
    public ArrayList<OutputDec> getOutputNodes() {
        return outNodes;
    }

    /**
     * Set cell's output nodes.
     *
     * @param outNodes
     */
    public void setOutputNodes(ArrayList<OutputDec> outNodes) {
        this.outNodes = outNodes;
    }

    /**
     * Get cell's inpEdges.
     *
     * @return
     */
    public ArrayList<Edge> getEdges() {
        return edges;
    }

    /**
     * Set cell's inpEdges.
     *
     * @param edges
     */
    public void setEdges(ArrayList<Edge> edges) {
        this.edges = edges;
    }

    /**
     * Add node to the cell.
     *
     * @param node
     */
    public void addNode(Node node) {
        if (node instanceof InputDec) {
            inpNodes.add((InputDec) node);
            return;
        }
        if (node instanceof OutputDec) {
            outNodes.add((OutputDec) node);
            return;
        }
        nodes.add(node);
    }


    /**
     * Add edge to the cell.
     *
     * @param edge
     */
    public void addEdge(Edge edge) {
        edges.add(edge);
        edge.getSource().addOutputEdge(edge);
        edge.getTarget().addEdgeInput(edge);
    }


    /**
     * Remove edge from the cell.
     *
     * @param edge Edge to be removed.
     */
    public void removeEdge(Edge edge) {
        if (edges.contains(edge)) {
            edges.remove(edge);
            edge.getSource().removeOutputEdge(edge);
            edge.getTarget().removeInputEdge(edge);
        }
    }


    /**
     * Get nodes of type InputDec.
     *
     * @return
     */
    public ArrayList<InputDec> getInputs() {
        return inpNodes;
    }

    /**
     * Get nodes of type OutputDec.
     *
     * @return
     */
    public ArrayList<OutputDec> getOutputs() {
        return outNodes;
    }

    /**
     * Remove the node with it's input inpEdges from the cell and return nodes output edge's that has been left unconnected.
     * <p>
     * When removing node from the cell output inpEdges won't be removed because they represent other nodes input inpEdges. The will be left floating and they must be properly reconnected to other nodes in order for the cell to proper work.
     *
     * @param node Node to be removed.
     * @return Unconnected output inpEdges of the node that has been removed.
     */
    public ArrayList<Edge> removeNode(Node node) {
        //Remove input inpEdges of the node
        for (Edge edge : node.getInputEdges()) {
            removeEdge(edge);
        }
        //Disconnect output inpEdges of the node
        for (Edge edge : node.getOutputEdges()) {
            edge.setSource(null);
        }
        //Remove node from the cell
        nodes.remove(node);
        return node.getOutputEdges();
    }

    /**
     * Calculate cell output data based on the provided input data for one discrete time event.
     *
     * @param inpData InputDec data to be mapped trough cell.
     * @return Mapped data.
     */
    public double[] runEvent(double[] inpData) {
        setInpValues(inpData);
        return clcCell();
    }

    /**
     * Calculate cell output Stream of data based on the provided input Stream of data.
     *
     * @param inpData InputDec Stream of data to be mapped trough cell where rows represents data series and columns represents inputs to cell.
     * @return Mapped Stream data.
     */
    public double[][] run(double[][] inpData) {
        clearCell();
        double[][] outData = new double[inpData.length][outNodes.size()];
        for (int i = 0; i < inpData.length; i++) {
            rstCell();
            outData[i]=(runEvent(inpData[i]));
        }
        return outData;
    }

    /**
     * Set signal values of InputDec nodes.
     *
     * @param inp InputDec data.
     */
    private void setInpValues(double[] inp) {
        for (int i = 0; i < inpNodes.size(); i++) {
            inpNodes.get(i).importSignal(inp[i]);
        }
    }

    /**
     * Execute calculation process of data transition trough nodes and inpEdges of the cell.
     */
    private double[] clcCell() {
        int nmbClcCell = 0;
        while (true) {
            nmbClcCell++;
            int nmbClcNode = 0;
            for (Node node : nodes) {
                node.clcNode();
                if (node.isSignalReady()) {
                    nmbClcNode++;
                }
            }
            if (nmbClcNode >= this.nodes.size() || nmbClcCell > this.nodes.size()) {
                break;
            }
        }
        double[] out = new double[outNodes.size()];
        for (int i = 0; i < outNodes.size(); i++) {
            out[i] = outNodes.get(i).getSignal();
        }
        return out;
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
        //Reset all inpEdges
        for (Edge edge : edges) {
            edge.rstEdge();
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
}

