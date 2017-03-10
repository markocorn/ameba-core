package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Edge;
import ameba.core.blocks.Signal;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.AddValueBin;

/**
 * Created by marko on 12/28/16.
 */
public class WeightAddValueBin extends Reproduction implements IMutateEdge {
    AddValueBin operation;

    public WeightAddValueBin(AddValueBin addValue) {
        super(addValue.getParOperationSettings().getProbability());
        this.operation = addValue;
    }

    @Override
    public Edge mutate(Edge edge) throws Exception {
        edge.setWeight(Signal.createBoolean(operation.mutate(edge.getWeight().getValueBoolean())));
        return edge;
    }

}
