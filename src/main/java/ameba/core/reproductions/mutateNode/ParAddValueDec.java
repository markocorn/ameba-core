package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.Cell;
import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.AddValueDec;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParAddValueDec extends Reproduction implements IMutateNode {
    AddValueDec operationType;
    Random random;

    public ParAddValueDec(AddValueDec operationType, long seed) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
        random = new Random(seed);
    }

    @Override
    public Node mutate(Node node) throws Exception {
        if (node.getParamsDec().size() > 0) {
            int ind = random.nextInt(node.getParamsDec().size());
            node.setParamDec(ind, operationType.mutate(node.getParamsDec().get(ind)));
            return node;
        }
        return null;
    }

    @Override
    public Cell.ParType getType() {
        return Cell.ParType.DECIMAL;
    }
}
