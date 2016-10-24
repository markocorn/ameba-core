package blocks;


import blocks.nodes.Input;
import blocks.nodes.Output;

import java.util.ArrayList;

public class Cell implements Cloneable {
    /**
     * Fitness value of last cell simulation run.
     */
    private Double fitnessValue;
    /**
     * List of nodes without Input and Output type.
     */
    private ArrayList<Node> nodes;
    /**
     * List of input nodes.
     */
    private ArrayList<Input> inpNodes;
    /**
     * List of output nodes.
     */
    private ArrayList<Output> outNodes;
    /**
     * List of edges.
     */
    private ArrayList<Edge> edges;

    /**
     *
     */
    public Cell() {
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
     * Get cell's nodes of class Input
     *
     * @return
     */
    public ArrayList<Input> getInputNodes() {
        return inpNodes;
    }

    /**
     * Set cell's input nodes.
     *
     * @param inpNodes
     */
    public void setInputNodes(ArrayList<Input> inpNodes) {
        this.inpNodes = inpNodes;
    }

    /**
     * Add node of type Input.
     *
     * @param node
     */
    public void addInputNode(Input node) {
        inpNodes.add(node);
    }

    /**
     * Get cell's output nodes.
     *
     * @return
     */
    public ArrayList<Output> getOutputNodes() {
        return outNodes;
    }

    /**
     * Set cell's output nodes.
     *
     * @param outNodes
     */
    public void setOutputNodes(ArrayList<Output> outNodes) {
        this.outNodes = outNodes;
    }

    /**
     * Add node of type Output.
     *
     * @param node
     */
    public void addOutputNode(Output node) {
        outNodes.add(node);
    }
    /**
     * Get cell's edges.
     *
     * @return
     */
    public ArrayList<Edge> getEdges() {
        return edges;
    }

    /**
     * Set cell's edges.
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
        nodes.add(node);
        if (node instanceof Input) {
            inpNodes.add((Input) node);
        }
        if (node instanceof Output) {
            outNodes.add((Output) node);
        }
    }


    /**
     * Add edge to the cell.
     *
     * @param edge
     */
    public void addEdge(Edge edge) {
        edges.add(edge);
        edge.getSource().addOutputEdge(edge);
        edge.getTarget().addInputEdge(edge);
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
     * Get nodes of type Input.
     *
     * @return
     */
    public ArrayList<Input> getInputs() {
        return inpNodes;
    }

    /**
     * Get nodes of type Output.
     *
     * @return
     */
    public ArrayList<Output> getOutputs() {
        return outNodes;
    }

    /**
     * Remove the node with it's input edges from the cell and return nodes output edge's that has been left unconnected.
     * <p>
     * When removing node from the cell output edges won't be removed because they represent other nodes input edges. The will be left floating and they must be properly reconnected to other nodes in order for the cell to proper work.
     *
     * @param node Node to be removed.
     * @return Unconnected output edges of the node that has been removed.
     */
    public ArrayList<Edge> removeNode(Node node) {
        //Remove input edges of the node
        for (Edge edge : node.getInputEdges()) {
            removeEdge(edge);
        }
        //Disconnect output edges of the node
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
     * @param inpData Input data to be mapped trough cell.
     * @return Mapped data.
     */
    public double[] runEvent(double[] inpData) {
        setInpValues(inpData);
        clcCell();
        return getOutValues();
    }

    /**
     * Calculate cell output stream of data based on the provided input stream of data.
     *
     * @param inpData Input stream of data to be mapped trough cell.
     * @return Mapped stream data.
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
     * Set signal values of Input nodes.
     *
     * @param inp Input data.
     */
    private void setInpValues(double[] inp) {
        for (int i = 0; i < inpNodes.size(); i++) {
            inpNodes.get(i).importSignal(inp[i]);
        }
    }

    /**
     * Get output signals of Output nodes.
     *
     * @return
     */
    private double[] getOutValues() {
        double[] out = new double[outNodes.size()];
        for (int i = 0; i < outNodes.size(); i++) {
            out[i]=outNodes.get(i).getSignal();
        }
        return out;
    }

    /**
     * Execute calculation process of data transition trough nodes and edges of the cell.
     */
    private void clcCell() {
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
    }

    /**
     * Reset cell's nodes.
     */
    private void rstCell() {
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
}

