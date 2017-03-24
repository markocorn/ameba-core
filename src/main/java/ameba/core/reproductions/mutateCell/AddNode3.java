package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryEdge;
import ameba.core.factories.FactoryNode;

import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class AddNode3 implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;

    public AddNode3(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory) {
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        this.edgeFactory = edgeFactory;
        random = new Random();
    }

    /**
     * Add new node and connect serial
     *
     * @param cell
     * @return
     * @throws Exception
     */
    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Select random edge
        Edge e1 = cell.getEdges().get(random.nextInt(cell.getEdges().size()));
        CollectorSource s1 = e1.getSource();
        CollectorTarget t1 = e1.getTarget();
        //Generate node with same type of input and output collector as edge type
        Node nodeNew = nodeFactory.genNodeRndCollectorsType(e1.getType(), e1.getType());
        if (nodeNew == null)
            throw new Exception("Can't generate node with " + e1.getType().toString() + " type of input and output collector");
        CollectorTarget t2 = nodeNew.getCollectorsTargetMin(e1.getType()).get(random.nextInt(nodeNew.getCollectorsTargetMin(e1.getType()).size()));
        CollectorSource s2 = nodeNew.getCollectorsSource(e1.getType()).get(random.nextInt(nodeNew.getCollectorsSource(e1.getType()).size()));
        //Create new edge to connect output od new node to the source of old connected node
        Edge e2 = edgeFactory.genEdge(e1.getType(), s2, t1);
        //Add new edge to new collector
        s2.addEdge(e2);
        //Add new edge to t1
        t1.removeEdge(e1);
        t1.addEdge(e2);
        t2.addEdge(e1);
        e1.setTarget(t2);

        //Add new node to the cell
        cell.addNode(nodeNew);
        cell.addEdgeNoVerification(e2);


        //Connects free inputs of new node
        cellFactory.connectsMinFreeInputs(cell);
        return cell;
    }
}
