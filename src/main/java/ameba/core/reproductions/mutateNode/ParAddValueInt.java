package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.Cell;
import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.AddValueInt;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParAddValueInt extends Reproduction implements IMutateNode {
    AddValueInt operationType;
    Random random;

    public ParAddValueInt(AddValueInt operationType, long seed) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
        random = new Random(seed);
    }

    @Override
    public Node mutate(Node node) throws Exception {
        if (node.getParamsInt().size() > 0) {
            int ind = random.nextInt(node.getParamsInt().size());
            node.setParamInt(ind, operationType.mutate(node.getParamsInt().get(ind)));
            return node;
        }
        return null;
    }

    @Override
    public Cell.ParType getType() {
        return Cell.ParType.INTEGER;
    }
}
