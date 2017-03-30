package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.RandValueDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightRandValueDec extends Reproduction implements IMutateEdgeDec {
    RandValueDec randValue;

    public WeightRandValueDec(RandValueDec randValue) {
        super(randValue.getParOperationSettings().getProbability());
        this.randValue = randValue;
    }

    @Override
    public EdgeDec mutate(EdgeDec edge) throws Exception {
        edge.setWeight(randValue.mutate(edge.getWeight()));
        return edge;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.DECIMAL;
    }
}
