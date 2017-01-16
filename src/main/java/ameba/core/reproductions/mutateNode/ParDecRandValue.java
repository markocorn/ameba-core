package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.parametersOperations.genParDecMutation.RandValue;

/**
 * Created by marko on 12/28/16.
 */
public class ParDecRandValue implements IMutateNode {
    RandValue randValue;

    public ParDecRandValue(RandValue randValue) {
        this.randValue = randValue;
    }

    @Override
    public Node mutate(Node node) {
        if (node.getDecimalParameters().length > 0) {
            node.setDecimalParameter(0, randValue.mutate(node.getDecimalParameter(0)));
        }
        return node;
    }
}
