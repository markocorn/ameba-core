package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

public class MultiplyInt extends Node {

    public MultiplyInt(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new int[]{0, 0}, new int[]{minInpCollectors, maxInpCollectors}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0});

        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTargetInt(new CollectorTargetInt(this));
        }
        addCollectorSourceInt(new CollectorSourceInt(this));
    }

    //Calculate output value
    @Override
    public void clcNode() {
        getCollectorsSourceInt().get(0).setSignal(1);
        for (CollectorTargetInt collectorInp : getCollectorsTargetConnectedInt()) {
            getCollectorsSourceInt().get(0).setSignal(getCollectorsSourceInt().get(0).getSignal() * collectorInp.getSignal());
        }

    }
}