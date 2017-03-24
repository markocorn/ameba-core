package ameba.core.reproductions.crossCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryEdge;
import ameba.core.factories.FactoryNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by marko on 3/21/17.
 */
public class TransferNodes implements ICrossCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;
    int maxNodes;

    /**
     * Transfer group of nodes from cell2 to cell1 where first the different group of nodes is removed.
     *
     * @param nodeFactory
     * @param cellFactory
     * @param edgeFactory
     * @param maxNodes
     */
    public TransferNodes(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory, int maxNodes) {
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        this.edgeFactory = edgeFactory;
        this.maxNodes = maxNodes;
        random = new Random();
    }

    @Override
    public Cell cross(Cell cell1, Cell cell2) throws Exception {
        Cell cell3 = cell1.clone(), cell4 = cell2.clone();

        //Remove group of nodes with edges from cell1
        ArrayList<ArrayList<Node>> group = cell1.getGroup(cell1.getInnerNodes().get(random.nextInt(cell1.getInnerNodes().size())), random.nextInt(maxNodes) + 1);
        HashMap<String, ArrayList<Edge>> borderEdges = cell1.getGroupEdgesBorder(group);
        HashMap<String, ArrayList<Edge>> innerEdges = cell1.getGroupEdgesInner(group);
        //Remove inner edges
        for (Edge edge : innerEdges.get("edgesDec")) {
            cell1.removeEdge(edge);
        }
        for (Edge edge : innerEdges.get("edgesInt")) {
            cell1.removeEdge(edge);
        }
        for (Edge edge : innerEdges.get("edgesBin")) {
            cell1.removeEdge(edge);
        }
        //Remove nodes
        for (ArrayList<Node> nodes : group) {
            for (Node node : nodes) {
                cell1.removeNode(node);
            }
        }

        //Add group of nodes and edges from cell2 to cell 1
        ArrayList<ArrayList<Node>> group2 = cell2.getGroup(cell2.getInnerNodes().get(random.nextInt(cell2.getInnerNodes().size())), random.nextInt(maxNodes - 1) + 2);
        HashMap<String, ArrayList<Edge>> borderEdges2 = cell2.getGroupEdgesBorder(group2);
        HashMap<String, ArrayList<Edge>> innerEdges2 = cell2.getGroupEdgesInner(group2);
        //Add new nodes to cell 1
        for (ArrayList<Node> nodes : group2) {
            for (Node node : nodes) {
                cell1.addNode(node);
            }
        }
        //Add inner edges
        for (Edge edge : innerEdges2.get("edgesDec")) {
            cell1.addEdgeNoVerification(edge);
        }
        for (Edge edge : innerEdges2.get("edgesInt")) {
            cell1.addEdgeNoVerification(edge);
        }
        for (Edge edge : innerEdges2.get("edgesBin")) {
            cell1.addEdgeNoVerification(edge);
        }

        //Reconnect edges
        reconnectEdgesInp(cell1, borderEdges.get("edgesInpDec"), borderEdges2.get("edgesInpDec"));
        reconnectEdgesInp(cell1, borderEdges.get("edgesInpInt"), borderEdges2.get("edgesInpInt"));
        reconnectEdgesInp(cell1, borderEdges.get("edgesInpBin"), borderEdges2.get("edgesInpBin"));

        reconnectEdgesOut(borderEdges.get("edgesOutDec"), borderEdges2.get("edgesOutDec"));
        reconnectEdgesOut(borderEdges.get("edgesOutInt"), borderEdges2.get("edgesOutInt"));
        reconnectEdgesOut(borderEdges.get("edgesOutBin"), borderEdges2.get("edgesOutBin"));

        cellFactory.connectsMinFreeInputs(cell1);
        if (cell1.checkCell().size() > 0) {
            //Remove group of nodes with edges from cell1
            group = cell3.getGroup(cell3.getInnerNodes().get(random.nextInt(cell3.getInnerNodes().size())), random.nextInt(maxNodes) + 1);
            borderEdges = cell3.getGroupEdgesBorder(group);
            innerEdges = cell3.getGroupEdgesInner(group);
            //Remove inner edges
            for (Edge edge : innerEdges.get("edgesDec")) {
                cell3.removeEdge(edge);
            }
            for (Edge edge : innerEdges.get("edgesInt")) {
                cell3.removeEdge(edge);
            }
            for (Edge edge : innerEdges.get("edgesBin")) {
                cell3.removeEdge(edge);
            }
            //Remove nodes
            for (ArrayList<Node> nodes : group) {
                for (Node node : nodes) {
                    cell3.removeNode(node);
                }
            }

            //Add group of nodes and edges from cell2 to cell 1
            group2 = cell4.getGroup(cell4.getInnerNodes().get(random.nextInt(cell4.getInnerNodes().size())), random.nextInt(maxNodes - 1) + 2);
            borderEdges2 = cell4.getGroupEdgesBorder(group2);
            innerEdges2 = cell4.getGroupEdgesInner(group2);
            //Add new nodes to cell 1
            for (ArrayList<Node> nodes : group2) {
                for (Node node : nodes) {
                    cell4.addNode(node);
                }
            }
            //Add inner edges
            for (Edge edge : innerEdges2.get("edgesDec")) {
                cell4.addEdgeNoVerification(edge);
            }
            for (Edge edge : innerEdges2.get("edgesInt")) {
                cell4.addEdgeNoVerification(edge);
            }
            for (Edge edge : innerEdges2.get("edgesBin")) {
                cell4.addEdgeNoVerification(edge);
            }

            //Reconnect edges
            reconnectEdgesInp(cell3, borderEdges.get("edgesInpDec"), borderEdges2.get("edgesInpDec"));
            reconnectEdgesInp(cell3, borderEdges.get("edgesInpInt"), borderEdges2.get("edgesInpInt"));
            reconnectEdgesInp(cell3, borderEdges.get("edgesInpBin"), borderEdges2.get("edgesInpBin"));

            reconnectEdgesOut(borderEdges.get("edgesOutDec"), borderEdges2.get("edgesOutDec"));
            reconnectEdgesOut(borderEdges.get("edgesOutInt"), borderEdges2.get("edgesOutInt"));
            reconnectEdgesOut(borderEdges.get("edgesOutBin"), borderEdges2.get("edgesOutBin"));

            cellFactory.connectsMinFreeInputs(cell1);
        }
        return cell1;
    }


    private void reconnectEdgesInp(Cell cell, ArrayList<Edge> borderEdges1, ArrayList<Edge> borderEdges2) throws Exception {
        int diff = borderEdges1.size() - borderEdges2.size();
        int same = Math.min(borderEdges1.size(), borderEdges2.size());
        Collections.shuffle(borderEdges2);
        for (int i = 0; i < same; i++) {
            Edge e1 = borderEdges1.get(i);
            Edge e2 = borderEdges2.get(i);
            e1.setTarget(e2.getTarget());
            e2.getTarget().addEdge(e1);
            System.out.println("1");
        }
        if (diff > 0) {
            for (int i = same; i < same + diff; i++) {
                cell.removeEdge(borderEdges1.get(i));
                System.out.println("2");
            }

        } else {
            for (int i = same; i < same - diff; i++) {
                borderEdges2.get(i).getTarget().removeEdge(borderEdges2.get(i));
                System.out.println("3");
            }
        }
    }

    private void reconnectEdgesOut(ArrayList<Edge> borderEdges1, ArrayList<Edge> borderEdges2) throws Exception {
        int diff = borderEdges1.size() - borderEdges2.size();
        int same = Math.min(borderEdges1.size(), borderEdges2.size());
        Collections.shuffle(borderEdges2);
        for (int i = 0; i < same; i++) {
            Edge e1 = borderEdges1.get(i);
            Edge e2 = borderEdges2.get(i);
            e1.setSource(e2.getSource());
            e2.getSource().removeEdge(e2);
            e2.getSource().addEdge(e1);
            System.out.println("4");
        }
        if (diff > 0) {
            for (int i = same; i < same - diff; i++) {
                borderEdges1.get(i).getTarget().removeEdge(borderEdges2.get(i));
                System.out.println("5");
            }
        } else {
            for (int i = same; i < same + diff; i++) {
                borderEdges2.get(i).getSource().removeEdge(borderEdges2.get(i));
                System.out.println("6");
            }
        }
    }
}

