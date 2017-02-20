package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.parametersOperations.genParMutation.MixSign;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParMixSign implements IMutateNode {
    MixSign operationType;
    Random random;

    public ParMixSign(MixSign mixSign) {
        this.operationType = mixSign;
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
