package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryEdge;
import ameba.core.factories.FactoryNode;

import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class AddNode1 implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    FactoryEdge edgeFactory;
    Random random;

    public AddNode1(FactoryNode nodeFactory, FactoryCell cellFactory, FactoryEdge edgeFactory) {
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        this.edgeFactory = edgeFactory;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Select random edge
        Edge edge = cell.getEdges().get(random.nextInt(cell.getEdges().size()));
        Class type = edge.getSource().getType();
        //Generate node with same type of output collector as edge type
        Node nodeNew = nodeFactory.genNodeRndOutColType(type);
        if (nodeNew == null)
            throw new Exception("Can't generate node with " + type.getSimpleName() + " type of output collector");
        CollectorOut collectorOut = nodeNew.getOutCollectors(type).get(random.nextInt(nodeNew.getOutCollectors(type).size()));
        //Add edge to new collector
        collectorOut.addEdge(edge);
        //Remove edge from old collector
        edge.getSource().removeEdge(edge);
        //Set edges new source collector
        edge.setSource(collectorOut);
        //Add new node to the cell
        cell.addNode(nodeNew);
        //Connects free inputs of new node
        cellFactory.connectsMinFreeInputs(cell);
        return cell;
    }
}
