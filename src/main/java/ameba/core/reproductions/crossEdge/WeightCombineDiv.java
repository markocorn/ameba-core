package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineDiv;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCombineDiv extends Reproduction implements ICrossEdge {
    CombineDiv operationType;

    public WeightCombineDiv(CombineDiv operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public Edge cross(Edge edge1, Edge edge2) {
        edge1.setWeight(operationType.crossover(edge1.getWeight(), edge2.getWeight()));
        return null;
    }
}
