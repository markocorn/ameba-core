package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Edge;
import ameba.core.reproductions.parametersOperations.genParDecMutation.RandValue;

/**
 * Created by marko on 12/28/16.
 */
public class WeightRandValue implements IMutateEdge {
    RandValue randValue;

    public WeightRandValue(RandValue randValue) {
        this.randValue = randValue;
    }

    @Override
    public Edge mutate(Edge edge) {
        edge.setWeight(randValue.mutate(edge.getWeight()));
        return edge;
    }
}
