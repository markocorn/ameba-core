package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.Node;
import ameba.core.factories.CellFactory;
import ameba.core.factories.NodeFactory;

import java.util.Random;

/**
 * Created by marko on 12/30/16.
 */
public class ReplaceNode implements IMutateCell {
    NodeFactory nodeFactory;
    CellFactory cellFactory;
    Random random;

    @Override
    public Cell mutate(Cell cell) {
        Node nodeOld = cellFactory.getNodeRndInner(cell);
        Node nodeNew = nodeFactory.genNodeRnd();
        //Output edges
        if (nodeOld.getOutputEdges().size() > nodeNew.getMaxOutputEdges()) {
            for (int i = 0; i < nodeNew.getMaxOutputEdges(); i++) {
                nodeNew.addOutputEdge(nodeOld.getOutputEdges().get(i));
                nodeOld.getOutputEdges().get(i).setSource(nodeNew);
            }
        }


        return null;
    }
}
