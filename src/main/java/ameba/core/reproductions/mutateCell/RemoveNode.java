package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;

import java.util.Random;

/**
 * Created by marko on 3/1/17.
 */
public class RemoveNode implements IMutateCell {
    FactoryCell cellFactory;
    Random random;

    public RemoveNode(FactoryCell cellFactory) {
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
        if (cell.getInnerNodes().size() > 0) {
            Node node = cell.getInnerNodes().get(random.nextInt(cell.getInnerNodes().size()));
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
                        throw new Exception("Cant remove cell no substitute source collector found of type:" + s.getType().toString());
                    }
                }
            }
            cell.removeNode(node);
        } else throw new Exception("Number of inner nodes is zero");
        return cell;
    }
}
