package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineAddDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCombineAdd extends Reproduction implements ICrossEdge {
    CombineAddDec operationType;

    public WeightCombineAdd(CombineAddDec operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public Edge cross(Edge edge1, Edge edge2) {
        edge1.setWeight(operationType.crossover(edge1.getWeight(), edge2.getWeight()));
        return null;
    }
}
