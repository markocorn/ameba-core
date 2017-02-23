package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;


public class AddInt extends Node {

    public AddInt(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0});
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorInp(Signal.createInteger(), this));
        }
        addOutCollector(new CollectorOut(Signal.createInteger(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getOutCollectorsInt().get(0).getSignal().setValueInteger(0);
        for (CollectorInp collectorInp : getInpCollectorsConnected(Integer.class)) {
            getOutCollectorsInt().get(0).getSignal().setValueInteger(getOutCollectorsInt().get(0).getSignal().getValueInteger() + collectorInp.getSignal().getValueInteger());
        }
    }
}

