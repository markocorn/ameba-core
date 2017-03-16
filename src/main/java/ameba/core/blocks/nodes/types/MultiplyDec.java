package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.Node;

public class MultiplyDec extends Node {

    public MultiplyDec(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new int[]{minInpCollectors, maxInpCollectors}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0});

        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTargetDec(new CollectorTargetDec(this));
        }
        addCollectorSourceDec(new CollectorSourceDec(this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceDec().get(0).setSignal(1.0);
        for (CollectorTargetDec collectorInp : getCollectorsTargetConnectedDec()) {
            getCollectorsSourceDec().get(0).setSignal(getCollectorsSourceDec().get(0).getSignal() * collectorInp.getSignal());
        }
    }
}