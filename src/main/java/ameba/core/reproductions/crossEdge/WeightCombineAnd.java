package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeBin;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineAnd;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCombineAnd extends Reproduction implements ICrossEdge {
    CombineAnd operationType;

    public WeightCombineAnd(CombineAnd operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public Edge cross(Edge edge1, Edge edge2) {
        ((EdgeBin) edge1).setWeight(operationType.crossover(((EdgeBin) edge1).getWeight(), ((EdgeBin) edge2).getWeight()));
        return null;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.BOOLEAN;
    }
}
