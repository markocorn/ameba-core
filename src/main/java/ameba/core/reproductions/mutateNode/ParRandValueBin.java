package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.Cell;
import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.RandValueBin;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParRandValueBin extends Reproduction implements IMutateNode {
    RandValueBin operationType;
    Random random;

    public ParRandValueBin(RandValueBin operationType) {
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

    @Override
    public Cell.Signal getType() {
        return Cell.Signal.BOOLEAN;
    }
}
