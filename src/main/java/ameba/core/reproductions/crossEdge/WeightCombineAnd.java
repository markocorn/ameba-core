package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.EdgeBin;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineAnd;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCombineAnd extends Reproduction implements ICrossEdgeBin {
    CombineAnd operationType;

    public WeightCombineAnd(CombineAnd operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public EdgeBin cross(EdgeBin edge1, EdgeBin edge2) throws Exception {
        edge1.setWeight(operationType.crossover(edge1.getWeight(), edge2.getWeight()));
        return edge1;
    }
}
