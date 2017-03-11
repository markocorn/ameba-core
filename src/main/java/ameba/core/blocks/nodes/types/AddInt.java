package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;


public class AddInt extends Node {

    public AddInt(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0});
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorTarget(Signal.createInteger(), this));
        }
        addOutCollector(new CollectorSource(Signal.createInteger(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceInt().get(0).getSignal().setValueInteger(0);
        for (CollectorTarget collectorInp : getInpCollectorsConnected(Integer.class)) {
            getCollectorsSourceInt().get(0).getSignal().setValueInteger(getCollectorsSourceInt().get(0).getSignal().getValueInteger() + collectorInp.getSignal().getValueInteger());
        }
    }
}

