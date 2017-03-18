package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.AddValueBin;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParAddValueBin extends Reproduction implements IMutateNode {
    AddValueBin operationType;
    Random random;

    public ParAddValueBin(AddValueBin operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
        random = new Random();
    }

    @Override
    public Node mutate(Node node) throws Exception {
        if (node.getParamsBin().size() > 0) {
            int ind = random.nextInt(node.getParamsBin().size());
            node.getParamsBin().set(ind, operationType.mutate(node.getParamsBin().get(ind)));
            return node;
        }
        return null;
    }
}
