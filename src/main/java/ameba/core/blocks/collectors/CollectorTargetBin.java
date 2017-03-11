package ameba.core.blocks.collectors;

import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorTargetBin extends CollectorTarget implements ICollectorBin {

    public CollectorTargetBin(Node node) {
        super(node);
    }

    @Override
    public boolean getSignal() {
        return false;
    }
}
