package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Edge;
import ameba.core.blocks.Signal;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.AddValueInt;

/**
 * Created by marko on 12/28/16.
 */
public class WeightAddValueInt extends Reproduction implements IMutateEdge {
    AddValueInt operation;

    public WeightAddValueInt(AddValueInt addValue) {
        super(addValue.getParOperationSettings().getProbability());
        this.operation = addValue;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        edge.setWeight(Signal.createInteger(operation.mutate(edge.getWeight().getValueInteger())));
        return edge;
    }

}
