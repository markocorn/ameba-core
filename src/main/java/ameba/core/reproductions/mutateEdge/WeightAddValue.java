package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Edge;
import ameba.core.reproductions.parametersOperations.RepParSettings;

/**
 * Created by marko on 12/28/16.
 */
public class WeightAddValue implements IMutateEdge {
    ameba.core.reproductions.parametersOperations.genParMutation.AddValue addValue;

    public WeightAddValue(ameba.core.reproductions.parametersOperations.genParMutation.AddValue addValue) {
        this.addValue = addValue;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        edge.setWeight(addValue.mutate(edge.getWeight()));
        return edge;
    }

    @Override
    public RepParSettings getSettings() {
        return addValue.getSettings();
    }
}
