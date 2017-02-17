package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.conectivity.Edge;
import ameba.core.reproductions.parametersOperations.genParMutation.RandValue;

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
//        edge.setWeight(randValue.mutate(edge.getWeight()));
        return edge;
    }
}
