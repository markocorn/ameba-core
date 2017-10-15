package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.Node;

public class OrBin extends Node {

    public OrBin(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{minInpCollectors, maxInpCollectors}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, 0, 0, 0);
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTargetBin(new CollectorTargetBin(this));
        }
        addCollectorSourceBin(new CollectorSourceBin(this));
    }

    //Calculate output value
    @Override
    public void clcNode() {
        getCollectorsSourceBin().get(0).setSignal(false);
        for (CollectorTargetBin collectorInp : getCollectorsTargetConnectedBinSim()) {
            //AddDec all sources signals together.
            if (collectorInp.getSignal())
                getCollectorsSourceBin().get(0).setSignal(true);
        }
    }
}

