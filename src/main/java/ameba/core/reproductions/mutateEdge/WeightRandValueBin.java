package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.EdgeBin;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.RandValueBin;

/**
 * Created by marko on 12/28/16.
 */
public class WeightRandValueBin extends Reproduction implements IMutateEdgeBin {
    RandValueBin randValue;

    public WeightRandValueBin(RandValueBin randValue) {
        super(randValue.getParOperationSettings().getProbability());
        this.randValue = randValue;
    }

    @Override
    public EdgeBin mutate(EdgeBin edge) throws Exception {
        edge.setWeight(randValue.mutate(edge.getWeight()));
        return edge;
    }

}
