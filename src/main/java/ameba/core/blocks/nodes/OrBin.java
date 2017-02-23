package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class OrBin extends Node {

    public OrBin(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorInp(Signal.createBoolean(), this));
        }
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getOutCollectorsDec().get(0).getSignal().setValueBoolean(false);
        for (CollectorInp collectorInp : getInpCollectorsConnected(Boolean.class)) {
            //AddDec all sources signals together.
            if (collectorInp.getSignal().getValueBoolean())
                getOutCollectorsDec().get(0).getSignal().setValueBoolean(true);
        }
    }
}

