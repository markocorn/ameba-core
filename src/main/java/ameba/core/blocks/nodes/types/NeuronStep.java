package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.Node;


public class NeuronStep extends Node {

    public NeuronStep(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new int[]{minInpCollectors, maxInpCollectors}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, 0, 0, 0);
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTargetDec(new CollectorTargetDec(this));
        }
        addCollectorSourceDec(new CollectorSourceDec(this));
    }

    @Override
    public void clcNode() {
        getCollectorsSourceDec().get(0).setSignal(0.0);
        for (CollectorTargetDec collector : getCollectorsTargetConnectedDecSim()) {
            getCollectorsSourceDec().get(0).setSignal(getCollectorsSourceDec().get(0).getSignal() + collector.getSignal());
        }
        if (getCollectorsSourceDec().get(0).getSignal() > 0) {
            getCollectorsSourceDec().get(0).setSignal(1.0);
        } else {
            getCollectorsSourceDec().get(0).setSignal(0.0);
        }
    }
}
