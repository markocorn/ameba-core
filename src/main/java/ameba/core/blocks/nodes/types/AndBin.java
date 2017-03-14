package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.Node;

public class AndBin extends Node {

    public AndBin(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{minInpCollectors, maxInpCollectors}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTargetBin(new CollectorTargetBin(this));
        }
        addCollectorSourceBin(new CollectorSourceBin(this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceBin().get(0).setSignal(true);
        for (CollectorTargetBin collector : getCollectorsTargetConnectedBin()) {
            if (!collector.getSignal())
                getCollectorsSourceBin().get(0).setSignal(false);
        }
    }
}

