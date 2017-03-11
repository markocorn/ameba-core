package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by marko on 3/1/17.
 */
public class RemoveNode1 implements IMutateCell {
    FactoryCell cellFactory;
    Random random;

    public RemoveNode1(FactoryCell cellFactory) {
        this.cellFactory = cellFactory;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Node to be removed
        if (cell.getInnerNodes().size() > 0) {
            Node node = cell.getInnerNodes().get(random.nextInt(cell.getInnerNodes().size()));
            ArrayList<CollectorSource> outs = new ArrayList<>();
            for (CollectorTarget collectorInp : node.getInpCollectorsConnected()) {
                outs.add(collectorInp.getEdges().get(0).getSource());
                collectorInp.getEdges().get(0).getSource().removeEdge(collectorInp.getEdges().get(0));
                cell.removeEdge(collectorInp.getEdges().get(0));
            }
            for (CollectorSource collectorOut : node.getCollectorSources()) {
                for (Edge edge : collectorOut.getEdges()) {
                    edge.getTarget().removeEdge(edge);
                    CollectorSource collectorOut1 = cellFactory.getRndCollector(edge.getWeight().gettClass(), outs);
                    if (collectorOut1 != null) {
                        outs.remove(collectorOut1);
                    } else {
                        collectorOut1 = cellFactory.getCollectorOutRndNoNode(edge.getSource().getType(), cell, node);
                    }
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
