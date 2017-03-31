package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;


public class AddInt extends Node {

    public AddInt(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new int[]{0, 0}, new int[]{minInpCollectors, maxInpCollectors}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0});
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTargetInt(new CollectorTargetInt(this));
        }
        addCollectorSourceInt(new CollectorSourceInt(this));
    }

    @Override
    public void clcNode() {
        getCollectorsSourceInt().get(0).setSignal(0);
        for (CollectorTargetInt collector : getCollectorsTargetConnectedInt()) {
            getCollectorsSourceInt().get(0).setSignal(getCollectorsSourceInt().get(0).getSignal() + collector.getSignal());
        }
    }
}

