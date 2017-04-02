package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryEdge;
import ameba.core.factories.FactoryNode;
import ameba.core.reproductions.Reproduction;

import java.util.ArrayList;
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
        cellFactory.reconnectEdges(Cell.Signal.DECIMAL, cell, borderEdges);
        cellFactory.reconnectEdges(Cell.Signal.INTEGER, cell, borderEdges);
        cellFactory.reconnectEdges(Cell.Signal.BOOLEAN, cell, borderEdges);

        return cell;
    }
}
