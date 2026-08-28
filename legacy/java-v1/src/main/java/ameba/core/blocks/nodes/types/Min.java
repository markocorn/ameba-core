package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.Node;

public class Min extends Node {

    public Min(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(new int[]{minInpCollectors, maxInpCollectors}, new int[]{1, 1}, 0, 0, 0);
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTarget(new CollectorTarget(this));
        }
        addCollectorSource(new CollectorSource(this));
    }

    @Override
    public void clcNode() {
        double min = Double.MAX_VALUE;
        for (CollectorTarget collector : getCollectorsTargetConnected()) {
            if (collector.getSignal() < min) min = collector.getSignal();
        }
        if (getCollectorsTargetConnected().isEmpty()) min = 0.0;
        getCollectorsSource().get(0).setSignal(min);
    }
}
