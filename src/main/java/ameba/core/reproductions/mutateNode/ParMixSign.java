package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParMutation.MixSignDec;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParMixSign extends Reproduction implements IMutateNode {
    MixSignDec operationType;
    Random random;

    public ParMixSign(MixSignDec operationType) {
        super(operationType.getSettings().getProbability());
        this.operationType = operationType;
        random = new Random();
    }

    @Override
    public Node mutate(Node node) throws Exception {
        if (node.getParams().size() > 0) {
            int ind = random.nextInt(node.getParams().size());
            node.setParam(ind, operationType.mutate(node.getParam(ind)));
            return node;
        }
        return null;
    }
}
