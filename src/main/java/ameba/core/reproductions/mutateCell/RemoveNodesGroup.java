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

    public RemoveNodesGroup(FactoryCell cellFactory, int[] nodesLimit, int probability) {
        super(probability);
        this.cellFactory = cellFactory;
        this.nodesLimit = nodesLimit;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        ArrayList<Node> nodesR = cell.getInnerNodesFullUnlocked();
        if (nodesR.size() < 2) throw new Exception("Number of inner unlocked nodes must be grater than one");
        ArrayList<ArrayList<Node>> group = cell.getGroup(nodesR.get(random.nextInt(nodesR.size())), random.nextInt(nodesLimit[1] - nodesLimit[0]) + nodesLimit[0]);
        HashMap<String, ArrayList<Edge>> borderEdges = cell.getGroupEdgesBorder(group);
        HashMap<String, ArrayList<Edge>> innerEdges = cell.getGroupEdgesInner(group);

        //Remove nodes
        for (ArrayList<Node> nodes : group) {
            for (Node node : nodes) {
                cell.removeNode(node);
            }
        }
        //Remove inner edges
        for (Edge edge : innerEdges.get("edgesDec")) {
            cell.removeEdge(edge);
        }
        for (Edge edge : innerEdges.get("edgesInt")) {
            cell.removeEdge(edge);
        }
        for (Edge edge : innerEdges.get("edgesBin")) {
            cell.removeEdge(edge);
        }

        //Reconnect edges
        reconnectEdges(Double.class, cell, borderEdges);
        reconnectEdges(Integer.class, cell, borderEdges);
        reconnectEdges(Boolean.class, cell, borderEdges);

        cellFactory.connectsMinFreeInputs(cell);

        return cell;
    }

    private void reconnectEdges(Class type, Cell cell, HashMap<String, ArrayList<Edge>> borderEdges) throws Exception {
        String ind = "";
        if (type.isAssignableFrom(Double.class)) ind = "Dec";
        if (type.isAssignableFrom(Integer.class)) ind = "Int";
        if (type.isAssignableFrom(Boolean.class)) ind = "Bin";
        if (ind.equals("")) throw new Exception("Cant reconnect edges of unknown type: " + type.getSimpleName());
        int diff = borderEdges.get("edgesInp" + ind).size() - borderEdges.get("edgesOut" + ind).size();
        int same = Math.min(borderEdges.get("edgesInp" + ind).size(), borderEdges.get("edgesOut" + ind).size());
        Collections.shuffle(borderEdges.get("edgesInp" + ind));
        Collections.shuffle(borderEdges.get("edgesOut" + ind));
        for (int i = 0; i < same; i++) {
            //Change source node of the edge
            Edge edge1 = borderEdges.get("edgesInp" + ind).get(i);
            Edge edge2 = borderEdges.get("edgesOut" + ind).get(i);
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
                Edge edge = borderEdges.get("edgesInp" + ind).get(i);
                //Remove old edge from collector
                edge.getSource().removeEdge(edge);
                //Remove old edge from cell
                cell.removeEdge(edge);
            }
        } else {
            for (int i = same; i < same - diff; i++) {
                Edge edge = borderEdges.get("edgesOut" + ind).get(i);
                //Remove edge from target collector later it will be randomly reconnected in the cell
                edge.getTarget().removeEdge(edge);
                cell.removeEdge(edge);
            }
        }

    }


}
