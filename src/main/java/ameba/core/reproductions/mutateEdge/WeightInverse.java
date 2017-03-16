package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.EdgeDec;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.InverseValue;

/**
 * Created by marko on 12/28/16.
 */
public class WeightInverse extends Reproduction implements IMutateEdgeDec {
    InverseValue inverseValue;

    public WeightInverse(InverseValue inverseValue) {
        super(inverseValue.getParOperationSettings().getProbability());
        this.inverseValue = inverseValue;
    }

    @Override
    public EdgeDec mutate(EdgeDec edge) throws Exception {
        edge.setWeight(inverseValue.mutate(edge.getWeight()));
        return edge;
    }


}
