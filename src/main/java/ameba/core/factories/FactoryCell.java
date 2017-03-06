package ameba.core.factories;

import ameba.core.blocks.Cell;
import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.nodes.*;
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
            //Get collector that must be connected
            CollectorInp collectorInp = getCollectorInpMinRnd(cell);
            Node node;
            if (collectorInp == null) {
                //if no input collector that must be connected exists add a node with one
                CollectorOut collectorOut = getCollectorOutRnd(cell);
                node = nodeFactory.genNodeRndInpColType(collectorOut.getType());
            } else {
                node = nodeFactory.genNodeRndOutColType(collectorInp.getType());
            }
            if (node == null) throw new Exception("Not possible to generate nodes");
            cell.addNode(node);
        }
        connectsMinFreeInputs(cell);


        return cell;
    }

    public void connectsMinFreeInputs(Cell cell) throws Exception {
        while (true) {
            CollectorInp collectorInp = getCollectorInpMinRnd(cell);
            if (collectorInp == null) {
                break;
            }

            CollectorOut collectorOut = getCollectorOutRndNoNode(collectorInp.getType(), cell, collectorInp.getNodeAttached());

            if (collectorOut != null) {
                cell.addEdge(edgeFactory.genEdge(collectorInp.getType(), collectorOut, collectorInp));
            } else {
                if (collectorInp.getType().isAssignableFrom(Double.class)) {
                    if (nodeFactory.settings.get("ConstantDec").getAvailable()) {
                        cell.addNode(nodeFactory.genNodeRndPar("ConstantDec"));
                    } else
                        throw new Exception("Cell can't be properly connected. Must allow the generation ov ConstantDec nodes.");
                }
                if (collectorInp.getType().isAssignableFrom(Integer.class)) {
                    if (nodeFactory.settings.get("ConstantInt").getAvailable()) {
                        cell.addNode(nodeFactory.genNodeRndPar("ConstantInt"));
                    } else
                        throw new Exception("Cell can't be properly connected. Must allow the generation ov ConstantInt nodes.");
                }
                if (collectorInp.getType().isAssignableFrom(Boolean.class)) {
                    if (nodeFactory.settings.get("ConstantBin").getAvailable()) {
                        cell.addNode(nodeFactory.genNodeRndPar("ConstantBin"));
                    } else
                        throw new Exception("Cell can't be properly connected. Must allow the generation ov ConstantBin nodes.");
                }
            }
        }
    }

    public CollectorOut getRndCollector(Class type, ArrayList<CollectorOut> collectorOuts) {
        ArrayList<CollectorOut> outs = new ArrayList<>();
        for (CollectorOut collectorOut : collectorOuts) {
            if (collectorOut.getType().isAssignableFrom(type)) {
                outs.add(collectorOut);
            }
        }
        if (outs.size() > 0) return outs.get(rndGen.nextInt(outs.size()));
        return null;
    }

    /**
     * Generate random number for the initial size of nodes in cell.
     *
     * @return Randomly generated number with constrains from cell settings.
     */
    public int genNmbNodesInitial() {
        if (Objects.equals(cellFactorySettings.getNodeInitial()[1], cellFactorySettings.getNodeInitial()[0])) {
            return cellFactorySettings.getNodeInitial()[1];
        } else {
            return rndGen.nextInt(cellFactorySettings.getNodeInitial()[1] - cellFactorySettings.getNodeInitial()[0]) + cellFactorySettings.getNodeInitial()[0];
        }
    }

    /**
     * Get random output collector from cell.
     *
     * @param type
     * @param cell
     * @return
     * @throws Exception
     */
    public CollectorOut getCollectorOutRnd(Class type, Cell cell) throws Exception {
        ArrayList<CollectorOut> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorOut collector : node.getOutCollectorsDec()) {
                if (collector.getSignal().gettClass().equals(type)) {
                    collectors.add(collector);
                }
            }
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }

    public CollectorOut getCollectorOutRndNoNode(Class type, Cell cell, Node node) throws Exception {
        ArrayList<CollectorOut> collectors = new ArrayList<>();
        for (Node node1 : cell.getNodes()) {
            for (CollectorOut collector : node1.getOutCollectors()) {
                if (collector.getSignal().gettClass().equals(type) && !node1.equals(node)) {
                    collectors.add(collector);
                }
            }
        }
        if (collectors.size() > 0) {
            return collectors.get(rndGen.nextInt(collectors.size()));
        } else return null;
    }

    public CollectorOut getCollectorOutRnd(Cell cell) throws Exception {
        ArrayList<CollectorOut> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorOut collector : node.getOutCollectorsDec()) {
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
     * @param cell Cell to select node from.
     * @return Selected node.
     * @Param node Node that can't be selected.
     */
    public CollectorInp getCollectorInpRnd(Class type, Cell cell) throws Exception {
        ArrayList<CollectorInp> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorInp collector : node.getInpCollectorsDec()) {
                if (collector.getSignal().gettClass().equals(type) && collector.getEdges().size() == 0) {
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
    public CollectorInp getCollectorInpMinRnd(Cell cell) throws Exception {
        ArrayList<CollectorInp> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            collectors.addAll(node.getInpCollectorsMinConnect());
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
    public CollectorInp getCollectorInpOneRnd(Class type, Cell cell) throws Exception {
        ArrayList<CollectorInp> collectors = new ArrayList<>();
        for (Node node : cell.getNodes()) {
            for (CollectorInp collector : node.getInpCollectors()) {
                if (collector.getSignal().gettClass().equals(type) && collector.getEdges().size() == 0) {
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
