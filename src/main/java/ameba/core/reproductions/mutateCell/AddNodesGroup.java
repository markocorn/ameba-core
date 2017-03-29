package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryEdge;
import ameba.core.factories.FactoryNode;
import ameba.core.reproductions.Reproduction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class AddNodesGroup extends Reproduction implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;
    int[] nodesLimits;

    public AddNodesGroup(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory, int[] nodesLimits, int probability) {
        super(probability);
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory.clone();
        this.edgeFactory = edgeFactory;
        random = new Random();
        this.nodesLimits = nodesLimits;
        this.cellFactory.getCellFactorySettings().setNodeInitial(new Integer[]{nodesLimits[1] * 10, nodesLimits[1] * 10});

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
        ArrayList<ArrayList<Node>> group = cellBase.getGroup(cellBase.getInnerNodes().get(random.nextInt(cellBase.getInnerNodes().size())), random.nextInt(nodesLimits[1] - nodesLimits[0]) + nodesLimits[1]);
        HashMap<String, ArrayList<Edge>> borderEdges = cellBase.getGroupEdgesBorder(group);
        HashMap<String, ArrayList<Edge>> innerEdges = cellBase.getGroupEdgesInner(group);
        //Add new nodes
        for (ArrayList<Node> nodes : group) {
            for (Node node : nodes) {
                cell.addNode(node);
            }
        }
        //Add inner edges
        for (Edge edge : innerEdges.get("edgesDec")) {
            cell.addEdgeNotSafe(edge);
        }
        for (Edge edge : innerEdges.get("edgesInt")) {
            cell.addEdgeNotSafe(edge);
        }
        for (Edge edge : innerEdges.get("edgesBin")) {
            cell.addEdgeNotSafe(edge);
        }

        //Reconnect border edges
        reconnectEdges(Cell.Signal.DECIMAL, cell, borderEdges);
        reconnectEdges(Cell.Signal.INTEGER, cell, borderEdges);
        reconnectEdges(Cell.Signal.BOOLEAN, cell, borderEdges);

        return cell;
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
