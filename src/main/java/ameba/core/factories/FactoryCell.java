package ameba.core.factories;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.blocks.nodes.types.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

/**
 * Created by marko on 10/20/16.
 */
public class FactoryCell implements Serializable {
    private FactoryCellSettings cellFactorySettings;
    private Random rndGen;
    private FactoryNode nodeFactory;
    private FactoryEdge edgeFactory;


    public FactoryCell(FactoryCellSettings cellFactorySettings, FactoryNode nodeFactory, FactoryEdge edgeFactory, long seed) {
        this.cellFactorySettings = cellFactorySettings;
        this.nodeFactory = nodeFactory;
        this.edgeFactory = edgeFactory;
        rndGen = new Random(seed);
    }

    public static FactoryCell build(long seed) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(FactoryCell.class.getClassLoader().getResourceAsStream("core/cellFactorySettings.json"));
        return new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), FactoryNode.build(seed), FactoryEdge.build(seed), seed);
    }

    public static FactoryCell build(String filePathCell, String filePathNode, String filePathEdge, long seed) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File(filePathCell));
        return new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), FactoryNode.build(filePathNode, seed), FactoryEdge.build(filePathEdge, seed), seed);
    }

    public FactoryCellSettings getCellFactorySettings() {
        return cellFactorySettings;
    }

    public FactoryNode getNodeFactory() {
        return nodeFactory;
    }

    public void setNodeFactory(FactoryNode nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public FactoryEdge getEdgeFactory() {
        return edgeFactory;
    }

    public void setEdgeFactory(FactoryEdge edgeFactory) {
        this.edgeFactory = edgeFactory;
    }

    /**
     * Build cell with random number of nodes and randomly connected edges.
     *
     * @return generated cell.
     */
    public Cell genCellRnd() throws Exception {
        Cell cell = new Cell(cellFactorySettings.getNodeMax());
        //Add input nodes
        for (int i = 0; i < cellFactorySettings.getNodeInp(); i++) {
            cell.addNode(new Input());
        }
        //Add output nodes
        for (int i = 0; i < cellFactorySettings.getNodeOut(); i++) {
            cell.addNode(new Output());
        }
        //Determine initial number of nodes
        int numNodes = genNmbNodesInitial();

        for (int i = 0; i < numNodes; i++) {
            Node node;
            //Get target collector that must be connected
            CollectorTarget collectorTarget = getCollectorTargetToConnectRnd(cell);
            if (collectorTarget == null) {
                // if there is no collector fine one on the source side of nodes
                CollectorSource collectorSource = getCollectorSourceRnd(cell);
                if (collectorSource == null) throw new Exception("Can't generate cell with no initial nodes");
                node = nodeFactory.genNodeRndCollectorSource();
            } else {
                node = nodeFactory.genNodeRndCollectorSource();
            }
            if (node == null) throw new Exception("Not possible to generate nodes");
            cell.addNode(node);
        }
        connectsMinFreeInputs(cell);
        if (cell.checkCell().size() > 0) {
            throw new Exception("Cell not properly generated. Cell Factory");
        }
        return cell;
    }

    public void connectsMinFreeInputs(Cell cell) throws Exception {
        while (true) {
            CollectorTarget collectorTarget = getCollectorTargetToConnectRnd(cell);
            if (collectorTarget == null) {
                break;
            }
            CollectorSource collectorSource = getCollectorSourceRndNoNode(cell, collectorTarget.getNodeAttached());
            if (collectorSource != null) {
                cell.addEdge(edgeFactory.genEdge(collectorSource, collectorTarget));
            } else {
                if (nodeFactory.nodeSettingsHashMap.get("Constant").getProbability() > 0) {
                    cell.addNode(nodeFactory.genNode("Constant"));
                } else
                    throw new Exception("Cell can't be properly connected. Must allow the generation of Constant nodes.");
            }
        }
    }

    public void reconnectEdges(Cell cell, HashMap<String, ArrayList<Edge>> borderEdges) throws Exception {
        String ind = "";
        ArrayList<Edge> oldEdges = (ArrayList<Edge>) cell.getEdges();
        int diff = borderEdges.get("edgesInp" + ind).size() - oldEdges.size();
        int same = Math.min(borderEdges.get("edgesInp" + ind).size(), oldEdges.size());
        Collections.shuffle(oldEdges);
        ArrayList<Edge> remains = new ArrayList<>();
        for (int i = 0; i < same; i++) {
            Edge edge1 = oldEdges.get(i);
            Edge edge2 = borderEdges.get("edgesInp" + ind).get(i);
            //Remove edge 2 from source
            edge2.getSource().removeEdge(edge2);
            //Set new source for edge 2
            edge2.setSource(edge1.getSource());
            //Remove edge 1 from its source collector
            edge2.getSource().removeEdge(edge1);
            remains.add(edge1);
            //Add edge 2 to cell
            cell.addEdge(edge2);
        }
        if (diff > 0) {
            for (int i = same; i < same + diff; i++) {
                borderEdges.get("edgesInp" + ind).get(i).getTarget().removeEdge(borderEdges.get("edgesInp" + ind).get(i));
            }
        }
        //Reconnect output edges
        diff = borderEdges.get("edgesOut" + ind).size() - remains.size();
        same = Math.min(borderEdges.get("edgesOut" + ind).size(), remains.size());
        for (int i = 0; i < same; i++) {
            Edge edge3 = borderEdges.get("edgesOut" + ind).get(i);
            Edge edge1 = remains.get(i);
            edge1.setSource(edge3.getSource());
            edge1.getSource().addEdge(edge1);
            edge1.getSource().removeEdge(edge3);
        }
        if (diff > 0) {
            for (int i = same; i < same + diff; i++) {
                borderEdges.get("edgesOut" + ind).get(i).getSource().removeEdge(borderEdges.get("edgesOut" + ind).get(i));
            }
        } else {
            for (int i = same; i < same - diff; i++) {
                remains.get(i).getTarget().removeEdge(remains.get(i));
                cell.removeEdge(remains.get(i));
            }
        }
        connectsMinFreeInputs(cell);
    }

    public CollectorSource getCollectorSourceRnd(ArrayList<CollectorSource> collectorOuts) {
        ArrayList<CollectorSource> outs = new ArrayList<>();
        for (CollectorSource collectorOut : collectorOuts) {
            outs.add(collectorOut);
        }
        if (outs.size() > 0) return outs.get(rndGen.nextInt(outs.size()));
        return null;
    }

    /**
     * Get random input collector of proper type and without edge.
     *
     * @param cell Cell to ISelect node from.
     * @return Selected node.
     * @Param node Node that can't be selected.
     */
    public CollectorTarget getCollectorTargetRnd(Cell cell) throws Exception {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorTarget collector : node.getCollectorsTarget()) {
                if (collector.getEdges().size() == 0) {
                    collectors.add(collector);
                }
            }
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }

    /**
     * Generate random number for the initial size of nodes in cell.
     *
     * @return Randomly generated number with constrains from cell nodeSettingsHashMap.
     */
    public int genNmbNodesInitial() {
        if (Objects.equals(cellFactorySettings.getNodeInitial()[1], cellFactorySettings.getNodeInitial()[0])) {
            return cellFactorySettings.getNodeInitial()[1];
        } else {
            return rndGen.nextInt(cellFactorySettings.getNodeInitial()[1] - cellFactorySettings.getNodeInitial()[0]) + cellFactorySettings.getNodeInitial()[0];
        }
    }

    public CollectorSource getCollectorSourceRndNoNode(Cell cell, Node node) throws Exception {
        ArrayList<CollectorSource> collectors = new ArrayList<>();
        for (Node node1 : cell.getNodes()) {
            for (CollectorSource collector : node1.getCollectorsSource()) {
                if (!node1.equals(node)) {
                    collectors.add(collector);
                }
            }
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }

    public CollectorSource getCollectorSourceRnd(Cell cell) throws Exception {
        ArrayList<CollectorSource> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getCollectorsSource());
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }


    /**
     * Get random input connector that is free and nodes minimum constrain of connected input connectors is meet.
     *
     * @param cell
     * @return
     * @throws Exception
     */

    public CollectorTarget getCollectorTargetToConnectRnd(Cell cell) {
        if (getCollectorTargetToConnect(cell).size() > 0) {
            return getCollectorTargetToConnect(cell).get(rndGen.nextInt(getCollectorTargetToConnect(cell).size()));
        } else return null;
    }

    public CollectorTarget getCollectorTargetToConnectRndNoNode(Cell cell, Node node) {
        ArrayList<CollectorTarget> list = getCollectorTargetToConnectNoNode(cell, node);
        if (list.size() > 0) {
            return list.get(rndGen.nextInt(list.size()));
        } else return null;
    }

    public ArrayList<CollectorTarget> getCollectorTargetToConnect(Cell cell) {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getCollectorsTargetToConnect());
        }
        return collectors;
    }

    public CollectorTarget getCollectorTargetFreeRnd(Cell cell) {
        ArrayList<CollectorTarget> list = getCollectorsTargetFree(cell);
        if (list.size() > 0) {
            return list.get(rndGen.nextInt(list.size()));
        }
        return null;
    }

    public ArrayList<CollectorTarget> getCollectorsTargetFree(Cell cell) {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getCollectorsTargetFree());
        }
        return collectors;
    }

    public ArrayList<CollectorTarget> getCollectorTargetToConnectNoNode(Cell cell, Node node) {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        for (Node node1 : cell.getNodes()) {
            if (!node1.equals(node)) {
                collectors.addAll(node.getCollectorsTargetToConnect());
            }
        }
        return collectors;
    }

    public ArrayList<Edge> getEdgesCanBeRemoved(Cell cell) {
        ArrayList<Edge> list = new ArrayList<>();
        for (Edge e : cell.getEdges()) {
            if (!e.isLockTarget() && !e.isLockSource() && e.getTarget().getNodeAttached().clcCollectorsTargetConnected().size() > e.getTarget().getNodeAttached().getCollectorTargetLimit()[0]) {
                list.add(e);
            }
        }

        int t = 0;
        return list;
    }

    public ArrayList<CollectorTarget> getCollectorsTargetNotConnected(Cell cell) {
        ArrayList<CollectorTarget> list = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorTarget t : node.getCollectorsTarget()) {
                if (t.getEdges().size() < 1) {
                    list.add(t);
                }
            }
        }
        return list;
    }


    public CollectorTarget getCollectorInpOneRnd(Cell cell) throws Exception {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorTarget collector : node.getCollectorsTarget()) {
                if (collector.getEdges().size() == 0) {
                    collectors.add(collector);
                    break;
                }
            }
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }

    /**
     * @param cell
     * @return
     */
    public Node getNodeRndInnerUnlocked(Cell cell) {
        if (cell.getInnerNodesUnlocked().size() > 0) {
            return cell.getInnerNodesUnlocked().get(rndGen.nextInt(cell.getInnerNodesUnlocked().size()));
        }
        return null;
    }

    public Node getNodeRndInnerFullUnlocked(Cell cell) {
        if (cell.getInnerNodesFullUnlocked().size() > 0) {
            return cell.getInnerNodesFullUnlocked().get(rndGen.nextInt(cell.getInnerNodesFullUnlocked().size()));
        }
        return null;
    }

    public FactoryCell clone() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (FactoryCell) ois.readObject();
        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Cell getCellJson(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        Cell cell = new Cell(cellFactorySettings.getNodeMax());
        HashMap<Integer, Node> nodeIntegerHashMap = new HashMap<>();
        HashMap<Integer, CollectorSource> sourceIntegerHashMap = new HashMap<>();
        HashMap<Integer, CollectorTarget> targetIntegerHashMap = new HashMap<>();
        //Set nodes and store collectors for edges
        for (int i = 0; i < node.get("cell").get("nodes").size(); i++) {
            Node newNode = nodeFactory.genNode(node.get("cell").get("nodes").get(i).get("type").asText());
            newNode.setLock(node.get("cell").get("nodes").get(i).get("lock").asBoolean());
            nodeIntegerHashMap.put(node.get("cell").get("nodes").get(i).get("id").asInt(), newNode);
            int[] count = new int[]{0, 0, 0};
            for (int j = 0; j < node.get("cell").get("nodes").get(i).get("sourceCol").size(); j++) {
                sourceIntegerHashMap.put(node.get("cell").get("nodes").get(i).get("sourceCol").get(j).get("id").asInt(), newNode.getCollectorsSource().get(count[0]));
                count[0]++;
            }
            count = new int[]{0, 0, 0};
            for (int j = 0; j < node.get("cell").get("nodes").get(i).get("targetCol").size(); j++) {
                targetIntegerHashMap.put(node.get("cell").get("nodes").get(i).get("targetCol").get(j).get("id").asInt(), newNode.getCollectorsTarget().get(count[0]));
                count[0]++;
            }
            //Set parameters of the node
            for (int j = 0; j < node.get("cell").get("nodes").get(i).get("paramsDec").size(); j++) {
                newNode.setParamDec(j, node.get("cell").get("nodes").get(i).get("paramsDec").get(j).asDouble());
            }
            for (int j = 0; j < node.get("cell").get("nodes").get(i).get("paramsInt").size(); j++) {
                newNode.setParamInt(j, node.get("cell").get("nodes").get(i).get("paramsInt").get(j).asInt());
            }
            for (int j = 0; j < node.get("cell").get("nodes").get(i).get("paramsBin").size(); j++) {
                newNode.setParamBin(j, node.get("cell").get("nodes").get(i).get("paramsBin").get(j).asBoolean());
            }
            //Set lock parameters of the node
            for (int j = 0; j < node.get("cell").get("nodes").get(i).get("lockDec").size(); j++) {
                newNode.setLockDec(j, node.get("cell").get("nodes").get(i).get("lockDec").get(j).asBoolean());
            }
            for (int j = 0; j < node.get("cell").get("nodes").get(i).get("lockInt").size(); j++) {
                newNode.setLockInt(j, node.get("cell").get("nodes").get(i).get("lockInt").get(j).asBoolean());
            }
            for (int j = 0; j < node.get("cell").get("nodes").get(i).get("lockBin").size(); j++) {
                newNode.setLockBin(j, node.get("cell").get("nodes").get(i).get("lockBin").get(j).asBoolean());
            }
            cell.addNode(newNode);
        }

        for (int i = 0; i < node.get("cell").get("edges").size(); i++) {
            Edge edge = null;
            edge = edgeFactory.genEdge(
                    (CollectorSource) sourceIntegerHashMap.get(node.get("cell").get("edges").get(i).get("sourceCol").asInt()),
                    (CollectorTarget) targetIntegerHashMap.get(node.get("cell").get("edges").get(i).get("targetCol").asInt()),
                    node.get("cell").get("edges").get(i).get("weight").asDouble());

            //Add lock parameters of the edge
            edge.setLockSource(node.get("cell").get("edges").get(i).get("lockSource").asBoolean());
            edge.setLockTarget(node.get("cell").get("edges").get(i).get("lockTarget").asBoolean());
            edge.setLockWeight(node.get("cell").get("edges").get(i).get("lockWeight").asBoolean());
            cell.addEdge(edge);
        }
        return cell;
    }

    public Cell getCellJsonFromFile(String filepath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(new File(filepath));
        return getCellJson(mapper.writeValueAsString(node));
    }

    public ArrayList<Cell> getCellsJson(String cells) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(cells);
        ArrayList<Cell> cells1 = new ArrayList<>();

        for (int i = 0; i < node.size(); i++) {
            cells1.add(getCellJson(node.get(i).toString()));
        }
        return cells1;
    }

    public ArrayList<Cell> getCellsJsonFromFile(String filepath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(new File(filepath));
        return getCellsJson(mapper.writeValueAsString(node));
    }

    public Cell genPerceptron(int inpNum, int[] layers, boolean lockNodes, boolean lockEdges, boolean lockParNodes, boolean lockParEdges) {
        Cell cell = new Cell(2000);
        ArrayList<ArrayList<NeuronStep>> net = new ArrayList<>();
        ArrayList<Input> inp = new ArrayList<>();
        ArrayList<Output> out = new ArrayList<>();

        int outLayer = layers.length - 1;
        int outNum = layers[outLayer];

        try {
            for (int i = 0; i < inpNum; i++) {
                Input i1 = new Input();
                cell.addNode(i1);
                inp.add(i1);
            }
            for (int i = 0; i < outNum; i++) {
                Output i1 = new Output();
                cell.addNode(i1);
                out.add(i1);
            }
            //Neurons
            for (int i = 0; i < layers.length; i++) {
                ArrayList<NeuronStep> n = new ArrayList<>();
                for (int j = 0; j < layers[i]; j++) {
                    NeuronStep m = new NeuronStep(1, 100);
                    n.add(m);
                    cell.addNode(m);
                }
                net.add(n);
            }
            //Edges inputs
            for (int i = 0; i < inp.size(); i++) {
                for (int j = 0; j < layers[0]; j++) {
                    Edge e = edgeFactory.genEdge(
                            inp.get(i).getCollectorsSource().get(0),
                            net.get(0).get(j).getCollectorsTarget().get(i));
                    cell.addEdge(e);
                }
            }
            //Edges outputs
            for (int i = 0; i < outNum; i++) {
                Edge e = edgeFactory.genEdge(
                        net.get(outLayer).get(i).getCollectorsSource().get(0),
                        out.get(i).getCollectorsTarget().get(0));
                cell.addEdge(e);
            }
            //Edges Layers
            for (int i = 0; i < net.size() - 1; i++) {
                for (int j = 0; j < net.get(i).size(); j++) {
                    for (int k = 0; k < net.get(i + 1).size(); k++) {
                        Edge e = edgeFactory.genEdge(
                                net.get(i).get(j).getCollectorsSource().get(0),
                                net.get(i + 1).get(k).getCollectorsTarget().get(j));
                        cell.addEdge(e);
                    }
                }
            }

            for (Node node : cell.getInnerNodes()) {
                if (lockParNodes) {
                    node.setLockDec(0, true);
                }
                if (lockNodes) {
                    node.setLock(true);
                }
            }

            for (Edge edge : cell.getEdges()) {
                if (lockParEdges) {
                    edge.setLockWeight(true);
                }
                if (lockEdges) {
                    edge.setLockSource(true);
                    edge.setLockTarget(true);
                }
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        }
        cell.checkCellPrint();

        return cell;
    }

    public Cell genPerceptronLin(int inpNum, int[] layers, boolean lockNodes, boolean lockEdges, boolean lockParNodes, boolean lockParEdges) {
        Cell cell = new Cell(2000);
        ArrayList<ArrayList<NeuronLin>> net = new ArrayList<>();
        ArrayList<Input> inp = new ArrayList<>();
        ArrayList<Output> out = new ArrayList<>();

        int outLayer = layers.length - 1;
        int outNum = layers[outLayer];

        try {
            for (int i = 0; i < inpNum; i++) {
                Input i1 = new Input();
                cell.addNode(i1);
                inp.add(i1);
            }
            for (int i = 0; i < outNum; i++) {
                Output i1 = new Output();
                cell.addNode(i1);
                out.add(i1);
            }
            //Neurons
            for (int i = 0; i < layers.length; i++) {
                ArrayList<NeuronLin> n = new ArrayList<>();
                for (int j = 0; j < layers[i]; j++) {
                    NeuronLin m = (NeuronLin) nodeFactory.genNode("NeuronLin");
                    n.add(m);
                    cell.addNode(m);
                }
                net.add(n);
            }
            //Edges inputs
            for (int i = 0; i < inp.size(); i++) {
                for (int j = 0; j < layers[0]; j++) {
                    Edge e = edgeFactory.genEdge(
                            inp.get(i).getCollectorsSource().get(0),
                            net.get(0).get(j).getCollectorsTarget().get(i));
                    cell.addEdge(e);
                }
            }
            //Edges outputs
            for (int i = 0; i < outNum; i++) {
                Edge e = edgeFactory.genEdge(
                        net.get(outLayer).get(i).getCollectorsSource().get(0),
                        out.get(i).getCollectorsTarget().get(0));
                cell.addEdge(e);
            }
            //Edges Layers
            for (int i = 0; i < net.size() - 1; i++) {
                for (int j = 0; j < net.get(i).size(); j++) {
                    for (int k = 0; k < net.get(i + 1).size(); k++) {
                        Edge e = edgeFactory.genEdge(
                                net.get(i).get(j).getCollectorsSource().get(0),
                                net.get(i + 1).get(k).getCollectorsTarget().get(j));
                        cell.addEdge(e);
                    }
                }
            }

            for (Node node : cell.getInnerNodes()) {
                if (lockParNodes) {
                    node.setLockDec(0, true);
                }
                if (lockNodes) {
                    node.setLock(true);
                }
            }

            for (Edge edge : cell.getEdges()) {
                if (lockParEdges) {
                    edge.setLockWeight(true);
                }
                if (lockEdges) {
                    edge.setLockSource(true);
                    edge.setLockTarget(true);
                }
            }
            cell.clearCell();

        } catch (Exception e1) {
            e1.printStackTrace();
        }
        cell.checkCellPrint();

        return cell;
    }

    public Cell genRecNN(int inpNum, int[] layers, boolean lockNodes, boolean lockEdges, boolean lockParNodes, boolean lockParEdges) {
        Cell cell = new Cell(2000);
        ArrayList<ArrayList<NeuronLin>> net = new ArrayList<>();
        ArrayList<ArrayList<Delay>> netD = new ArrayList<>();
        ArrayList<Input> inp = new ArrayList<>();
        ArrayList<Output> out = new ArrayList<>();

        int outLayer = layers.length - 1;
        int outNum = layers[outLayer];

        try {
            for (int i = 0; i < inpNum; i++) {
                Input i1 = new Input();
                cell.addNode(i1);
                inp.add(i1);
            }
            for (int i = 0; i < outNum; i++) {
                Output i1 = new Output();
                cell.addNode(i1);
                out.add(i1);
            }

            //Neurons
            for (int i = 0; i < layers.length; i++) {
                ArrayList<NeuronLin> n = new ArrayList<>();
                ArrayList<Delay> d = new ArrayList<>();
                for (int j = 0; j < layers[i]; j++) {
                    NeuronLin m = (NeuronLin) nodeFactory.genNode("NeuronLin");
                    n.add(m);
                    cell.addNode(m);
                    Delay e = (Delay) nodeFactory.genNode("Delay");
                    d.add(e);
                    cell.addNode(e);
                }
                net.add(n);
                netD.add(d);
            }

            //Delays
            for (int i = 0; i < layers.length; i++) {
                ArrayList<Delay> d = new ArrayList<>();
                for (int j = 0; j < out.size(); j++) {
                    Delay e = (Delay) nodeFactory.genNode("Delay");
                    d.add(e);
                    cell.addNode(e);
                }
                netD.add(d);
            }

            //Edges inputs
            for (int i = 0; i < inp.size(); i++) {
                for (int j = 0; j < layers[0]; j++) {
                    Edge e = edgeFactory.genEdge(
                            inp.get(i).getCollectorsSource().get(0),
                            net.get(0).get(j).getCollectorsTarget().get(i));
                    cell.addEdge(e);
                }
            }
            //Edges outputs
            for (int i = 0; i < outNum; i++) {
                Edge e = edgeFactory.genEdge(
                        net.get(outLayer).get(i).getCollectorsSource().get(0),
                        out.get(i).getCollectorsTarget().get(0));
                cell.addEdge(e);
            }

            //Edges Layers
            for (int i = 0; i < net.size() - 1; i++) {
                for (int j = 0; j < net.get(i).size(); j++) {
                    for (int k = 0; k < net.get(i + 1).size(); k++) {
                        Edge e = edgeFactory.genEdge(
                                net.get(i).get(j).getCollectorsSource().get(0),
                                net.get(i + 1).get(k).getCollectorsTarget().get(j));
                        cell.addEdge(e);
                    }
                }
            }

            for (Node node : cell.getInnerNodes()) {
                if (lockParNodes) {
                    node.setLockDec(0, true);
                }
                if (lockNodes) {
                    node.setLock(true);
                }
            }

            for (Edge edge : cell.getEdges()) {
                if (lockParEdges) {
                    edge.setLockWeight(true);
                }
                if (lockEdges) {
                    edge.setLockSource(true);
                    edge.setLockTarget(true);
                }
            }

            for (int i = 0; i < 0; i++) {
                Delay n = (Delay) nodeFactory.genNode("Delay");
                cell.addNode(n);


                connectsMinFreeInputs(cell);

                Edge edge = edgeFactory.genEdge(
                        n.getCollectorsSource().get(0),
                        getCollectorTargetFreeRnd(cell));

                cell.addEdge(edge);


            }

        } catch (Exception e1) {
            e1.printStackTrace();
        }
        cell.checkCellPrint();

        return cell;
    }
}