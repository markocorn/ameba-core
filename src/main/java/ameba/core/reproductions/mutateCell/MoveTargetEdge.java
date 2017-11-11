package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryNode;
import ameba.core.reproductions.Reproduction;

import java.util.Random;

public class MoveTargetEdge extends Reproduction implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    Random random;

    public MoveTargetEdge(FactoryNode nodeFactory, FactoryCell cellFactory, int probability) {
        super(probability);
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Select random edge
        Edge edge = cell.getEdgesUnlockedTarget().get(random.nextInt(cell.getEdgesUnlockedTarget().size()));
        //Find new target to reconnect
        CollectorTarget t = cellFactory.getCollectorTargetToConnectRndNoNode(cell, edge.getType(), edge.getSource().getNodeAttached());
        if (t == null) {
            throw new Exception("There is no other target collector of type " + edge.getTarget().getType().toString() + " to move to.");
        }
        //Add edge to new collector
        t.addEdge(edge);
        //Remove edge from old collector
        edge.getTarget().removeEdge(edge);
        //Set edges new target collector
        edge.setTarget(t);
        return cell;
    }
}