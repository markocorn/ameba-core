package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.AddValueDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightAddValueDec extends Reproduction implements IMutateEdge {
    AddValueDec operation;

    public WeightAddValueDec(AddValueDec addValue) {
        super(addValue.getParOperationSettings().getProbability());
        this.operation = addValue;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        edge.setWeight(Signal.createDouble(operation.mutate(edge.getWeight().getValueDouble())));
        return edge;
    }

}
