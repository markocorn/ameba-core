package blocks;


import blocks.nodes.Input;
import blocks.nodes.Output;

import java.io.*;
import java.util.*;

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
    public ArrayList<Input> getInpNodes() {
        return inpNodes;
    }

    /**
     * Set cell's input nodes.
     *
     * @param inpNodes
     */
    public void setInpNodes(ArrayList<Input> inpNodes) {
        this.inpNodes = inpNodes;
    }

    /**
     * Get cell's output nodes.
     *
     * @return
     */
    public ArrayList<Output> getOutNodes() {
        return outNodes;
    }

    /**
     * Set cell's output nodes.
     *
     * @param outNodes
     */
    public void setOutNodes(ArrayList<Output> outNodes) {
        this.outNodes = outNodes;
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
    }

    /**
     * Add edges references to edges source and target nodes.
     */
    public void edgesAutoConnect() {
        for (Edge edge : edges) {
            edge.getSource().addOutputEdge(edge);
            edge.getTarget().addInputEdge(edge);
        }
    }

    /**
     * Remove edge and
     *
     * @param edge
     */
    public void removeEdge(Edge edge) {
        Iterator<Edge> iterator = edges.values().iterator();
        Edge edgeIt;
        while (iterator.hasNext()) {
            edgeIt = iterator.next();
            if (edgeIt == edge) {
                edge.getSource().removeOutputEdge(edge);
                edge.getTarget().removeInput(edge);
                iterator.remove();
            }
        }
    }


    //Get input blocks.nodes
    public ArrayList<nodes.Node> getInputs() {
        return inpNodes;
    }

    //Get output blocks.nodes
    public ArrayList<nodes.Node> getOutputs() {
        return outNodes;
    }

    //Remove node
    public ArrayList<Edge> removeNode(nodes.Node node) {
        ArrayList<Edge> outEdges = node.getOutputs();
        //Remove input edges
        while (node.getInputs().size() > 0) {
            removeEdge(node.getInputs().get(0));
        }
        Iterator iterator = nodes.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == node) {
                iterator.remove();
            }
        }
        if (inpNodes.contains(node)) inpNodes.remove(node);
        if (outNodes.contains(node)) outNodes.remove(node);

        return outEdges;
    }

    //Refresh edge

    //Get blocks.nodes (without input and output)
    public ArrayList<nodes.Node> getInnerNodes() {
        ArrayList<nodes.Node> nodeList = new ArrayList<nodes.Node>();
        for (Map.Entry<Integer, nodes.Node> entry : nodes.entrySet()) {
            nodes.Node node = entry.getValue();
            if (node instanceof nodes.Input || node instanceof nodes.Output) {
            } else nodeList.add(node);
        }
        return nodeList;
    }

    //Simulate cell for one time event
    public ArrayList<Double> simEventCell(ArrayList<Double> inpData) {
        setInpValues(inpData);
        clcCell();
        return getOutValues();
    }

    //Simulate cell
    public ArrayList<ArrayList<Double>> simCell(ArrayList<ArrayList<Double>> inpData) {
        clearCell();
        ArrayList<ArrayList<Double>> outData = new ArrayList<ArrayList<Double>>();
        for (int i = 0; i < inpData.size(); i++) {
            rstCell();
            outData.add(simEventCell(inpData.get(i)));
        }
        return outData;
    }

    //Copy of object
    public Cell copy() {
        Object obj = null;
        try {
            // Write the object out to a byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            out.close();

            // Make an input stream from the byte array and read
            // a copy of the object back in.
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
            obj = in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return (Cell) obj;
    }

    //Set input data
    private void setInpValues(ArrayList<Double> inp) {
        for (int i = 0; i < inpNodes.size(); i++) {
            ((nodes.Input) inpNodes.get(i)).importSignal(inp.get(i));
        }
    }

    //Get output values
    private ArrayList<Double> getOutValues() {
        ArrayList<Double> out = new ArrayList();
        for (nodes.Node node : outNodes) {
            out.add(node.getOutputValue());
        }
        return out;
    }

    //Run cell
    private void clcCell() {
        int nmbClcCell = 0;
        //All blocks.nodes with initial values transmit data
//        for (Node node:blocks.nodes.values()){
//            if (node.getOutputFlag()){
//
//            }
//        }
        while (true) {
            nmbClcCell++;
            int nmbClcNode = 0;
            for (Map.Entry<Integer, nodes.Node> entry : nodes.entrySet()) {
                nodes.Node node = entry.getValue();
                node.clcNode();
                if (node.getClcFlag()) {
                    nmbClcNode++;
                }
            }
            if (nmbClcNode >= this.nodes.size() || nmbClcCell > this.nodes.size()) {
                break;
            }
        }
    }

    //Reset cell
    private void rstCell() {
        for (Map.Entry<Integer, nodes.Node> entry : nodes.entrySet()) {
            nodes.Node node = entry.getValue();
            node.rstNode();
        }
    }

    //Clear cell
    private void clearCell() {

        for (Map.Entry<Integer, nodes.Node> entry : nodes.entrySet()) {
            nodes.Node node = entry.getValue();
            node.clearNode();
        }

    }

    //Add fitness function
    public void addFitnessValue(Double fitValue) {
        this.fitnessValue = this.fitnessValue + fitValue;
    }

    //Get fitness function
    public Double getFitnessValue() {
        return this.fitnessValue;
    }

    //Set fitness function
    public void setFitnessValue(double fitValue) {
        this.fitnessValue = fitValue;
    }

    //Get random inner node
    public nodes.Node getRandomInnerNode() {
        ArrayList<nodes.Node> anode = new ArrayList<nodes.Node>();
        for (Map.Entry<Integer, nodes.Node> entry : nodes.entrySet()) {
            nodes.Node node = entry.getValue();
            if (node.getClass().getSimpleName().equals("Input") || node.getClass().getSimpleName().equals("Output")) {
            } else anode.add(node);
        }
        if (anode.size() > 0) {
            return anode.get(rndGen.nextInt(anode.size()));
        } else return null;
    }

    //Get random inner node with parameter
    public nodes.Node getRandomInnerNodeParameter() {
        ArrayList<nodes.Node> anode = new ArrayList<nodes.Node>();
        for (Map.Entry<Integer, nodes.Node> entry : nodes.entrySet()) {
            nodes.Node node = entry.getValue();
            if (node.getClass().getSimpleName().equals("Input") || node.getClass().getSimpleName().equals("Output") || node.getPar() == null) {
            } else anode.add(node);
        }
        if (anode.size() > 0) {
            return anode.get(rndGen.nextInt(anode.size()));
        } else return null;
    }

    //Get random  node with free output
    public nodes.Node getRandomNodeOutput() {
        ArrayList<nodes.Node> anode = new ArrayList<nodes.Node>();
        for (Map.Entry<Integer, nodes.Node> entry : nodes.entrySet()) {
            nodes.Node node = entry.getValue();
            if (node.getClass().getSimpleName().equals("Output")) {
            } else anode.add(node);
        }
        if (anode.size() > 0) {
            return anode.get(rndGen.nextInt(anode.size()));
        } else return null;
    }

    //Get random  node with free output
    public nodes.Node getRandomNodeOutputExlude(ArrayList<nodes.Node> excludedNodes) {
        ArrayList<nodes.Node> anode = new ArrayList<nodes.Node>();
        for (Map.Entry<Integer, nodes.Node> entry : nodes.entrySet()) {
            nodes.Node node = entry.getValue();
            if (node.getClass().getSimpleName().equals("Output") || excludedNodes.contains(node)) {
            } else anode.add(node);
        }
        if (anode.size() > 0) {
            return anode.get(rndGen.nextInt(anode.size()));
        } else return null;
    }

    //Get random  node with free output
    public nodes.Node getRandomNode() {
        return nodes.get(rndGen.nextInt(nodes.size()));
    }

    //Get random connection
    public Edge getRandomEdge() {
        List<Edge> list = new ArrayList<Edge>(edges.values());
        if (edges.size() == 0) {
            int t = 0;
        }
        return list.get(rndGen.nextInt(list.size()));
    }

    //Get key of edge
    public Integer getEdgeKey(Edge edge) {
        Integer key = 0;
        for (Map.Entry<Integer, Edge> entry : edges.entrySet()) {
            if (entry.getValue() == edge) key = entry.getKey();
        }
        return key;
    }

    //Get number of edges
    public Integer getEdgeKeyNumber(Edge edge) {
        Integer key = 0;
        for (Map.Entry<Integer, Edge> entry : edges.entrySet()) {
            if (entry.getValue() == edge) key++;
        }
        return key;
    }

    //Get key of node
    public Integer getNodeKey(nodes.Node node) {
        Integer key = 0;
        for (Map.Entry<Integer, nodes.Node> entry : nodes.entrySet()) {
            if (entry.getValue() == node) key = entry.getKey();
        }
        return key;
    }

    //Get list of connected blocks.nodes
    public ArrayList<nodes.Node> getListConnectedNodes(nodes.Node node) {
        ArrayList<nodes.Node> nodes = new ArrayList<nodes.Node>();
        for (Edge edge : node.getInputs()) {
            nodes.add(edge.getSource());
        }
        for (Edge edge : node.getOutputs()) {
            nodes.add(edge.getTarget());
        }
        return nodes;
    }

    //Get list of connected blocks.nodes by layers
    public ArrayList<ArrayList<nodes.Node>> getTreeMapNodes(nodes.Node node) {
        ArrayList<ArrayList<nodes.Node>> nodes = new ArrayList<ArrayList<nodes.Node>>();
        //First layer
        nodes.add(new ArrayList<nodes.Node>(Arrays.asList(node)));
        //Other layers
        //Get list of all blocks.nodes of layer
        int size = 0;
        while (size < nodes.size()) {
            size = nodes.size();
            ArrayList<nodes.Node> nodeLayer = new ArrayList<nodes.Node>();
            for (nodes.Node node1 : nodes.get(nodes.size() - 1)) {
                ArrayList<nodes.Node> nodesOfNode1 = getListConnectedNodes(node1);
                for (nodes.Node nodeOfNode1 : nodesOfNode1) {
                    if (!chkNodeTreeMap(nodeOfNode1, nodes) && !nodeLayer.contains(nodeOfNode1) && !(nodeOfNode1 instanceof nodes.Input) && !(nodeOfNode1 instanceof nodes.Output)) {
                        nodeLayer.add(nodeOfNode1);
                    }
                }
            }
            if (nodeLayer.size() > 0) {
                nodes.add(nodeLayer);
            }
        }
        return nodes;
    }

    public ArrayList<Edge> getInputEdgesTreeMap(ArrayList<nodes.Node> nodes) {
        ArrayList<Edge> edges = new ArrayList<Edge>();
        for (nodes.Node node : nodes) {
            for (Edge edge : node.getInputs()) {
                if (!nodes.contains(edge.getSource())) {
                    edges.add(edge);
                }
            }
        }
        return edges;
    }

    public ArrayList<Edge> getOutputEdgesTreeMap(ArrayList<nodes.Node> nodes) {
        ArrayList<Edge> edges = new ArrayList<Edge>();
        for (nodes.Node node : nodes) {
            for (Edge edge : node.getOutputs()) {
                if (!nodes.contains(edge.getTarget())) {
                    edges.add(edge);
                }
            }
        }
        return edges;
    }

    private boolean chkNodeTreeMap(nodes.Node node, ArrayList<ArrayList<nodes.Node>> nodes) {
        for (ArrayList<nodes.Node> nodeArrayList : nodes) {
            if (nodeArrayList.contains(node)) return true;
        }
        return false;
    }

    //Get list of connected input blocks.nodes
    public ArrayList<nodes.Node> getListConnectedInputNodes(nodes.Node node) {
        ArrayList<nodes.Node> nodes = new ArrayList<nodes.Node>();
        for (Edge edge : node.getInputs()) {
            nodes.add(edge.getSource());
        }
        return nodes;
    }

    //Get list of connected output blocks.nodes
    public ArrayList<nodes.Node> getListConnectedOutputNodes(nodes.Node node) {
        ArrayList<nodes.Node> nodes = new ArrayList<nodes.Node>();
        for (Edge edge : node.getOutputs()) {
            nodes.add(edge.getTarget());
        }
        return nodes;
    }

    //Check cell connections
    public boolean checkCell() {
        boolean wrong = false;
        for (nodes.Node node : nodes.values()) {
            if (node.getInputs() != null) {
                for (Edge edge : node.getInputs()) {
                    if (edge.getTarget() != node) {
                        System.out.println("Node input: " + node + " is not connected to the edge: " + edge + " target");
                        wrong = true;
                    }
                }
            }
        }
        for (nodes.Node node : nodes.values()) {
            if (node.getOutputs() != null) {
                for (Edge edge : node.getOutputs()) {
                    if (edge.getSource() != node) {
                        System.out.println("Node output: " + node + " is not connected to the edge: " + edge + " source");
                        wrong = true;
                    }
                }
            }
        }

        for (Edge edge : edges.values()) {
            if (!edge.getTarget().getInputs().contains(edge)) {
                System.out.println("blocks.Edge target: " + edge + " is not connected to the node input: " + edge.getTarget());
                wrong = true;
            }
            if (!edge.getSource().getOutputs().contains(edge)) {
                System.out.println("blocks.Edge source: " + edge + " is not connected to the node output: " + edge.getSource());
                wrong = true;
            }
        }

        for (nodes.Node node : nodes.values()) {
            if (!(node instanceof nodes.Input)) {
                if (node.getInputs().size() > 1 && node instanceof Amplify) {
                    System.out.println("Amplify node inputs: " + node.getInputs().size());
                    wrong = true;
                }
                if (node.getInputs().size() > 1 && node instanceof nodes.Delay) {
                    System.out.println("Delay node inputs: " + node.getInputs().size());
                    wrong = true;
                }
                if (node.getInputs().size() > 1 && node instanceof nodes.Output) {
                    System.out.println("Output node inputs: " + node.getInputs().size());
                    wrong = true;
                }
            }
        }
        return wrong;
    }

    //Find next key
    public Integer getNextKey(Set<Integer> keySet) {
        for (int i = 0; i < keySet.size() + 1; i++) {
            if (!keySet.contains(i)) {
                return i;
            }
        }
        return null;
    }

}

