package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CopyValueDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCopyValueDec extends Reproduction implements ICrossEdge {
    CopyValueDec operationType;

    public WeightCopyValueDec(CopyValueDec operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public Edge cross(Edge edge1, Edge edge2) {
        ((EdgeDec) edge1).setWeight(operationType.crossover(((EdgeDec) edge1).getWeight(), ((EdgeDec) edge2).getWeight()));
        return null;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.DECIMAL;
    }
}
