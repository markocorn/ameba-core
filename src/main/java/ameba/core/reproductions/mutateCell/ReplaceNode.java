package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryEdge;
import ameba.core.factories.FactoryNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by marko on 12/30/16.
 */
public class ReplaceNode implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;

    public ReplaceNode(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory) {
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        this.edgeFactory = edgeFactory;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        Node nodeOld = cellFactory.getNodeRndInner(cell);
        Node nodeNew = nodeFactory.genNodeRnd();

        reconnectInputs(Double.class, cell, nodeOld, nodeNew);
        reconnectInputs(Integer.class, cell, nodeOld, nodeNew);
        reconnectInputs(Boolean.class, cell, nodeOld, nodeNew);

        reconnectsOutputs(Double.class, cell, nodeOld, nodeNew);
        reconnectsOutputs(Integer.class, cell, nodeOld, nodeNew);
        reconnectsOutputs(Boolean.class, cell, nodeOld, nodeNew);

        cell.getNodes().set(cell.getNodes().indexOf(nodeOld), nodeNew);
        return cell;
    }

    private void reconnectInputs(Class type, Cell cell, Node nodeOld, Node nodeNew) throws Exception {
        ArrayList<CollectorInp> oldInpCol = nodeOld.getInpCollectorsConnected(type);
        Collections.shuffle(oldInpCol);
        int diff = oldInpCol.size() - nodeNew.getInpColLimit(type)[0];
        int same = Math.min(oldInpCol.size(), nodeNew.getInpColLimit(type)[0]);
        for (int i = 0; i < same; i++) {
            Edge edge = oldInpCol.get(i).getEdges().get(0);
            edge.setTarget(oldInpCol.get(i));
            nodeNew.getInpCollectors(type).get(i).addEdge(edge);
        }
        //If number of old input edges is greater than number of minimum edges of new node the edges are removed from cell
        for (int i = 0; i < diff; i++) {
            cell.removeEdge(oldInpCol.get(same + i).getEdges().get(0));
        }
        //If number of old input edges is less than number of minimum edges of new node the empty spots are connected with new edges
        for (int i = 0; i < -diff; i++) {
            CollectorOut collectorOut = cellFactory.getCollectorOutRndNoNode(type, cell, nodeOld);
            Edge edge;
            if (collectorOut != null) {
                edge = edgeFactory.genEdge(type, collectorOut, nodeNew.getInpCollectors(type).get(same + i));
            } else {
                //No proper node generate constant node
                if (nodeFactory.isConstantAvaliable(type)) {
                    Node node = nodeFactory.genConstant(type);
                    edge = edgeFactory.genEdge(type, node.getOutCollectors(type).get(0), nodeNew.getInpCollectors(type).get(same + i));
                    cell.addNode(node);
                } else
                    throw new Exception("Cell can't be properly connected. Must allow the generation of Constant nodes of type: " + type.getSimpleName());
            }
            cell.addEdge(edge);
        }
    }

    private void reconnectsOutputs(Class type, Cell cell, Node nodeOld, Node nodeNew) throws Exception {
        ArrayList<CollectorOut> oldOutCol = nodeOld.getOutCollectors(type);
        ArrayList<Edge> edges = new ArrayList<>();
        for (CollectorOut collectorOut : oldOutCol) {
            edges.addAll(collectorOut.getEdges());
        }
        ArrayList<CollectorOut> outs = nodeNew.getOutCollectors(type);
        for (Edge edge : edges) {
            if (outs.size() > 0) {
                CollectorOut collectorOut = outs.get(random.nextInt(outs.size()));
                collectorOut.addEdge(edge);
                edge.setSource(collectorOut);
            } else {
                CollectorOut collectorOut = cellFactory.getCollectorOutRndNoNode(type, cell, nodeOld);
                collectorOut.addEdge(edge);
                edge.setSource(collectorOut);
            }
        }
    }
}
