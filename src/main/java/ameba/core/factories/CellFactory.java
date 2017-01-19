//package ameba.core.factories;
//
//import ameba.core.blocks.Cell;
//import ameba.core.blocks.connections.Edge;
//import ameba.core.blocks.nodes.Node;
//
//import java.util.ArrayList;
//import java.util.Random;
//
///**
// * Created by marko on 10/20/16.
// */
//public class CellFactory {
//    private CellFactorySettings cellFactorySettings;
//    private Random rndGen;
//    private NodeFactory nodeFactory;
//    private EdgeFactory edgeFactory;
//
//
//    public CellFactory(CellFactorySettings cellFactorySettings, NodeFactory nodeFactory, EdgeFactory edgeFactory) {
//        this.cellFactorySettings = cellFactorySettings;
//        this.nodeFactory = nodeFactory;
//        this.edgeFactory = edgeFactory;
//        rndGen = new Random();
//    }
//
//    public static String verifyCellConnections(Cell cell) {
//        String out = "OK";
//        for (Edge edge : cell.getEdges()) {
//            //Check source side of connections
//            for (Edge outEdge : edge.getSource().getOutputEdges()) {
//                out = "Edge: " + edge.toString() + " not registered as output edge of node:" + edge.getSource().toString();
//                if (outEdge == edge) {
//                    out = "OK";
//                    break;
//                }
//            }
//            //Check target side of connections
//            for (Edge outEdge : edge.getTarget().getInputEdges()) {
//                out = "Edge: " + edge.toString() + " not registered as input edge of node:" + edge.getTarget().toString();
//                if (outEdge == edge) {
//                    out = "OK";
//                    break;
//                }
//            }
//        }
//        return out;
//    }
//
//    /**
//     * Build cell with random number of nodes and randomly connected connections.
//     *
//     * @return generated cell.
//     */
//    public Cell genCellRnd() {
//        Cell cell = new Cell();
//        //Add nodes of type InputDec
//        //Add nodes of type OutputDec
//        for (int i = 0; i < cellFactorySettings.getNodeInputs(); i++) {
//            cell.addNode(nodeFactory.genNode("InputDec"));
//        }
//        for (int i = 0; i < cellFactorySettings.getNodeOutputs(); i++) {
//            cell.addNode(nodeFactory.genNode("OutputDec"));
//        }
//        //Determine initial number of nodes
//        int numNodes = genNmbNodesInitial();
//        //Add node of random type constrained by the node factory settings
//        for (int i = 0; i < numNodes; i++) {
//            cell.addNode(nodeFactory.genNodeRnd());
//        }
//        //Randomly connect nodes with connections
//        for (Node node : cell.getNodes()) {
//            //If node min input connection is violated connect node
//            while (node.getMinInputEdgesDifference() > 0) {
//                Node source = getNodeRndFreeOutput(cell);
//                Edge edge = edgeFactory.genEdge(source, node);
//                node.addEdgeInput(edge);
//                source.addOutputEdge(edge);
//            }
//        }
//        return cell;
//    }
//
//    /**
//     * Generate random number for the initial size of nodes in cell.
//     *
//     * @return Randomly generated number with constrains from cell settings.
//     */
//    private int genNmbNodesInitial() {
//        return rndGen.nextInt(cellFactorySettings.getNodeInitial());
//    }
//
//    /**
//     * Get node from cell that can add output edge to it's output connections.
//     *
//     * @param cell Cell to select node from.
//     * @return Selected node.
//     */
//    public Node getNodeRndFreeOutput(Cell cell) {
//        ArrayList<Node> nodes = new ArrayList<>();
//        for (Node node : cell.getNodes()) {
//            if (node.getMaxOutputEdgesDifference() > 0) {
//                nodes.add(node);
//            }
//        }
//        if (nodes.size() > 0) {
//            return nodes.get(rndGen.nextInt(nodes.size()));
//        } else return null;
//    }
//
//    /**
//     * Get node from cell that can add output edge to it's output connections not taking in account input argument node.
//     *
//     * @param cell Cell to select node from.
//     * @return Selected node.
//     * @Param node Node that can't be selected.
//     */
//    public Node getNodeRndFreeOutput(Cell cell, Node node) {
//        ArrayList<Node> nodes = new ArrayList<>();
//        for (Node node1 : cell.getNodes()) {
//            if (node1.getMaxOutputEdgesDifference() > 0 && !node1.equals(node)) {
//                nodes.add(node);
//            }
//        }
//        if (nodes.size() > 0) {
//            return nodes.get(rndGen.nextInt(nodes.size()));
//        } else return null;
//    }
//
//    /**
//     * Get node from cell that can add input edge to it's output connections not taking in account input argument node.
//     *
//     * @param cell Cell to select node from.
//     * @return Selected node.
//     * @Param node Node that can't be selected.
//     */
//    public Node getNodeRndFreeInput(Cell cell, Node node) {
//        ArrayList<Node> nodes = new ArrayList<>();
//        for (Node node1 : cell.getNodes()) {
//            if (node1.getMaxInputEdgesDifference() > 0 && !node1.equals(node)) {
//                nodes.add(node);
//            }
//        }
//        if (nodes.size() > 0) {
//            return nodes.get(rndGen.nextInt(nodes.size()));
//        } else return null;
//    }
//
//    /**
//     * @param cell
//     * @return
//     */
//    public Node getNodeRndInner(Cell cell) {
//        if (cell.getNodes().size() > 0) {
//            return cell.getNodes().get(rndGen.nextInt(cell.getNodes().size()));
//        }
//        return null;
//    }
//
//
//}
