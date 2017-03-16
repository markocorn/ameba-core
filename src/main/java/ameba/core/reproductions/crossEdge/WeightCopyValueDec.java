package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.EdgeDec;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CopyValueDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCopyValueDec extends Reproduction implements ICrossEdgeDec {
    CopyValueDec operationType;

    public WeightCopyValueDec(CopyValueDec operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public EdgeDec cross(EdgeDec edge1, EdgeDec edge2) throws Exception {
        edge1.setWeight(operationType.crossover(edge1.getWeight(), edge2.getWeight()));
        return edge1;
    }
}
