package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryEdge;
import ameba.core.factories.FactoryNode;
import ameba.core.reproductions.Reproduction;

import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class AddNode4 extends Reproduction implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;

    public AddNode4(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory, int probability, long seed) {
        super(probability);
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        this.edgeFactory = edgeFactory;
        random = new Random(seed);
    }

    /**
     * Add new node and random connect
     *
     * @param cell
     * @return
     * @throws Exception
     */
    @Override
    public Cell mutate(Cell cell) throws Exception {
        Node n = nodeFactory.genNodeRnd();
        cell.addNode(n);
        //Connect target collectors of node
        for (CollectorTarget t : n.getCollectorsTargetToConnect()) {
            //Select random source
            CollectorSource s = cellFactory.getCollectorSourceRnd(cell);
            if (s == null) {
                throw new Exception("No random source present in cell");
            }
            Edge e = edgeFactory.genEdge(s, t);
            cell.addEdge(e);
        }
        //Connect one source collector to target collector of some node in cell
        CollectorTarget t = cellFactory.getCollectorTargetFreeRnd(cell);
        if (t == null) {
            throw new Exception("No free random target present in cell");
        }
        CollectorSource s = cellFactory.getCollectorSourceRnd(cell);
        if (s == null) {
            throw new Exception("No free random source present in cell");
        }
        Edge e = edgeFactory.genEdge(s, t);
        cell.addEdge(e);

        return cell;
    }
}
