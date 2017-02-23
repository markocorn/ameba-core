package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class Divide extends Node {

    public Divide(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorInp(Signal.createDouble(), this));
        }
        addOutCollector(new CollectorOut(Signal.createDouble(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getOutCollectorsDec().get(0).getSignal().setValueDouble(1.0);
        for (CollectorInp collectorInp : getInpCollectorsDec()) {
            if (collectorInp.getSignal().getValueDouble().equals(0.0)) {
                getOutCollectorsDec().get(0).getSignal().setValueDouble(0.0);
                break;
            } else {
                getOutCollectorsDec().get(0).getSignal().setValueDouble(getOutCollectorsDec().get(0).getSignal().getValueDouble() / collectorInp.getSignal().getValueDouble());
            }
        }
    }
}