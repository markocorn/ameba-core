package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryEdge;
import ameba.core.factories.FactoryNode;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class AddNodesGroup implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Integer[] nodesLimits;

    Random random;

    public AddNodesGroup(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory, Integer[] nodesLimits) {
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        this.edgeFactory = edgeFactory;
        this.nodesLimits = nodesLimits;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        int numNodes = random.nextInt(nodesLimits[1]-nodesLimits[0])+nodesLimits[0];



        return cell;
    }
}
