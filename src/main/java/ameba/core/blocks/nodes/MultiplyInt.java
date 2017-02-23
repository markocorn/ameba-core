package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class MultiplyInt extends Node {

    public MultiplyInt(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorInp(Signal.createDouble(), this));
        }
        addOutCollector(new CollectorOut(Signal.createDouble(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getOutCollectorsDec().get(0).getSignal().setValueInteger(1);
        for (CollectorInp collectorInp : getInpCollectors()) {
            getOutCollectorsDec().get(0).getSignal().setValueInteger(getOutCollectorsDec().get(0).getSignal().getValueInteger() * collectorInp.getSignal().getValueInteger());
        }

    }
}