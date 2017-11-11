package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.edges.Edge;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryNode;
import ameba.core.reproductions.Reproduction;

import java.util.Random;

public class MoveSourceEdge extends Reproduction implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    Random random;

    public MoveSourceEdge(FactoryNode nodeFactory, FactoryCell cellFactory, int probability) {
        super(probability);
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Select random edge
        Edge edge = cell.getEdgesUnlockedSource().get(random.nextInt(cell.getEdgesUnlockedSource().size()));
        //Find new source to reconnect
        CollectorSource s = cellFactory.getCollectorSourceRndNoNode(edge.getType(), cell, edge.getSource().getNodeAttached());
        if (s == null) {
            throw new Exception("There is no other source collector of type " + edge.getSource().getType().toString() + " to move to.");
        }
        //Add edge to new collector
        s.addEdge(edge);
        //Remove edge from old collector
        edge.getSource().removeEdge(edge);
        //Set edges new source collector
        edge.setSource(s);
        return cell;
    }
}
