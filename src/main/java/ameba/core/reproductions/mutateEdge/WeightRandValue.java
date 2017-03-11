package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.RandValueDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightRandValue extends Reproduction implements IMutateEdge {
    RandValueDec randValue;

    public WeightRandValue(RandValueDec randValue) {
        super(randValue.getSettings().getProbability());
        this.randValue = randValue;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        edge.setWeight(randValue.mutate(edge.getWeight()));
        return edge;
    }

}
