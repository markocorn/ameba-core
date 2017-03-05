package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by marko on 3/1/17.
 */
public class RemoveNodesGroup implements IMutateCell {
    FactoryCell cellFactory;
    Random random;
    int maxRemove;

    public RemoveNodesGroup(FactoryCell cellFactory, int maxRemove) {
        this.cellFactory = cellFactory;
        this.maxRemove = maxRemove;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        if (cell.getInnerNodes().size()<1) throw new Exception("Number of inner nodes must be grater than zero");
        ArrayList<ArrayList<Node>> group = cell.getGroup(cell.getInnerNodes().get(random.nextInt(cell.getInnerNodes().size())), random.nextInt(maxRemove));
        HashMap<String, ArrayList<Edge>> borderEdges = cell.getGroupBorderEdges(group);
        //Reconnect edges
        reconnectEdges(Double.class, cell, borderEdges);
        reconnectEdges(Integer.class, cell, borderEdges);
        reconnectEdges(Boolean.class, cell, borderEdges);
        //Remove nodes
        for (ArrayList<Node> nodes : group) {
            for (Node node : nodes) {
                cell.removeNode(node);
            }
        }
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
