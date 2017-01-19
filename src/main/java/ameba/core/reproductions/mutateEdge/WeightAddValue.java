package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.connections.Edge;

/**
 * Created by marko on 12/28/16.
 */
public class WeightAddValue implements IMutateEdge {
    ameba.core.reproductions.parametersOperations.genParDecMutation.AddValue addValue;

    public WeightAddValue(ameba.core.reproductions.parametersOperations.genParDecMutation.AddValue addValue) {
        this.addValue = addValue;
    }

    @Override
    public Edge mutate(Edge edge) {
//        edge.setWeight(addValue.mutate(edge.getWeight()));
        return edge;
    }
}
