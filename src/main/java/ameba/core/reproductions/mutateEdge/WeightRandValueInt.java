package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.RandValueInt;

/**
 * Created by marko on 12/28/16.
 */
public class WeightRandValueInt extends Reproduction implements IMutateEdge {
    RandValueInt operation;

    public WeightRandValueInt(RandValueInt randValue) {
        super(randValue.getParOperationSettings().getProbability());
        this.operation = randValue;
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
