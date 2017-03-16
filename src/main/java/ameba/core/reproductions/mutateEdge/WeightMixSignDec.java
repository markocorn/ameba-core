package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.EdgeDec;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.MixSignDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightMixSignDec extends Reproduction implements IMutateEdgeDec {
    MixSignDec mixSign;

    public WeightMixSignDec(MixSignDec mixSign) {
        super(mixSign.getParOperationSettings().getProbability());
        this.mixSign = mixSign;
    }

    @Override
    public EdgeDec mutate(EdgeDec edge) throws Exception {
        edge.setWeight(mixSign.mutate(edge.getWeight()));
        return edge;
    }

}
