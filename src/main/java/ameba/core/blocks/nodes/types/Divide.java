package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

public class Divide extends Node {

    public Divide(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorTarget(Signal.createDouble(), this));
        }
        addOutCollector(new CollectorSource(Signal.createDouble(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceDec().get(0).getSignal().setValueDouble(1.0);
        for (CollectorTarget collectorInp : getInpCollectorsConnected(Double.class)) {
            if (collectorInp.getSignal().getValueDouble().equals(0.0)) {
                getCollectorsSourceDec().get(0).getSignal().setValueDouble(0.0);
                break;
            } else {
                getCollectorsSourceDec().get(0).getSignal().setValueDouble(getCollectorsSourceDec().get(0).getSignal().getValueDouble() / collectorInp.getSignal().getValueDouble());
            }
        }
    }
}