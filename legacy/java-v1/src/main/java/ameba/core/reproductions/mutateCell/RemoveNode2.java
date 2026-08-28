package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.reproductions.Reproduction;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by marko on 3/1/17.
 */
public class RemoveNode2 extends Reproduction implements IMutateCell {
    FactoryCell cellFactory;
    Random random;

    public RemoveNode2(FactoryCell cellFactory, int probability, long seed) {
        super(probability);
        this.cellFactory = cellFactory;
        random = new Random(seed);
    }

    /**
     * Remove node serial
     * <p>
     * Remove node that can be free removed
     *
     * @param cell
     * @return
     * @throws Exception
     */
    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Find node
        ArrayList<Node> nodes = cell.getInnerNodesFullUnlocked();
        Node n = nodes.get(random.nextInt(nodes.size()));

        //Remove Target edges
        for (CollectorTarget t : n.getCollectorsTargetConnected()) {
            for (Edge e : t.getEdges()) {
                e.getSource().removeEdge(e);
                cell.getEdges().remove(e);
            }
        }
        //Remove Source edges
        for (CollectorSource s : n.getCollectorsSourceConnected()) {
            for (Edge e : s.getEdges()) {
                e.getTarget().removeEdge(e);
                cell.getEdges().remove(e);
            }
        }

        cell.removeNode(n);

        cellFactory.connectsMinFreeInputs(cell);


        return cell;
    }


}
