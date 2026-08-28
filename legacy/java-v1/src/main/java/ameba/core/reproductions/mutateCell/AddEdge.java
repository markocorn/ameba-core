package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryEdge;
import ameba.core.factories.FactoryNode;
import ameba.core.reproductions.Reproduction;

import java.util.ArrayList;
import java.util.Random;

public class AddEdge extends Reproduction implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;

    public AddEdge(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory, int probability, long seed) {
        super(probability);
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        this.edgeFactory = edgeFactory;
        random = new Random(seed);
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Select random free target collector
        ArrayList<CollectorTarget> list = cellFactory.getCollectorsTargetNotConnected(cell);
        if (list.size() < 1) {
            throw new Exception("There is no free target collector");
        }
        CollectorTarget t = list.get(random.nextInt(list.size()));

        //Select random free source that matches target by type
        CollectorSource s = cellFactory.getCollectorSourceRnd(cell);
        if (s == null) {
            cellFactory.getCollectorSourceRnd(cell);
            throw new Exception("There is no free source collector");

        }
        Edge e = edgeFactory.genEdge(s, t);
        cell.addEdge(e);

        return cell;
    }
}
