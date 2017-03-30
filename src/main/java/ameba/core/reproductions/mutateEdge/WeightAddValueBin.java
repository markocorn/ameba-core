package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.EdgeBin;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.AddValueBin;

/**
 * Created by marko on 12/28/16.
 */
public class WeightAddValueBin extends Reproduction implements IMutateEdgeBin {
    AddValueBin operation;

    public WeightAddValueBin(AddValueBin addValue) {
        super(addValue.getParOperationSettings().getProbability());
        this.operation = addValue;
    }

    @Override
    public EdgeBin mutate(EdgeBin edge) throws Exception {
        edge.setWeight(operation.mutate(edge.getWeight()));
        return edge;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.BOOLEAN;
    }
}
