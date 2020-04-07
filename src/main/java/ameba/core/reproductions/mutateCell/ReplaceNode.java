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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by marko on 12/30/16.
 */
public class ReplaceNode extends Reproduction implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;

    public ReplaceNode(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory, int probability, long seed) {
        super(probability);
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        this.edgeFactory = edgeFactory;
        random = new Random(seed);
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        Node nodeOld = cellFactory.getNodeRndInnerFullUnlocked(cell);
        Node nodeNew = nodeFactory.genNodeRnd();

        cell.getNodes().set(cell.getNodes().indexOf(nodeOld), nodeNew);

        reconnectInputs(cell, nodeOld, nodeNew);

        reconnectsOutputs(cell, nodeOld, nodeNew);

        return cell;
    }

    private void reconnectInputs(Cell cell, Node nodeOld, Node nodeNew) throws Exception {
        ArrayList<CollectorTarget> tt = nodeOld.clcCollectorsTargetConnected();
        Collections.shuffle(tt);
        int diff = tt.size() - nodeNew.getCollectorTargetLimit()[0];
        int same = Math.min(tt.size(), nodeNew.getCollectorTargetLimit()[0]);
        for (int i = 0; i < same; i++) {
            Edge e = tt.get(i).getEdges().get(0);
            e.setTarget(nodeNew.getCollectorsTarget().get(i));
            e.getTarget().addEdge(e);
        }

        //If number of old input edges is greater than number of minimum edges of new node the edges are removed from cell
        for (int i = 0; i < diff; i++) {
            cell.removeEdge(tt.get(same + i).getEdges().get(0));
        }
        //If number of old input edges is less than number of minimum edges of new node the empty spots are connected with new edges
        for (int i = 0; i < -diff; i++) {
            CollectorSource s = cellFactory.getCollectorSourceRndNoNode(cell, nodeOld);
            Edge edge;
            if (s != null) {
                edge = edgeFactory.genEdge(s, nodeNew.getCollectorsTarget().get(same + i));
            } else {
                //No proper node generate constant node
                if (nodeFactory.isConstantNodeAvailable()) {
                    Node node = nodeFactory.genConstantNode();
                    edge = edgeFactory.genEdge(node.getCollectorsSource().get(0), nodeNew.getCollectorsTarget().get(same + i));
                    cell.addNode(node);
                } else
                    throw new Exception("Cell can't be properly connected. Must allow the generation of Constant nodes.");
            }
            cell.addEdge(edge);
        }
    }

    private void reconnectsOutputs(Cell cell, Node nodeOld, Node nodeNew) throws Exception {
        ArrayList<CollectorSource> oldOutCol = (ArrayList<CollectorSource>) nodeOld.getCollectorsSource();
        ArrayList<Edge> edges = new ArrayList<>();
        for (CollectorSource collectorOut : oldOutCol) {
            edges.addAll(collectorOut.getEdges());
        }
        ArrayList<CollectorSource> outs = (ArrayList<CollectorSource>) nodeNew.getCollectorsSource();
        for (Edge edge : edges) {
            if (outs.size() > 0) {
                CollectorSource collectorOut = outs.get(random.nextInt(outs.size()));
                collectorOut.addEdge(edge);
                edge.setSource(collectorOut);
            } else {
                CollectorSource collectorOut = cellFactory.getCollectorSourceRndNoNode(cell, nodeOld);
                collectorOut.addEdge(edge);
                edge.setSource(collectorOut);
            }
        }
    }
}
