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
public class AddNodes implements ICrossCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;
    int maxNodes;

    /**
     * Add new nodes from cell2 to cell1
     *
     * @param nodeFactory
     * @param cellFactory
     * @param edgeFactory
     * @param maxNodes
     */
    public AddNodes(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory, int maxNodes) {
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        this.edgeFactory = edgeFactory;
        this.maxNodes = maxNodes;
        random = new Random();
    }

    @Override
    public Cell cross(Cell cell1, Cell cell2) throws Exception {
        ArrayList<ArrayList<Node>> group = cell2.getGroup(cell2.getInnerNodes().get(random.nextInt(cell2.getInnerNodes().size())), random.nextInt(maxNodes - 1) + 2);
        HashMap<String, ArrayList<Edge>> borderEdges = cell2.getGroupEdgesBorder(group);
        HashMap<String, ArrayList<Edge>> innerEdges = cell2.getGroupEdgesInner(group);
        //Add new nodes
        for (ArrayList<Node> nodes : group) {
            for (Node node : nodes) {
                cell1.addNode(node);
            }
        }
        //Add inner edges
        for (Edge edge : innerEdges.get("edgesDec")) {
            cell1.addEdgeNotSafe(edge);
        }
        for (Edge edge : innerEdges.get("edgesInt")) {
            cell1.addEdgeNotSafe(edge);
        }
        for (Edge edge : innerEdges.get("edgesBin")) {
            cell1.addEdgeNotSafe(edge);
        }

        //Reconnect border edges
        reconnectEdges(Cell.Signal.DECIMAL, cell1, borderEdges);
        reconnectEdges(Cell.Signal.INTEGER, cell1, borderEdges);
        reconnectEdges(Cell.Signal.BOOLEAN, cell1, borderEdges);

        return cell1;
    }


    private void reconnectEdges(Cell.Signal type, Cell cell, HashMap<String, ArrayList<Edge>> borderEdges) throws Exception {
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
            cellFactory.connectsMinFreeInputs(cell);
        }
    }
}
