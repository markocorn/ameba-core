package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.MixSignInt;

/**
 * Created by marko on 12/28/16.
 */
public class WeightMixSignInt extends Reproduction implements IMutateEdge {
    MixSignInt operation;

    public WeightMixSignInt(MixSignInt mixSign) {
        super(mixSign.getParOperationSettings().getProbability());
        this.operation = mixSign;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        ((EdgeInt) edge).setWeight(operation.mutate(((EdgeInt) edge).getWeight()));
        return edge;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.INTEGER;
    }
}
