package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.parametersOperations.genParMutation.InverseValue;

/**
 * Created by marko on 12/28/16.
 */
public class ParDecInverse implements IMutateNode {
    InverseValue inverseValue;

    public ParDecInverse(InverseValue inverseValue) {
        this.inverseValue = inverseValue;
    }

    @Override
    public Node mutate(Node node) {
//        if (node.getParamsDec().length > 0) {
//            node.setDecimalParameter(0, inverseValue.mutate(node.getDecimalParameter(0)));
//        }
        return node;
    }
}
