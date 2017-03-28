package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.FactoryCell;
import ameba.core.factories.FactoryNode;
import ameba.core.reproductions.Reproduction;

import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class AddNode1 extends Reproduction implements IMutateCell {
    FactoryNode nodeFactory;
    FactoryCell cellFactory;
    Random random;

    public AddNode1(FactoryNode nodeFactory, FactoryCell cellFactory, int probability) {
        super(probability);
        this.nodeFactory = nodeFactory;
        this.cellFactory = cellFactory;
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        //Select random edge
        Edge edge = cell.getEdges().get(random.nextInt(cell.getEdges().size()));
        //Generate node with same type of output collector as edge type
        Node nodeNew = nodeFactory.genNodeRndCollectorSourceType(edge.getType());
        if (nodeNew == null)
            throw new Exception("Can't generate node with " + edge.getType().toString() + " type of output collector");
        CollectorSource collectorOut = nodeNew.getCollectorsSource(edge.getType()).get(random.nextInt(nodeNew.getCollectorsSource(edge.getType()).size()));
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
