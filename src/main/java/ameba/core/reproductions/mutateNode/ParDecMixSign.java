package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.parametersOperations.genParDecMutation.MixSign;

/**
 * Created by marko on 12/28/16.
 */
public class ParDecMixSign implements IMutateNode {
    MixSign mixSign;

    public ParDecMixSign(MixSign mixSign) {
        this.mixSign = mixSign;
    }

    @Override
    public Node mutate(Node node) {
        if (node.getDecimalParameters().length > 0) {
            node.setDecimalParameter(0, mixSign.mutate(node.getDecimalParameter(0)));
        }
        return node;
    }
}
