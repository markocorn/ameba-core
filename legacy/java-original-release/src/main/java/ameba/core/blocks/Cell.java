package ameba.core.blocks;


import ameba.core.blocks.collectors.Collector;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.blocks.nodes.types.Input;
import ameba.core.blocks.nodes.types.Output;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.util.*;

strictfp public class Cell implements Serializable {
    public String lastRep = "";
    double[] exports;
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
    private ArrayList<Input> inpNodes = new ArrayList<>();
    /**
     * List of input nodes.
     */
    private ArrayList<Output> outNodes = new ArrayList<>();

    private ArrayList<Edge> edges;

    private int maxNodes;

    private String id = "";

    /**
     *
     */
    public Cell(int maxNodes) {
        fitnessValue = 0.0;
        nodes = new ArrayList<>();
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


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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


    /**
     * Add node to the cell.
     *
     * @param node
     */

    public void addNode(Node node) throws Exception {
        if (nodes.size() < maxNodes) {
            nodes.add(node);
            if (node instanceof Input) {
                inpNodes.add((Input) node);
                return;
            }
            if (node instanceof Output) {
                outNodes.add((Output) node);
                return;
            }
        } else throw new Exception("Maximum number of nodes exceeded");
    }

    public ArrayList<Node> getInnerNodes() {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof Input || node instanceof Output)) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }

    public ArrayList<Node> getInnerNodesUnlocked() {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof Input || node instanceof Output) && !node.isLock()) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }

    public ArrayList<Node> getInnerNodesFullUnlocked() {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof Input || node instanceof Output)
                    && !node.isLock()
                    && !node.hasLockedEdgesSource()
                    && !node.hasLockedEdgesTarget()) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }


    public ArrayList<Node> getInnerNodesParType(ParType type, Node nodeNot) {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof Input || node instanceof Output) && node.getParTypes().contains(type) && !node.equals(nodeNot)) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }

    public ArrayList<Node> getInnerNodesParTypeUnlocked(ParType type) {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof Input || node instanceof Output) && node.getParTypes().contains(type) && node.hasParamsUnlocked(type)) {
                nodes1.add(node);
            }
        }
        return nodes1;
    }

    public ArrayList<Node> getInnerNodesParTypeUnlocked(ParType type, Node nodeNot) {
        ArrayList<Node> nodes1 = new ArrayList<>();
        for (Node node : nodes) {
            if (!(node instanceof Input || node instanceof Output) && node.getParTypes().contains(type) && !node.equals(nodeNot) && node.hasParamsUnlocked(type)) {
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
     * Add edge to the cell.
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

    public ArrayList<Input> getInpNodes() {
        return inpNodes;
    }

    public ArrayList<Output> getOutNodes() {
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

    public void importSignals(double[] signals) {
        for (int i = 0; i < inpNodes.size(); i++) {
            inpNodes.get(i).importSignal(signals[i]);
        }
    }

    public void exportSignals() {
        exports = new double[outNodes.size()];
        for (int i = 0; i < outNodes.size(); i++) {
            exports[i] = outNodes.get(i).exportSignal();
        }
    }

    public void initCell() {
        for (Node node : nodes) {
            node.simOptimization();
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

    public double[] getExportedValues() {
        return exports;
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
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (Cell) ois.readObject();
        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

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

    public ArrayList<Edge> getGroupEdgesInner(ArrayList<ArrayList<Node>> group) {
        ArrayList<Edge> edges = new ArrayList<>();

        for (ArrayList<Node> levels : group) {
            for (Node node : levels) {
                for (CollectorTarget collectorInp : node.clcCollectorsTargetConnected()) {
                    if (containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached()) && !edges.contains(collectorInp.getEdges().get(0))) {
                        edges.add(collectorInp.getEdges().get(0));
                    }
                }

                for (CollectorSource collectorOut : node.getCollectorsSourceConnected()) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (containsNodeGroup(group, edge.getTarget().getNodeAttached()) && !edges.contains(edge)) {
                            edges.add(edge);
                        }
                    }
                }
            }
        }
        return edges;
    }

    public HashMap<String, ArrayList<Edge>> getGroupEdgesBorder(ArrayList<ArrayList<Node>> group) {

        ArrayList<Edge> edgesInp = new ArrayList<>();
        ArrayList<Edge> edgesOut = new ArrayList<>();

        for (ArrayList<Node> levels : group) {
            for (Node node : levels) {
                for (CollectorTarget collectorInp : node.clcCollectorsTargetConnected()) {
                    if (!containsNodeGroup(group, collectorInp.getEdges().get(0).getSource().getNodeAttached())) {
                        edgesInp.add(collectorInp.getEdges().get(0));
                    }
                }
                for (CollectorSource collectorOut : node.getCollectorsSourceConnected()) {
                    for (Edge edge : collectorOut.getEdges()) {
                        if (!containsNodeGroup(group, edge.getTarget().getNodeAttached())) {
                            edgesOut.add(edge);
                        }
                    }
                }
            }
        }
        HashMap<String, ArrayList<Edge>> out = new HashMap<>();
        out.put("edgesInp", edgesInp);
        out.put("edgesOut", edgesOut);
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
            for (CollectorTarget t : node.getCollectorsTargetToConnect()) {
                if (t.getEdges().size() == 0) {
                    out.add("Nodes: " + node.toString() + " target collector Dec: " + t.toString() + "not connected.");
                }
            }
//            Check for target collectors -> edge -> target collector -> attached nodes
            for (CollectorTarget t : node.getCollectorsTargetConnected()) {
                for (Edge e : t.getEdges()) {
                    if (e.getTarget().getNodeAttached() != node) {
                        out.add("Attached node " + node.toString() + "of Target collector: " + t.toString() + " does not match attached node of edges target collector");
                    }
                }

            }
        }

        //Check for duplicate edges
        for (Node n : nodes) {
            for (CollectorTarget t : n.getCollectorsTarget()) {
                if (t.getEdges().size() > 0) {
                    Edge e = t.getEdges().get(0);
                    for (CollectorTarget t2 : n.getCollectorsTarget()) {
                        if (t2.getEdges().size() > 0) {
                            if (t != t2 && t2.getEdges().get(0) == e) {
                                out.add("Edge multiplied on targets");
                            }
                        }
                    }
                }
            }
            ArrayList<Edge> list = new ArrayList<>();
            for (CollectorSource s : n.getCollectorsSource()) {
                list.addAll(s.getEdges());
            }
            Set<Edge> set = new HashSet<Edge>(list);
            if (set.size() < list.size()) {
                out.add("Source edges duplication");
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
            getNodes().get(i).clcCollectorsTargetConnected();
            ObjectNode node = mapper.createObjectNode();
            node.put("id", ind);
            node.put("lock", getNodes().get(i).isLock());
            ind++;
            node.put("type", getNodes().get(i).getClass().getSimpleName());
            ArrayNode inpColDec = node.putArray("sourceCol");
            for (int j = 0; j < getNodes().get(i).getCollectorsSourceConnected().size(); j++) {
                inpColDec.add(mapper.createObjectNode().put("id", ind));
                sourceId.put(getNodes().get(i).getCollectorsSourceConnected().get(j), ind);
                ind++;
            }
            ArrayNode outColDec = node.putArray("targetCol");
            for (int j = 0; j < getNodes().get(i).getCollectorsTargetConnected().size(); j++) {
                outColDec.add(mapper.createObjectNode().put("id", ind));
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
            edge.put("id", ind)
                    .put("weight", (getEdges().get(i)).getWeight())
                    .put("sourceCol", sourceId.get(getEdges().get(i).getSource()))
                    .put("targetCol", targetId.get(getEdges().get(i).getTarget()))
                    .put("lockTarget", getEdges().get(i).isLockTarget())
                    .put("lockSource", getEdges().get(i).isLockSource())
                    .put("lockWeight", getEdges().get(i).isLockWeight());
            edges.add(edge);
            ind++;
        }


        ObjectNode out = mapper.createObjectNode();
        out.put("cell", cell);
        return mapper.writerWithDefaultPrettyPrinter().

                writeValueAsString(out);
    }

    public enum ParType {DECIMAL, INTEGER, BOOLEAN}
}

