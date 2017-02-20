package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryEdge;
import ameba.core.factories.FactoryNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by marko on 12/30/16.
 */
public class ReplaceNode implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;

    public ReplaceNode(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory) {
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        this.edgeFactory = edgeFactory;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        Cell old = cell.clone();
        Node nodeOld = cellFactory.getNodeRndInner(cell);
        Node nodeNew = nodeFactory.genNodeRnd();

        ArrayList<CollectorInp> inpColOldDec = nodeOld.getInpCollectorConnected(Double.class);
        ArrayList<CollectorInp> inpColOldInt = nodeOld.getInpCollectorConnected(Integer.class);
        ArrayList<CollectorInp> inpColOldBin = nodeOld.getInpCollectorConnected(Boolean.class);
        ArrayList<CollectorOut> outColOldDec = nodeOld.getOutCollectorConnected(Double.class);
        ArrayList<CollectorOut> outColOldInt = nodeOld.getOutCollectorConnected(Integer.class);
        ArrayList<CollectorOut> outColOldBin = nodeOld.getOutCollectorConnected(Boolean.class);

        ArrayList<CollectorInp> inpColNewDec = nodeNew.getInpColLimitDec()[0];
        ArrayList<CollectorInp> inpColNewInt = nodeNew.getInpCollector(Integer.class);
        ArrayList<CollectorInp> inpColNewBin = nodeNew.getInpCollector(Boolean.class);
        ArrayList<CollectorOut> outColNewDec = nodeNew.getOutCollector(Double.class);
        ArrayList<CollectorOut> outColNewInt = nodeNew.getOutCollector(Integer.class);
        ArrayList<CollectorOut> outColNewBin = nodeNew.getOutCollector(Boolean.class);

        /**
         * Decimal type of connections
         */
        Collections.shuffle(inpColOldDec);
        int diff = inpColOldDec.size() - inpColNewDec.size();
        int same = Math.abs(diff);
        for (int i = 0; i < same; i++) {
            Edge edge = inpColOldDec.get(i).getEdges().get(0);
            edge.setTarget(inpColNewDec.get(i));
            inpColNewDec.get(i).addEdge(edge);
        }
        //Left old input collectors remove edges from cell
        for (int i = 0; i < diff; i++) {


        }
        //Left new input collectors
        for (int i = 0; i < -diff; i++) {

        }


//        //Number of old connectors is smaller than minimum number of conectivity of new node
//        if (nodeOld.getInputEdges().size() < nodeNew.getMinInpCollectors()) {
//            //Reconnect old conectivity to new node
//            for (int i = 0; i < nodeOld.getInputEdges().size(); i++) {
//                nodeNew.addEdgeInput(nodeOld.getInputEdges().get(i));
//                nodeOld.getInputEdges().get(i).setTarget(nodeNew);
//            }
//            //Remaining conectivity to minimum constrain satisfaction must be generated
//            for (int i = 0; i < nodeNew.getMinInpCollectors() - nodeOld.getInputEdges().size(); i++) {
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
//        //Number of conectivity ig greater than minimum number of conectivity and smaller than max number of conectivity
//        if (nodeOld.getInputEdges().size() >= nodeNew.getMinInpCollectors() && nodeOld.getInputEdges().size() <= nodeNew.getMaxInputEdgesDec()) {
//            //Reconnect old conectivity to new node
//            for (int i = 0; i < nodeOld.getInputEdges().size(); i++) {
//                nodeNew.addEdgeInput(nodeOld.getInputEdges().get(i));
//                nodeOld.getInputEdges().get(i).setTarget(nodeNew);
//            }
//        }
//        //Number of conectivity is greater than maximum number of conectivity of new node
//        if (nodeOld.getInputEdges().size() > nodeNew.getMaxInputEdgesDec()) {
//            //Reconnect old conectivity to new node
//            for (int i = 0; i < nodeNew.getMaxInputEdgesDec(); i++) {
//                nodeNew.addEdgeInput(nodeOld.getInputEdges().get(i));
//                nodeOld.getInputEdges().get(i).setTarget(nodeNew);
//            }
//            //Rest of the conectivity has to be deleted or reconnected
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
//        //******************OutputDec conectivity***************************************
//        Collections.shuffle(nodeOld.getOutputEdges());
//        //Number of conectivity is smaller than minimum number of conectivity of new node
//        if (nodeOld.getOutputEdges().size() < nodeNew.getMinOutputEdges()) {
//            //Reconnect old conectivity to new node
//            for (int i = 0; i > nodeOld.getOutputEdges().size(); i++) {
//                nodeNew.addOutputEdge(nodeOld.getOutputEdges().get(i));
//                nodeOld.getOutputEdges().get(i).setSource(nodeNew);
//            }
//            //Remaining conectivity to minimum constrain satisfaction must be generated
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
//        //Number of conectivity ig greater than minimum number of conectivity and smaller than max number of conectivity
//        if (nodeOld.getOutputEdges().size() >= nodeNew.getMinOutputEdges() && nodeOld.getOutputEdges().size() <= nodeNew.getMaxOutCollectors()) {
//            //Reconnect old conectivity to new node
//            for (int i = 0; i < nodeOld.getOutputEdges().size(); i++) {
//                nodeNew.addOutputEdge(nodeOld.getOutputEdges().get(i));
//                nodeOld.getOutputEdges().get(i).setSource(nodeNew);
//            }
//        }
//        //Number of conectivity is greater than maximum number of conectivity of new node
//        if (nodeOld.getOutputEdges().size() > nodeNew.getMaxOutCollectors()) {
//            //Reconnect old conectivity to new node
//            for (int i = 0; i > nodeNew.getMaxOutCollectors(); i++) {
//                nodeNew.addOutputEdge(nodeOld.getOutputEdges().get(i));
//                nodeOld.getOutputEdges().get(i).setSource(nodeNew);
//            }
//            //Rest of the conectivity has to be deleted or reconnected
//            for (int i = nodeNew.getMaxOutCollectors(); i < nodeOld.getOutputEdges().size(); i++) {
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
        cell.getNodes().set(cell.getNodes().indexOf(nodeOld), nodeNew);
        return cell;
    }


}
