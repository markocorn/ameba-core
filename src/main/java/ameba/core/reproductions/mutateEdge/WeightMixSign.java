package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Edge;
import ameba.core.reproductions.parametersOperations.RepParSettings;
import ameba.core.reproductions.parametersOperations.genParMutation.MixSign;

/**
 * Created by marko on 12/28/16.
 */
public class WeightMixSign implements IMutateEdge {
    MixSign mixSign;

    public WeightMixSign(MixSign mixSign) {
        this.mixSign = mixSign;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        edge.setWeight(mixSign.mutate(edge.getWeight()));
        return edge;
    }

    @Override
    public RepParSettings getSettings() {
        return mixSign.getSettings();
    }
}
