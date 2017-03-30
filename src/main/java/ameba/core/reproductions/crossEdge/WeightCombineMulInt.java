package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineMulInt;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCombineMulInt extends Reproduction implements ICrossEdgeInt {
    CombineMulInt operationType;

    public WeightCombineMulInt(CombineMulInt operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public EdgeInt cross(EdgeInt edge1, EdgeInt edge2) throws Exception {
        edge1.setWeight(operationType.crossover(edge1.getWeight(), edge2.getWeight()));
        return edge1;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.INTEGER;
    }
}
