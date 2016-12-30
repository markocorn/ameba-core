package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Edge;
import ameba.core.reproductions.parametersOperations.genParDecMutation.InverseValue;

/**
 * Created by marko on 12/28/16.
 */
public class WeightInverse implements IMutateEdge {
    InverseValue inverseValue;

    public WeightInverse(InverseValue inverseValue) {
        this.inverseValue = inverseValue;
    }

    @Override
    public Edge mutate(Edge edge) {
        edge.setWeight(inverseValue.mutate(edge.getWeight()));
        return edge;
    }
}
