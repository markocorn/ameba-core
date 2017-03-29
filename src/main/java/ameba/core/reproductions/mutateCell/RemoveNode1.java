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
public class RemoveNode1 extends Reproduction implements IMutateCell {
    FactoryCell cellFactory;
    Random random;

    public RemoveNode1(FactoryCell cellFactory, int probability) {
        super(probability);
        this.cellFactory = cellFactory;
        random = new Random();
    }

    /**
     * Remove node serial
     * <p>
     * Remove node and reconnect output edges with input edges of the removed node.
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
            ArrayList<CollectorSource> ss = new ArrayList<>();
            for (CollectorTarget t : node.getCollectorsTargetConnected()) {
                ss.add(t.getEdges().get(0).getSource());
                cell.removeEdge(t.getEdges().get(0));
            }
            for (CollectorSource s : node.getCollectorsSourceConnected()) {
                for (Edge e : s.getEdges()) {
                    CollectorSource s1 = cellFactory.getCollectorSourceRnd(e.getType(), ss);
                    if (s1 != null) {
                        ss.remove(s1);
                    } else {
                        s1 = cellFactory.getCollectorSourceRndNoNode(e.getSource().getType(), cell, node);
                    }
                    if (s1 != null) {
                        e.setSource(s1);
                        s1.addEdge(e);
                    } else {
                        cell.removeEdge(e);
                    }
                }
            }
            cell.removeNode(node);
            cellFactory.connectsMinFreeInputs(cell);
        } else throw new Exception("Number of inner nodes is zero");
        return cell;
    }


}
