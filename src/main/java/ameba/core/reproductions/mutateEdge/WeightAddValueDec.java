package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Cell;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.AddValueDec;

/**
 * Created by marko on 12/28/16.
 */
public class WeightAddValueDec extends Reproduction implements IMutateEdgeDec {
    AddValueDec operation;

    public WeightAddValueDec(AddValueDec addValue) {
        super(addValue.getParOperationSettings().getProbability());
        this.operation = addValue;
    }

    @Override
    public EdgeDec mutate(EdgeDec edge) throws Exception {
        edge.setWeight(operation.mutate(edge.getWeight()));
        return edge;
    }

    @Override
    public Cell.Signal getEdgeType() {
        return Cell.Signal.DECIMAL;
    }

}
