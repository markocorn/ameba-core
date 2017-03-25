package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.EdgeInt;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineSubInt;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCombineSubInt extends Reproduction implements ICrossEdgeInt {
    CombineSubInt operationType;

    public WeightCombineSubInt(CombineSubInt operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public EdgeInt cross(EdgeInt edge1, EdgeInt edge2) throws Exception {
        edge1.setWeight(operationType.crossover(edge1.getWeight(), edge2.getWeight()));
        return edge1;
    }
}