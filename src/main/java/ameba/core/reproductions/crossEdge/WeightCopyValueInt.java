package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CopyValueInt;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCopyValueInt extends Reproduction implements ICrossEdge {
    CopyValueInt operationType;

    public WeightCopyValueInt(CopyValueInt operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public Edge cross(Edge edge1, Edge edge2) {
        ((EdgeInt) edge1).setWeight(operationType.crossover(((EdgeInt) edge1).getWeight(), ((EdgeInt) edge2).getWeight()));
        return null;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.INTEGER;
    }
}
