package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.MixSignDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightMixSign extends Reproduction implements IMutateEdge {
    MixSignDec operation;

    public WeightMixSign(MixSignDec mixSign) {
        super(mixSign.getParOperationSettings().getProbability());
        this.operation = mixSign;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        edge.setWeight(operation.mutate(edge.getWeight()));
        return edge;
    }
}
