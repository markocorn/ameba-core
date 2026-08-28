package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.Node;


public class NeuronStep extends Node {

    public NeuronStep(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new int[]{minInpCollectors, maxInpCollectors}, new int[]{1, 1}, 0, 0, 0);
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTarget(new CollectorTarget(this));
        }
        addCollectorSource(new CollectorSource(this));
    }

    @Override
    public void clcNode() {
        getCollectorsSource().get(0).setSignal(0.0);
        for (CollectorTarget collector : getCollectorsTargetConnected()) {
            getCollectorsSource().get(0).setSignal(getCollectorsSource().get(0).getSignal() + collector.getSignal());
        }
        if (getCollectorsSource().get(0).getSignal() > 0) {
            getCollectorsSource().get(0).setSignal(1.0);
        } else {
            getCollectorsSource().get(0).setSignal(-1.0);
        }
        clcCollectorsTargetConnected();
    }
}

