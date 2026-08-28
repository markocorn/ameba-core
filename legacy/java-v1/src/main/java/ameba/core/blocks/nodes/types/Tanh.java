package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.Node;

public class Tanh extends Node {

    public Tanh() throws Exception {
        super(new int[]{1, 1}, new int[]{1, 1}, 0, 0, 0);
        addCollectorTarget(new CollectorTarget(this));
        addCollectorSource(new CollectorSource(this));
    }

    @Override
    public void clcNode() {
        getCollectorsSource().get(0).setSignal(Math.tanh(getCollectorsTarget().get(0).getSignal()));
    }
}
