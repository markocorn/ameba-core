package ameba.core.factories;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.*;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeBin;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.blocks.nodes.Node;
import ameba.core.blocks.nodes.types.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rits.cloning.Cloner;

import java.io.File;
import java.util.*;

/**
 * Created by marko on 10/20/16.
 */
public class FactoryCell {
    private FactoryCellSettings cellFactorySettings;
    private Random rndGen;
    private FactoryNode nodeFactory;
    private FactoryEdge edgeFactory;


    public FactoryCell(FactoryCellSettings cellFactorySettings, FactoryNode nodeFactory, FactoryEdge edgeFactory) {
        this.cellFactorySettings = cellFactorySettings;
        this.nodeFactory = nodeFactory;
        this.edgeFactory = edgeFactory;
        rndGen = new Random();
    }

    public static FactoryCell build() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(FactoryCell.class.getClassLoader().getResourceAsStream("cellFactorySettings.json"));
        return new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), FactoryNode.build(), FactoryEdge.build());
    }

    public static FactoryCell build(String filePathCell, String filePathNode, String filePathEdge) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File(filePathCell));
        return new FactoryCell(mapper.readValue(jsonSettings.get("cellFactorySettings").toString(), FactoryCellSettings.class), FactoryNode.build(filePathNode), FactoryEdge.build(filePathEdge));
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
        //AddDec input nodes
        for (int i = 0; i < cellFactorySettings.getNodeInpDec(); i++) {
            cell.addNode(new InputDec());
        }
        for (int i = 0; i < cellFactorySettings.getNodeInpInt(); i++) {
            cell.addNode(new InputInt());
        }
        for (int i = 0; i < cellFactorySettings.getNodeInpBin(); i++) {
            cell.addNode(new InputBin());
        }
        //AddDec output nodes
        for (int i = 0; i < cellFactorySettings.getNodeOutDec(); i++) {
            cell.addNode(new OutputDec());
        }
        for (int i = 0; i < cellFactorySettings.getNodeOutInt(); i++) {
            cell.addNode(new OutputInt());
        }
        for (int i = 0; i < cellFactorySettings.getNodeOutBin(); i++) {
            cell.addNode(new OutputBin());
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
                node = nodeFactory.genNodeRndCollectorSourceType(collectorSource.getType());
            } else {
                node = nodeFactory.genNodeRndCollectorSourceType(collectorTarget.getType());
            }
            if (node == null) throw new Exception("Not possible to generate nodes");
            cell.addNode(node);
        }
        connectsMinFreeInputs(cell, Cell.Signal.DECIMAL);
        connectsMinFreeInputs(cell, Cell.Signal.INTEGER);
        connectsMinFreeInputs(cell, Cell.Signal.BOOLEAN);
        if (cell.checkCell().size() > 0) {
            throw new Exception("Cell not properly generated. Cell Factory");
        }
        return cell;
    }

    public void connectsMinFreeInputs(Cell cell) throws Exception {
        connectsMinFreeInputs(cell, Cell.Signal.DECIMAL);
        connectsMinFreeInputs(cell, Cell.Signal.INTEGER);
        connectsMinFreeInputs(cell, Cell.Signal.BOOLEAN);
    }

    public void connectsMinFreeInputs(Cell cell, Cell.Signal type) throws Exception {
        while (true) {
            CollectorTarget collectorTarget = getCollectorTargetToConnectRnd(cell, type);
            if (collectorTarget == null) {
                if (cell.checkCell().size() > 0) {
                    int t = 0;
                }
                break;
            }
            CollectorSource collectorSource = getCollectorSourceRndNoNode(collectorTarget.getType(), cell, collectorTarget.getNodeAttached());
            if (collectorSource != null) {
                cell.addEdge(edgeFactory.genEdge(collectorTarget.getType(), collectorSource, collectorTarget));
            } else {
                switch (collectorTarget.getType()) {
                    case DECIMAL:
                        if (nodeFactory.nodeSettingsHashMap.get("ConstantDec").getProbability() > 0) {
                            cell.addNode(nodeFactory.genNode("ConstantDec"));
                        } else
                            throw new Exception("Cell can't be properly connected. Must allow the generation of ConstantDec nodes.");
                        break;
                    case INTEGER:
                        if (nodeFactory.nodeSettingsHashMap.get("ConstantInt").getProbability() > 0) {
                            cell.addNode(nodeFactory.genNode("ConstantInt"));
                        } else
                            throw new Exception("Cell can't be properly connected. Must allow the generation of ConstantInt nodes.");
                        break;
                    case BOOLEAN:
                        if (nodeFactory.nodeSettingsHashMap.get("ConstantBin").getProbability() > 0) {
                            cell.addNode(nodeFactory.genNode("ConstantBin"));
                        } else
                            throw new Exception("Cell can't be properly connected. Must allow the generation of ConstantBin nodes.");
                        break;
                }
            }
        }
    }

    public void reconnectEdges(Cell.Signal type, Cell cell, HashMap<String, ArrayList<Edge>> borderEdges) throws Exception {
        String ind = "";
        if (type.equals(Cell.Signal.DECIMAL)) ind = "Dec";
        if (type.equals(Cell.Signal.INTEGER)) ind = "Int";
        if (type.equals(Cell.Signal.BOOLEAN)) ind = "Bin";
        ArrayList<Edge> oldEdges = (ArrayList<Edge>) cell.getEdges(type);
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
        connectsMinFreeInputs(cell, type);
    }

    public CollectorSource getCollectorSourceRnd(Cell.Signal type, ArrayList<CollectorSource> collectorOuts) {
        ArrayList<CollectorSource> outs = new ArrayList<>();
        for (CollectorSource collectorOut : collectorOuts) {
            if (collectorOut.equals(type)) {
                outs.add(collectorOut);
            }
        }
        if (outs.size() > 0) return outs.get(rndGen.nextInt(outs.size()));
        return null;
    }

    /**
     * Get random output collector from cell.
     *
     * @param type
     * @param cell
     * @return
     * @throws Exception
     */
    public CollectorSource getCollectorTargetRnd(Cell.Signal type, Cell cell) throws Exception {
        ArrayList<CollectorSource> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorSource collector : node.getCollectorsSourceDec()) {
                if (collector.getType().equals(type)) {
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

    public CollectorSource getCollectorSourceRndNoNode(Cell.Signal type, Cell cell, Node node) throws Exception {
        ArrayList<CollectorSource> collectors = new ArrayList<>();
        for (Node node1 : cell.getNodes()) {
            for (CollectorSource collector : node1.getCollectorsSource()) {
                if (collector.getType().equals(type) && !node1.equals(node)) {
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
            collectors.addAll(node.getCollectorsSourceDec());
        }
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getCollectorsSourceInt());
        }
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getCollectorsSourceBin());
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }

    public CollectorSourceDec getCollectorSourceRndDec(Cell cell) throws Exception {
        ArrayList<CollectorSourceDec> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorSourceDec collector : node.getCollectorsSourceDec()) {
                collectors.add(collector);
            }
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }

    public CollectorSourceInt getCollectorSourceRndInt(Cell cell) throws Exception {
        ArrayList<CollectorSourceInt> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorSourceInt collector : node.getCollectorsSourceInt()) {
                collectors.add(collector);
            }
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }

    public CollectorSourceBin getCollectorSourceRndBin(Cell cell) throws Exception {
        ArrayList<CollectorSourceBin> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorSourceBin collector : node.getCollectorsSourceBin()) {
                collectors.add(collector);
            }
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }


    /**
     * Get random input collector of proper type and without edge.
     *
     * @param cell Cell to ISelect node from.
     * @return Selected node.
     * @Param node Node that can't be selected.
     */
    public CollectorTarget getCollectorInpRnd(Cell.Signal type, Cell cell) throws Exception {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorTarget collector : node.getCollectorsTargetDec()) {
                if (collector.getType().equals(type) && collector.getEdges().size() == 0) {
                    collectors.add(collector);
                }
            }
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

    public CollectorTarget getCollectorTargetToConnectRnd(Cell cell, Cell.Signal type) {
        if (getCollectorTargetToConnect(cell, type).size() > 0) {
            return getCollectorTargetToConnect(cell, type).get(rndGen.nextInt(getCollectorTargetToConnect(cell, type).size()));
        } else return null;
    }

    public ArrayList<CollectorTarget> getCollectorTargetToConnect(Cell cell) {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getCollectorsTargetToConnect());
        }
        return collectors;
    }

    public ArrayList<CollectorTarget> getCollectorTargetToConnect(Cell cell, Cell.Signal type) {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getCollectorsTargetToConnect(type));
        }
        return collectors;
    }

    /**
     * Get random input connector from node's where only one free input connector from node is taken into pool to be randomly selected.
     *
     * @param type
     * @param cell
     * @return
     * @throws Exception
     */
    public CollectorTarget getCollectorInpOneRnd(Cell.Signal type, Cell cell) throws Exception {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorTarget collector : node.getCollectorsTarget()) {
                if (collector.getType().equals(type) && collector.getEdges().size() == 0) {
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
        Cloner cloner = new Cloner();
        return cloner.deepClone(this);
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
                switch (node.get("cell").get("nodes").get(i).get("sourceCol").get(j).get("type").asText()) {
                    case "DECIMAL": {
                        sourceIntegerHashMap.put(node.get("cell").get("nodes").get(i).get("sourceCol").get(j).get("id").asInt(), newNode.getCollectorsSourceDec().get(count[0]));
                        count[0]++;
                    }
                    break;
                    case "INTEGER": {
                        sourceIntegerHashMap.put(node.get("cell").get("nodes").get(i).get("sourceCol").get(j).get("id").asInt(), newNode.getCollectorsSourceInt().get(count[1]));
                        count[1]++;
                    }
                    break;
                    case "BOOLEAN": {
                        sourceIntegerHashMap.put(node.get("cell").get("nodes").get(i).get("sourceCol").get(j).get("id").asInt(), newNode.getCollectorsSourceBin().get(count[2]));
                        count[2]++;
                    }
                    break;
                }
            }
            count = new int[]{0, 0, 0};
            for (int j = 0; j < node.get("cell").get("nodes").get(i).get("targetCol").size(); j++) {
                switch (node.get("cell").get("nodes").get(i).get("targetCol").get(j).get("type").asText()) {
                    case "DECIMAL": {
                        targetIntegerHashMap.put(node.get("cell").get("nodes").get(i).get("targetCol").get(j).get("id").asInt(), newNode.getCollectorsTargetDec().get(count[0]));
                        count[0]++;
                    }
                    break;
                    case "INTEGER": {
                        targetIntegerHashMap.put(node.get("cell").get("nodes").get(i).get("targetCol").get(j).get("id").asInt(), newNode.getCollectorsTargetInt().get(count[1]));
                        count[1]++;
                    }
                    break;
                    case "BOOLEAN": {
                        targetIntegerHashMap.put(node.get("cell").get("nodes").get(i).get("targetCol").get(j).get("id").asInt(), newNode.getCollectorsTargetBin().get(count[2]));
                        count[2]++;
                    }
                    break;
                }
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
            switch (node.get("cell").get("edges").get(i).get("type").asText()) {
                case "DECIMAL": {
                    edge = new EdgeDec(
                            (CollectorSourceDec) sourceIntegerHashMap.get(node.get("cell").get("edges").get(i).get("sourceCol").asInt()),
                            (CollectorTargetDec) targetIntegerHashMap.get(node.get("cell").get("edges").get(i).get("targetCol").asInt()),
                            node.get("cell").get("edges").get(i).get("weight").asDouble());
                }
                break;
                case "INTEGER": {
                    edge = new EdgeInt(
                            (CollectorSourceInt) sourceIntegerHashMap.get(node.get("cell").get("edges").get(i).get("sourceCol").asInt()),
                            (CollectorTargetInt) targetIntegerHashMap.get(node.get("cell").get("edges").get(i).get("targetCol").asInt()),
                            node.get("cell").get("edges").get(i).get("weight").asInt());
                }
                break;
                case "BOOLEAN": {
                    edge = new EdgeBin(
                            (CollectorSourceBin) sourceIntegerHashMap.get(node.get("cell").get("edges").get(i).get("sourceCol").asInt()),
                            (CollectorTargetBin) targetIntegerHashMap.get(node.get("cell").get("edges").get(i).get("targetCol").asInt()),
                            node.get("cell").get("edges").get(i).get("weight").asBoolean());
                }
                break;
            }
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


}