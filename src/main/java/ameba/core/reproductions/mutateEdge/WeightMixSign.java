package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.parametersOperations.genParDecMutation.MixSign;

/**
 * Created by marko on 12/28/16.
 */
public class WeightMixSign implements IMutateEdge {
    MixSign mixSign;

    public WeightMixSign(MixSign mixSign) {
        this.mixSign = mixSign;
    }

    @Override
    public Edge mutate(Edge edge) {
        edge.setWeight(mixSign.mutate(edge.getWeight()));
        return edge;
    }
}
