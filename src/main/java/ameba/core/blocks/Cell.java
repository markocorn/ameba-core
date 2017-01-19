package ameba.core.blocks;


import ameba.core.blocks.connections.Collector;
import ameba.core.blocks.connections.Edge;
import ameba.core.blocks.nodes.INodeInput;
import ameba.core.blocks.nodes.INodeOutput;
import ameba.core.blocks.nodes.Node;
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
     * List of input nodes interfaces.
     */
    private ArrayList<INodeInput> inpNodes;
    /**
     * List of input nodes.
     */
    private ArrayList<INodeOutput> outNodes;

    private ArrayList<Edge> edges;

    private int maxNodes;

    private ArrayList<Object> importedValues;
    private ArrayList<Object> exportedValues;


    /**
     *
     */
    public Cell(int maxNodes) {
        fitnessValue = 0.0;
        nodes = new ArrayList<>();
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
     * Get cell's connections.
     *
     * @return
     */
    public ArrayList<Edge> getEdges() {
        return edges;
    }

    /**
     * Set cell's connections.
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
    public void addNode(Node node) throws Exception {
        if (nodes.size() < maxNodes) {
            nodes.add(node);
            if (node instanceof INodeInput) {
                inpNodes.add((INodeInput) node);
            }
            if (node instanceof INodeOutput) {
                outNodes.add((INodeOutput) node);
            }
        } else throw new Exception("Maximum number of nodes exceeded");
    }

    /**
     * Add edge to the cell.
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
     * Remove the node with it's input connections from the cell and return nodes output edge's that has been left unconnected.
     * <p>
     * When removing node from the cell output connections won't be removed because they represent other nodes input connections. The will be left floating and they must be properly reconnected to other nodes in order for the cell to proper work.
     *
     * @param node Node to be removed.
     * @return Unconnected output connections of the node that has been removed.
     */
    public void removeNode(Node node) throws Exception {
        //Remove input Edges of the node from collectors
        for (Collector collector : node.getInpCollectors()) {
            collector.removeEdges();
        }
        for (Collector collector : node.getOutCollectors()) {
            collector.removeEdges();
        }
        //Remove node from the cell
        nodes.remove(node);
    }

    //    /**
//     * Calculate cell output data based on the provided input data for one discrete time event.
//     *
//     * @param inpData InputDec data to be mapped trough cell.
//     * @return Mapped data.
//     */
    public void runEvent(ArrayList<Object> values) throws Exception {
        importSignals(values);
        clcCell();
    }

    public ArrayList<ArrayList<Object>> run(ArrayList<ArrayList<Object>> inputs) throws Exception {
        clearCell();
        ArrayList<ArrayList<Object>> out = new ArrayList<>();
        for (ArrayList<Object> data : inputs) {
            rstCell();
            runEvent(data);
            out.add((ArrayList<Object>) exportedValues.clone());
        }
        return out;
    }


    private void importSignals(ArrayList<Object> values) throws Exception {
        if (values.size() == inpNodes.size()) {
            importedValues = values;
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) instanceof Double) {
                    inpNodes.get(i).importSignal(Double.class, values.get(i));
                }
                if (values.get(i) instanceof Integer) {
                    inpNodes.get(i).importSignal(Integer.class, values.get(i));
                }
                if (values.get(i) instanceof Boolean) {
                    inpNodes.get(i).importSignal(Boolean.class, values.get(i));
                }
            }

        } else throw new Exception("Input array not equal to the number of input nodes");
    }

    /**
     * Execute calculation process of data transition trough nodes and connections of the cell.
     */
    private void clcCell() {
        boolean in = true;
        while (in) {
            in = false;
            for (Node node : nodes) {
                node.clcNode();
                if (!node.hasAllEdgesSend()) {
                    in = true;
                }
            }
        }
        exportedValues.clear();
        for (INodeOutput node : outNodes) {
            exportedValues.add(node.exportSignal());
        }
    }

    public ArrayList<Object> getExportedValues() {
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
        //Reset all connections
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

