package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;


public class AddDec extends Node {

    public AddDec(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorTarget(Signal.createDouble(), this));
        }
        addOutCollector(new CollectorSource(Signal.createDouble(), this));
    }

    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceDec().get(0).getSignal().setValueDouble(0.0);
        for (CollectorTarget collectorInp : getInpCollectorsConnected(Double.class)) {
            getCollectorsSourceDec().get(0).getSignal().setValueDouble(getCollectorsSourceDec().get(0).getSignal().getValueDouble() + collectorInp.getSignal().getValueDouble());
        }
    }
}

