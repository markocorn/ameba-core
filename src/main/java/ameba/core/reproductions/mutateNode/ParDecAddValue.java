package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.parametersOperations.genParDecMutation.AddValue;

/**
 * Created by marko on 12/28/16.
 */
public class ParDecAddValue implements IMutateNode {
    AddValue addValue;

    public ParDecAddValue(AddValue addValue) {
        this.addValue = addValue;
    }

    @Override
    public Node mutate(Node node) {
        if (node.getDecimalParameters().length > 0) {
            node.setDecimalParameter(0, addValue.mutate(node.getDecimalParameter(0)));
        }
        return node;
    }
}
