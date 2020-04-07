package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineSubDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCombineSub extends Reproduction implements ICrossEdge {
    CombineSubDec operationType;

    public WeightCombineSub(CombineSubDec operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public Edge cross(Edge edge1, Edge edge2) {
        edge1.setWeight(operationType.crossover(edge1.getWeight(), edge2.getWeight()));
        return null;
    }
}
