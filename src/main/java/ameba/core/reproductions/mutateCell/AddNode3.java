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

    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Select random edge
        Edge edge = cell.getEdges().get(random.nextInt(cell.getEdges().size()));
        //Generate node with same type of input and output collector as edge type
        Node nodeNew = nodeFactory.genNodeRndCollectorsType(edge.getType(), edge.getType());
        if (nodeNew == null)
            throw new Exception("Can't generate node with " + edge.getType().toString() + " type of input and output collector");
        CollectorTarget collectorInp = nodeNew.getCollectorsTargetMin(edge.getType()).get(random.nextInt(nodeNew.getCollectorsTargetMin(edge.getType()).size()));
        CollectorSource collectorOut = nodeNew.getCollectorsSource(edge.getType()).get(random.nextInt(nodeNew.getCollectorsSource(edge.getType()).size()));
        //Create new edge to connect output od new node to the source of old connected node
        Edge edge1 = edgeFactory.genEdge(edge.getType(), collectorOut, edge.getTarget());
        //Add edge to new collector
        collectorOut.addEdge(edge1);
        //Add edge to new collector
        collectorInp.addEdge(edge);
        //Remove edge from old collector
        edge.getTarget().removeEdge(edge);
        //Set edges new source collector
        edge.setTarget(collectorInp);
        //Add new node to the cell
        cell.addNode(nodeNew);
        cell.addEdge(edge1);

        //Connects free inputs of new node
        cellFactory.connectsMinFreeInputs(cell);
        return cell;
    }
}
