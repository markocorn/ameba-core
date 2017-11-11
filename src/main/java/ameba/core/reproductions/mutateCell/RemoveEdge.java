package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryNode;
import ameba.core.reproductions.Reproduction;

import java.util.ArrayList;
import java.util.Random;

public class RemoveEdge extends Reproduction implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    Random random;

    public RemoveEdge(FactoryNode nodeFactory, FactoryCell cellFactory, int probability) {
        super(probability);
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Select random edge that can be removed
        ArrayList<Edge> list = cellFactory.getEdgesCanBeRemoved(cell);
        if (list.size() < 1) {
            throw new Exception("There is no edge that can be removed.");
        }
        Edge edge = list.get(random.nextInt(list.size()));
        cell.removeEdge(edge);
        return cell;
    }
}
