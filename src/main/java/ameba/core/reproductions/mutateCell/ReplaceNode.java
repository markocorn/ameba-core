//package ameba.core.reproductions.mutateCell;
//
//import ameba.core.blocks.Cell;
//import ameba.core.blocks.connections.Edge;
//import ameba.core.blocks.nodes.Node;
//import ameba.core.factories.CellFactory;
//import ameba.core.factories.EdgeFactory;
//import ameba.core.factories.NodeFactory;
//
//import java.util.Collections;
//import java.util.Random;
//
///**
// * Created by marko on 12/30/16.
// */
//public class ReplaceNode implements IMutateCell {
//    NodeFactory nodeFactory;
//    CellFactory cellFactory;
//    EdgeFactory edgeFactory;
//    Random random;
//
//    public ReplaceNode(NodeFactory nodeFactory, CellFactory cellFactory, EdgeFactory edgeFactory) {
//        this.nodeFactory = nodeFactory;
//        this.cellFactory = cellFactory;
//        this.edgeFactory = edgeFactory;
//    }
//
//    @Override
//    public Cell mutate(Cell cell) {
//        Cell old = cell.clone();
//        Node nodeOld = cellFactory.getNodeRndInner(cell);
////        Node nodeNew = nodeFactory.genNodeRnd();
//        Node nodeNew = nodeFactory.genNode("Integral");
//        //******************InputDec connections***************************************
//        Collections.shuffle(nodeOld.getInputEdges());
//        //Number of connections is smaller than minimum number of connections of new node
//        if (nodeOld.getInputEdges().size() < nodeNew.getMinInputEdges()) {
//            //Reconnect old connections to new node
//            for (int i = 0; i < nodeOld.getInputEdges().size(); i++) {
//                nodeNew.addEdgeInput(nodeOld.getInputEdges().get(i));
//                nodeOld.getInputEdges().get(i).setTarget(nodeNew);
//            }
//            //Remaining connections to minimum constrain satisfaction must be generated
//            for (int i = 0; i < nodeNew.getMinInputEdges() - nodeOld.getInputEdges().size(); i++) {
//                Node node = cellFactory.getNodeRndFreeOutput(cell, nodeNew);
//                if (node != null) {
//                    Edge edge = edgeFactory.genEdge(node, nodeNew);
//                    nodeNew.addEdgeInput(edge);
//                    edge.getSource().addOutputEdge(edge);
//                } else {
//                    cell = old;
//                    return null;
//                }
//            }
//        }
//        //Number of connections ig greater than minimum number of connections and smaller than max number of connections
//        if (nodeOld.getInputEdges().size() >= nodeNew.getMinInputEdges() && nodeOld.getInputEdges().size() <= nodeNew.getMaxInputEdgesDec()) {
//            //Reconnect old connections to new node
//            for (int i = 0; i < nodeOld.getInputEdges().size(); i++) {
//                nodeNew.addEdgeInput(nodeOld.getInputEdges().get(i));
//                nodeOld.getInputEdges().get(i).setTarget(nodeNew);
//            }
//        }
//        //Number of connections is greater than maximum number of connections of new node
//        if (nodeOld.getInputEdges().size() > nodeNew.getMaxInputEdgesDec()) {
//            //Reconnect old connections to new node
//            for (int i = 0; i < nodeNew.getMaxInputEdgesDec(); i++) {
//                nodeNew.addEdgeInput(nodeOld.getInputEdges().get(i));
//                nodeOld.getInputEdges().get(i).setTarget(nodeNew);
//            }
//            //Rest of the connections has to be deleted or reconnected
//            for (int i = nodeNew.getMaxInputEdgesDec(); i < nodeOld.getInputEdges().size(); i++) {
//                Edge edge = nodeOld.getInputEdges().get(i);
//                //Check if edge can be deleted
//                if (edge.getSource().getMinOutputEdges() < edge.getSource().getOutputEdges().size()) {
//                    edge.getSource().removeOutputEdge(edge);
//                    cell.removeEdge(edge);
//                } else {
//                    //Reconnect edge to new node
//                    Node node2 = cellFactory.getNodeRndFreeOutput(cell, edge.getSource());
//                    //If node exists reconnect
//                    if (node2 != null) {
//                        node2.addOutputEdge(edge);
//                        edge.setSource(node2);
//                    } else {
//                        //If node does not exist reverse all and return null
//                        cell = old;
//                        return null;
//                    }
//                }
//            }
//        }
//
//        //******************OutputDec connections***************************************
//        Collections.shuffle(nodeOld.getOutputEdges());
//        //Number of connections is smaller than minimum number of connections of new node
//        if (nodeOld.getOutputEdges().size() < nodeNew.getMinOutputEdges()) {
//            //Reconnect old connections to new node
//            for (int i = 0; i > nodeOld.getOutputEdges().size(); i++) {
//                nodeNew.addOutputEdge(nodeOld.getOutputEdges().get(i));
//                nodeOld.getOutputEdges().get(i).setSource(nodeNew);
//            }
//            //Remaining connections to minimum constrain satisfaction must be generated
//            for (int i = 0; i < nodeNew.getMinOutputEdges() - nodeOld.getOutputEdges().size(); i++) {
//                Node node = cellFactory.getNodeRndFreeInput(cell, nodeNew);
//                if (node != null) {
//                    Edge edge = edgeFactory.genEdge(node, nodeNew);
//                    nodeNew.addOutputEdge(edge);
//                    edge.getTarget().addEdgeInput(edge);
//                } else {
//                    cell = old;
//                    return null;
//                }
//            }
//        }
//        //Number of connections ig greater than minimum number of connections and smaller than max number of connections
//        if (nodeOld.getOutputEdges().size() >= nodeNew.getMinOutputEdges() && nodeOld.getOutputEdges().size() <= nodeNew.getMaxOutputEdges()) {
//            //Reconnect old connections to new node
//            for (int i = 0; i < nodeOld.getOutputEdges().size(); i++) {
//                nodeNew.addOutputEdge(nodeOld.getOutputEdges().get(i));
//                nodeOld.getOutputEdges().get(i).setSource(nodeNew);
//            }
//        }
//        //Number of connections is greater than maximum number of connections of new node
//        if (nodeOld.getOutputEdges().size() > nodeNew.getMaxOutputEdges()) {
//            //Reconnect old connections to new node
//            for (int i = 0; i > nodeNew.getMaxOutputEdges(); i++) {
//                nodeNew.addOutputEdge(nodeOld.getOutputEdges().get(i));
//                nodeOld.getOutputEdges().get(i).setSource(nodeNew);
//            }
//            //Rest of the connections has to be deleted or reconnected
//            for (int i = nodeNew.getMaxOutputEdges(); i < nodeOld.getOutputEdges().size(); i++) {
//                Edge edge = nodeOld.getOutputEdges().get(i);
//                //Check if edge can be deleted
//                if (edge.getTarget().getMaxInputEdgesDifference() > 0) {
//                    edge.getTarget().removeInputEdge(edge);
//                    cell.removeEdge(edge);
//                } else {
//                    //Reconnect edge to new node
//                    Node node2 = cellFactory.getNodeRndFreeOutput(cell, edge.getSource());
//                    //If node exists reconnect
//                    if (node2 != null) {
//                        node2.addOutputEdge(edge);
//                        edge.setSource(node2);
//                    } else {
//                        //If node does not exist reverse all and return null
//                        cell = old;
//                        return null;
//                    }
//                }
//            }
//        }
//        cell.getNodes().set(cell.getNodes().indexOf(nodeOld), nodeNew);
//        return cell;
//    }
//
//
//}
