package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.AddValueInt;

/**
 * Created by marko on 12/28/16.
 */
public class WeightAddValueInt extends Reproduction implements IMutateEdgeInt {
    AddValueInt operation;

    public WeightAddValueInt(AddValueInt addValue) {
        super(addValue.getParOperationSettings().getProbability());
        this.operation = addValue;
    }

    @Override
    public EdgeInt mutate(EdgeInt edge) throws Exception {
        edge.setWeight(operation.mutate(edge.getWeight()));
        return edge;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.INTEGER;
    }
}
