package ameba.core.blocks.collectors;

import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 1/19/17.
 */
public class CollectorSource extends Collector {

    public CollectorSource(Node node) {
        super(node);
    }

    @Override
    public boolean isSignalReady() {
        return getNodeAttached().isSignalReady();
    }


}
