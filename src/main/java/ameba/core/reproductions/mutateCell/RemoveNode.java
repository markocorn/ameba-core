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
public class RemoveNode extends Reproduction implements IMutateCell {
    FactoryCell cellFactory;
    Random random;

    public RemoveNode(FactoryCell cellFactory, int probability) {
        super(probability);
        this.cellFactory = cellFactory;
        random = new Random();
    }

    /**
     * Remove node edged connected to the target collectors are removed. Edges connected to the source collectors are randomly reconnected.
     *
     * @param cell
     * @return
     * @throws Exception
     */
    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Node to be removed
        ArrayList<Node> nodes = cell.getInnerNodesFullUnlocked();
        if (nodes.size() > 0) {
            Node node = nodes.get(random.nextInt(nodes.size()));
            for (CollectorTarget t : node.getCollectorsTargetConnected()) {
                cell.removeEdge(t.getEdges().get(0));
            }
            for (CollectorSource s : node.getCollectorsSource()) {
                for (Edge e : s.getEdges()) {
                    CollectorSource s1 = cellFactory.getCollectorSourceRndNoNode(e.getSource().getType(), cell, node);
                    if (s1 != null) {
                        e.setSource(s1);
                        s1.addEdge(e);
                    } else {
                        throw new Exception("Can't remove cell no substitute source collector found of type:" + s.getType().toString());
                    }
                }
            }
            cell.removeNode(node);
        } else throw new Exception("Number of inner nodes is zero");
        return cell;
    }
}
