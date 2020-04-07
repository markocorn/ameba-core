package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.Node;

public class Divide extends Node {

    public Divide(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new int[]{minInpCollectors, maxInpCollectors}, new int[]{1, 1}, 0, 0, 0);
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTarget(new CollectorTarget(this));
        }
        addCollectorSource(new CollectorSource(this));
    }

    //Calculate output value
    @Override
    public void clcNode() {
        getCollectorsSource().get(0).setSignal(1.0);
        for (CollectorTarget collectorInp : getCollectorsTargetConnected()) {
            if (collectorInp.getSignal() == 0) {
                getCollectorsSource().get(0).setSignal(0.0);
                break;
            } else {
                getCollectorsSource().get(0).setSignal(getCollectorsSource().get(0).getSignal() / collectorInp.getSignal());
            }
        }
    }
}