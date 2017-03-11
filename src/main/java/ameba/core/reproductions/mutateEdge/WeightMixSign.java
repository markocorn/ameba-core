package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.MixSignDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightMixSign extends Reproduction implements IMutateEdge {
    MixSignDec mixSign;

    public WeightMixSign(MixSignDec mixSign) {
        super(mixSign.getSettings().getProbability());
        this.mixSign = mixSign;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        edge.setWeight(mixSign.mutate(edge.getWeight()));
        return edge;
    }

}
