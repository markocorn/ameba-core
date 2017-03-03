package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Edge;
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

    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Node to be removed
        if (cell.getInnerNodes().size() > 0) {
            Node node = cell.getInnerNodes().get(random.nextInt(cell.getInnerNodes().size()));
            for (CollectorInp collectorInp : node.getInpCollectorsConnected()) {
                collectorInp.getEdges().get(0).getSource().removeEdge(collectorInp.getEdges().get(0));
                cell.removeEdge(collectorInp.getEdges().get(0));
            }
            for (CollectorOut collectorOut : node.getOutCollectors()) {
                for (Edge edge : collectorOut.getEdges()) {
                    edge.getTarget().removeEdge(edge);
                    CollectorOut collectorOut1 = cellFactory.getCollectorOutRndNoNode(edge.getSource().getType(), cell, node);
                    if (collectorOut1 != null) {
                        edge.setSource(collectorOut1);
                    } else {
                        cell.removeEdge(edge);
                    }
                }
            }
            cell.removeNode(node);
            cellFactory.connectsMinFreeInputs(cell);
        } else throw new Exception("Number of inner nodes is zero");
        return cell;
    }
}
