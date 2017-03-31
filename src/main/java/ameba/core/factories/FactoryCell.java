package ameba.core.factories;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.*;
import ameba.core.blocks.nodes.Node;
import ameba.core.blocks.nodes.types.*;
import com.rits.cloning.Cloner;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

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

    public FactoryCellSettings getCellFactorySettings() {
        return cellFactorySettings;
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
            CollectorTarget collectorTarget = getCollectorTargetMinRnd(cell);
            if (collectorTarget == null) {
                // if there is no collector fine one on the source side of nodes
                CollectorSource collectorSource = getCollectorSourceRnd(cell);
                if (collectorSource == null) throw new Exception("Can't generate nod with no initial nodes");
                node = nodeFactory.genNodeRndCollectorSourceType(collectorSource.getType());
            } else {
                node = nodeFactory.genNodeRndCollectorSourceType(collectorTarget.getType());
            }
            if (node == null) throw new Exception("Not possible to generate nodes");
            cell.addNode(node);
        }
        connectsMinFreeInputs(cell);

        return cell;
    }

    public void connectsMinFreeInputs(Cell cell) throws Exception {
        while (true) {
            CollectorTarget collectorTarget = getCollectorTargetMinRnd(cell);
            if (collectorTarget == null) {
                break;
            }

            CollectorSource collectorSource = getCollectorSourceRndNoNode(collectorTarget.getType(), cell, collectorTarget.getNodeAttached());

            if (collectorSource != null) {
                cell.addEdge(edgeFactory.genEdge(collectorTarget.getType(), collectorSource, collectorTarget));
            } else {
                switch (collectorTarget.getType()) {
                    case DECIMAL:
                        if (nodeFactory.nodeSettingsHashMap.get("ConstantDec").getAvailable()) {
                            cell.addNode(nodeFactory.genNodeRndPar("ConstantDec"));
                        } else
                            throw new Exception("Cell can't be properly connected. Must allow the generation ov ConstantDec nodes.");
                        break;
                    case INTEGER:
                        if (nodeFactory.nodeSettingsHashMap.get("ConstantInt").getAvailable()) {
                            cell.addNode(nodeFactory.genNodeRndPar("ConstantInt"));
                        } else
                            throw new Exception("Cell can't be properly connected. Must allow the generation ov ConstantInt nodes.");
                        break;
                    case BOOLEAN:
                        if (nodeFactory.nodeSettingsHashMap.get("ConstantBin").getAvailable()) {
                            cell.addNode(nodeFactory.genNodeRndPar("ConstantBin"));
                        } else
                            throw new Exception("Cell can't be properly connected. Must allow the generation ov ConstantBin nodes.");
                        break;
                }
            }
        }
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

    public CollectorTarget getCollectorTargetMinRnd(Cell cell) {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getCollectorsTargetMinConnect());
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }

    public CollectorTargetDec getCollectorTargetMinRndDec(Cell cell) throws Exception {
        ArrayList<CollectorTargetDec> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getCollectorsTargetMinConnectDec());
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }

    public CollectorTargetInt getCollectorTargetMinRndInt(Cell cell) throws Exception {
        ArrayList<CollectorTargetInt> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getCollectorsTargetMinConnectInt());
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }

    public CollectorTargetBin getCollectorTargetMinRndBin(Cell cell) throws Exception {
        ArrayList<CollectorTargetBin> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getCollectorsTargetMinConnectBin());
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
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
    public Node getNodeRndInner(Cell cell) {
        if (cell.getNodes().size() > 0) {
            return cell.getInnerNodes().get(rndGen.nextInt(cell.getInnerNodes().size()));
        }
        return null;
    }

    public FactoryCell clone() {
        Cloner cloner = new Cloner();
        return cloner.deepClone(this);
    }


}
