package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.conectivity.Edge;

/**
 * Created by marko on 12/28/16.
 */
public class WeightAddValue implements IMutateEdge {
    ameba.core.reproductions.parametersOperations.genParMutation.AddValue addValue;

    public WeightAddValue(ameba.core.reproductions.parametersOperations.genParMutation.AddValue addValue) {
        this.addValue = addValue;
    }

    @Override
    public Edge mutate(Edge edge) {
        edge.setWeight(addValue.mutate(edge.getWeight()));
        return edge;
    }
}
