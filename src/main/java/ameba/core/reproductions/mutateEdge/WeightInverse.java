package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.InverseValue;

/**
 * Created by marko on 12/28/16.
 */
public class WeightInverse extends Reproduction implements IMutateEdge {
    InverseValue inverseValue;

    public WeightInverse(InverseValue inverseValue) {
        super(inverseValue.getSettings().getProbability());
        this.inverseValue = inverseValue;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        edge.setWeight(inverseValue.mutate(edge.getWeight()));
        return edge;
    }


}
