package ameba.core.blocks.collectors;

import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorTargetDec extends CollectorTarget implements ICollectorDec {

    public CollectorTargetDec(Node node) {
        super(node);
    }

    @Override
    public double getSignal() {
        return 0;
    }
}
