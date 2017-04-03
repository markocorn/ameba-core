package ameba.core.reproductions.crossCell;

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
 * Created by marko on 3/21/17.
 */
public class AddNodes extends Reproduction implements ICrossCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;
    int[] nodesLimit;

    /**
     * Add new nodes from cell2 to cell1
     *
     * @param nodeFactory
     * @param cellFactory
     * @param edgeFactory
     */
    public AddNodes(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory, int[] nodesLimit, int probability) {
        super(probability);
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        this.edgeFactory = edgeFactory;
        this.nodesLimit = nodesLimit;
        random = new Random();
    }

    @Override
    public Cell cross(Cell cell1, Cell cell2) throws Exception {
        ArrayList<ArrayList<Node>> group = cell2.getGroup(cell2.getInnerNodes().get(random.nextInt(cell2.getInnerNodes().size())), random.nextInt(nodesLimit[1] - nodesLimit[0]) + nodesLimit[0]);
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
        cellFactory.reconnectEdges(Cell.Signal.DECIMAL, cell1, borderEdges);
        cellFactory.reconnectEdges(Cell.Signal.INTEGER, cell1, borderEdges);
        cellFactory.reconnectEdges(Cell.Signal.BOOLEAN, cell1, borderEdges);

        return cell1;
    }
}
