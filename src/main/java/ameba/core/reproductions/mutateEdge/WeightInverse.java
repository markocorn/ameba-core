package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.InverseValue;

/**
 * Created by marko on 12/28/16.
 */
public class WeightInverse extends Reproduction implements IMutateEdge {
    InverseValue operation;

    public WeightInverse(InverseValue inverseValue) {
        super(inverseValue.getParOperationSettings().getProbability());
        this.operation = inverseValue;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        ((EdgeDec) edge).setWeight(operation.mutate(((EdgeDec) edge).getWeight()));
        return edge;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.DECIMAL;
    }
}
