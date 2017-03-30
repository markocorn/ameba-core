package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeBin;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.RandValueBin;

/**
 * Created by marko on 12/28/16.
 */
public class WeightRandValueBin extends Reproduction implements IMutateEdge {
    RandValueBin operation;

    public WeightRandValueBin(RandValueBin randValue) {
        super(randValue.getParOperationSettings().getProbability());
        this.operation = randValue;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        ((EdgeBin) edge).setWeight(operation.mutate(((EdgeBin) edge).getWeight()));
        return edge;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.BOOLEAN;
    }
}
