package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.reproductions.Reproduction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by marko on 3/1/17.
 */
public class RemoveNodesGroup extends Reproduction implements IMutateCell {
    FactoryCell cellFactory;
    Random random;
    int[] nodesLimit;

    public RemoveNodesGroup(FactoryCell cellFactory, int[] nodesLimit, int probability, long seed) {
        super(probability);
        this.cellFactory = cellFactory;
        this.nodesLimit = nodesLimit;
        random = new Random(seed);
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        ArrayList<Node> nodesR = cell.getInnerNodesFullUnlocked();
        if (nodesR.size() < 2) throw new Exception("Number of inner unlocked nodes must be grater than one");
        ArrayList<ArrayList<Node>> group = cell.getGroup(nodesR.get(random.nextInt(nodesR.size())), random.nextInt(nodesLimit[1] - nodesLimit[0]) + nodesLimit[0]);
        HashMap<String, ArrayList<Edge>> borderEdges = cell.getGroupEdgesBorder(group);
        ArrayList<Edge> innerEdges = cell.getGroupEdgesInner(group);

        //Remove nodes
        for (ArrayList<Node> nodes : group) {
            for (Node node : nodes) {
                cell.removeNode(node);
            }
        }
        //Remove inner edges
        for (Edge edge : innerEdges) {
            cell.removeEdge(edge);
        }

        //Reconnect edges
        reconnectEdges(cell, borderEdges);

        cellFactory.connectsMinFreeInputs(cell);

        return cell;
    }

    private void reconnectEdges(Cell cell, HashMap<String, ArrayList<Edge>> borderEdges) throws Exception {
        int diff = borderEdges.get("edgesInp").size() - borderEdges.get("edgesOut").size();
        int same = Math.min(borderEdges.get("edgesInp").size(), borderEdges.get("edgesOut").size());
        Collections.shuffle(borderEdges.get("edgesInp"));
        Collections.shuffle(borderEdges.get("edgesOut"));
        for (int i = 0; i < same; i++) {
            //Change source node of the edge
            Edge edge1 = borderEdges.get("edgesInp").get(i);
            Edge edge2 = borderEdges.get("edgesOut").get(i);
            //Set new source for edge 2
            edge2.setSource(edge1.getSource());
            //Remove old edge from collector
            edge2.getSource().removeEdge(edge1);
            //add new edge
            edge2.getSource().addEdge(edge2);
            //Remove old edge from cell
            cell.removeEdge(edge1);
        }
        if (diff > 0) {
            for (int i = same; i < same + diff; i++) {
                Edge edge = borderEdges.get("edgesInp").get(i);
                //Remove old edge from collector
                edge.getSource().removeEdge(edge);
                //Remove old edge from cell
                cell.removeEdge(edge);
            }
        } else {
            for (int i = same; i < same - diff; i++) {
                Edge edge = borderEdges.get("edgesOut").get(i);
                //Remove edge from target collector later it will be randomly reconnected in the cell
                edge.getTarget().removeEdge(edge);
                cell.removeEdge(edge);
            }
        }

    }


}
