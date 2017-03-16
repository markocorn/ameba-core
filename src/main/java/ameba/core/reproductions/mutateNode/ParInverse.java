package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.InverseValue;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParInverse extends Reproduction implements IMutateNode {
    InverseValue operationType;
    Random random;

    public ParInverse(InverseValue operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
        random = new Random();
    }

    @Override
    public Node mutate(Node node) throws Exception {
        if (node.getParamsDec().length > 0) {
            int ind = random.nextInt(node.getParamsDec().length);
            node.getParamsDec()[ind] = operationType.mutate(node.getParamsDec()[ind]);
            return node;
        }
        return null;
    }
}
