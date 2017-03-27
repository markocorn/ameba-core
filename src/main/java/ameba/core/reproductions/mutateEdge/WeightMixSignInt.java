package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.EdgeInt;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.MixSignInt;

/**
 * Created by marko on 12/28/16.
 */
public class WeightMixSignInt extends Reproduction implements IMutateEdgeInt {
    MixSignInt mixSign;

    public WeightMixSignInt(MixSignInt mixSign) {
        super(mixSign.getParOperationSettings().getProbability());
        this.mixSign = mixSign;
    }

    @Override
    public EdgeInt mutate(EdgeInt edge) throws Exception {
        edge.setWeight(mixSign.mutate(edge.getWeight()));
        return edge;
    }

}
