package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryEdge;
import ameba.core.factories.FactoryNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class AddNodesGroup implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;
    int numNodes;

    public AddNodesGroup(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory, int numNodes) {
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory.clone();
        this.edgeFactory = edgeFactory;
        random = new Random();
        this.numNodes = numNodes;
        this.cellFactory.getCellFactorySettings().setNodeInitial(new Integer[]{numNodes * 10, numNodes * 10});

        this.cellFactory.getCellFactorySettings().setNodeInpDec(1);
        this.cellFactory.getCellFactorySettings().setNodeInpInt(1);
        this.cellFactory.getCellFactorySettings().setNodeInpBin(1);

        this.cellFactory.getCellFactorySettings().setNodeOutDec(1);
        this.cellFactory.getCellFactorySettings().setNodeOutInt(1);
        this.cellFactory.getCellFactorySettings().setNodeOutBin(1);
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        Cell cellBase = cellFactory.genCellRnd();
        ArrayList<ArrayList<Node>> group = cellBase.getGroup(cellBase.getInnerNodes().get(random.nextInt(cellBase.getInnerNodes().size())), numNodes);
        HashMap<String, ArrayList<Edge>> borderEdges = cellBase.getGroupEdgesBorder(group);
        HashMap<String, ArrayList<Edge>> innerEdges = cellBase.getGroupEdgesInner(group);
        //Add new nodes
        for (ArrayList<Node> nodes : group) {
            for (Node node : nodes) {
                cell.addNode(node);
            }
        }
        //Reconnect border edges
        reconnectEdges(Double.class, cell, borderEdges);
        reconnectEdges(Integer.class, cell, borderEdges);
        reconnectEdges(Boolean.class, cell, borderEdges);

        //Add inner edges of group
        for (Edge edge : innerEdges.get("edgesDec")) {
            cell.addEdge(edge);
        }
        for (Edge edge : innerEdges.get("edgesInt")) {
            cell.addEdge(edge);
        }
        for (Edge edge : innerEdges.get("edgesBin")) {
            cell.addEdge(edge);
        }


        //Connect free remaining nodes
        cellFactory.connectsMinFreeInputs(cell);

        return cell;
    }

    private void reconnectEdges(Class type, Cell cell, HashMap<String, ArrayList<Edge>> borderEdges) throws Exception {
        String ind = "";
        if (type.isAssignableFrom(Double.class)) ind = "Dec";
        if (type.isAssignableFrom(Integer.class)) ind = "Int";
        if (type.isAssignableFrom(Boolean.class)) ind = "Bin";
        if (ind.equals("")) throw new Exception("Cant reconnect edges of unknown type: " + type.getSimpleName());
        ArrayList<Edge> oldEdges = cell.getEdges(type);
        int diff = borderEdges.get("edgesInp" + ind).size() - oldEdges.size();
        int same = Math.min(borderEdges.get("edgesInp" + ind).size(), oldEdges.size());
        Collections.shuffle(cell.getEdges());
        ArrayList<Edge> remains = new ArrayList<>();
        for (int i = 0; i < same; i++) {
            Edge edge1 = cell.getEdges(type).get(i);
            Edge edge2 = borderEdges.get("edgesInp" + ind).get(i);
            //Set new source for edge 2
            edge2.setSource(edge1.getSource());
            edge2.getSource().addEdge(edge2);
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
            Edge edge1 = borderEdges.get("edgesOut" + ind).get(i);
            Edge edge2 = remains.get(i);
            edge1.setSource(edge2.getSource());
            edge1.getSource().addEdge(edge1);
            edge1.getSource().removeEdge(edge2);

        }
        if (diff > 0) {
            for (int i = same; i < same + diff; i++) {
                borderEdges.get("edgesOut" + ind).get(i).getSource().removeEdge(borderEdges.get("edgesOut" + ind).get(i));
            }
        } else {
            for (int i = same; i < same - diff; i++) {
                remains.get(i).getTarget().removeEdge(remains.get(i));
            }
        }
    }
}
