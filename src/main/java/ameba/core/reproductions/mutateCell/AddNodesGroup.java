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

    public AddNodesGroup(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory, int[] nodesLimits, int probability, long seed) {
        super(probability);
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory.clone();
        this.edgeFactory = edgeFactory;
        random = new Random(seed);
        this.nodesLimits = nodesLimits;
        this.cellFactory.getCellFactorySettings().setNodeInitial(new Integer[]{nodesLimits[1] * 10, nodesLimits[1] * 10});

        this.cellFactory.getCellFactorySettings().setNodeInp(1);

        this.cellFactory.getCellFactorySettings().setNodeOut(1);
        ;
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        Cell cellBase = cellFactory.genCellRnd();
        ArrayList<ArrayList<Node>> group = cellBase.getGroup(cellBase.getInnerNodes().get(random.nextInt(cellBase.getInnerNodes().size())), random.nextInt(nodesLimits[1] - nodesLimits[0]) + nodesLimits[1]);
        HashMap<String, ArrayList<Edge>> borderEdges = cellBase.getGroupEdgesBorder(group);
        ArrayList<Edge> innerEdges = cellBase.getGroupEdgesInner(group);
        //Add new nodes
        for (ArrayList<Node> nodes : group) {
            for (Node node : nodes) {
                cell.addNode(node);
            }
        }
        //Add inner edges
        for (Edge edge : innerEdges) {
            cell.addEdgeNotSafe(edge);
        }

        //Reconnect border edges
        cellFactory.reconnectEdges(cell, borderEdges);
        return cell;
    }
}
