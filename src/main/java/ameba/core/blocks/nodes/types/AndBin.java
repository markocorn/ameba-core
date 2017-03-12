package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

public class AndBin extends Node {

    public AndBin(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorTarget(Signal.createBoolean(), this));
        }
        addOutCollector(new CollectorSource(Signal.createBoolean(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceBin().get(0).getSignal().setValueBoolean(true);
        for (CollectorTarget collectorInp : getCollectorsTargetConnected(Boolean.class)) {
            //AddDec all sources signals together.
            if (!collectorInp.getSignal().getValueBoolean())
                getCollectorsSourceBin().get(0).getSignal().setValueBoolean(false);
        }
    }
}

