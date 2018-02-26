package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineAvgDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCombineAvgDec extends Reproduction implements ICrossEdge {
    CombineAvgDec operationType;

    public WeightCombineAvgDec(CombineAvgDec operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.DECIMAL;
    }

    @Override
    public Edge cross(Edge edge1, Edge edge2) {
        ((EdgeDec) edge1).setWeight(operationType.crossover(((EdgeDec) edge1).getWeight(), ((EdgeDec) edge2).getWeight()));
        return null;
    }
}
