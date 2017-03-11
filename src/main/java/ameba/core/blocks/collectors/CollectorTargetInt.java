package ameba.core.blocks.collectors;

import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorTargetInt extends CollectorTarget implements ICollectorInt {

    public CollectorTargetInt(Node node) {
        super(node);
    }

    @Override
    public int getSignal() {
        return 0;
    }
}
