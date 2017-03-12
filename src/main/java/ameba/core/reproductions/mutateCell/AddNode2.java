package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryNode;

import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class AddNode2 implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    Random random;

    public AddNode2(FactoryNode nodeFactory, FactoryCell cellFactory) {
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Select random edge
        Edge edge = cell.getEdges().get(random.nextInt(cell.getEdges().size()));
        Class type = edge.getSource().getType();
        //Generate node with same type of output collector as edge type
        Node nodeNew = nodeFactory.genNodeRndInpColType(type);
        if (nodeNew == null)
            throw new Exception("Can't generate node with " + type.getSimpleName() + " type of input collector");
        CollectorTarget collectorInp = nodeNew.getCollectorsTargetMin(type).get(random.nextInt(nodeNew.getCollectorsTargetMin(type).size()));
        //Add edge to new collector
        collectorInp.addEdge(edge);
        //Remove edge from old collector
        edge.getTarget().removeEdge(edge);
        //Set edges new source collector
        edge.setTarget(collectorInp);
        //Add new node to the cell
        cell.addNode(nodeNew);
        //Connects free inputs of new node
        cellFactory.connectsMinFreeInputs(cell);
        return cell;
    }
}
