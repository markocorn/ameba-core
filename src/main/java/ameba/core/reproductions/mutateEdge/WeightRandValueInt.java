package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.EdgeInt;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.RandValueInt;

/**
 * Created by marko on 12/28/16.
 */
public class WeightRandValueInt extends Reproduction implements IMutateEdgeInt {
    RandValueInt randValue;

    public WeightRandValueInt(RandValueInt randValue) {
        super(randValue.getParOperationSettings().getProbability());
        this.randValue = randValue;
    }

    @Override
    public EdgeInt mutate(EdgeInt edge) throws Exception {
        edge.setWeight(randValue.mutate(edge.getWeight()));
        return edge;
    }

}
