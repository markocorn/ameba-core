package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.Node;


public class Add extends Node {

    public Add(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new int[]{minInpCollectors, maxInpCollectors}, new int[]{1, 1}, 0, 0, 0);
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTarget(new CollectorTarget(this));
        }
        addCollectorSource(new CollectorSource(this));
    }

    @Override
    public void clcNode() {
//        System.out.print(getCollectorsSource().get(0).getSignal()+" ");
        getCollectorsSource().get(0).setSignal(0.0);
        for (CollectorTarget collector : getCollectorsTargetConnected()) {
            getCollectorsSource().get(0).setSignal(getCollectorsSource().get(0).getSignal() + collector.getSignal());
        }
    }
}

