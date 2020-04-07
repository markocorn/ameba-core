package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.Node;


public class NeuronLin extends Node {

    public NeuronLin(int minInpCollectors, int maxInpCollectors, Double par, Double[] parLimits) throws Exception {
        super(new int[]{minInpCollectors, maxInpCollectors}, new int[]{1, 1}, 0, 0, 0);
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTarget(new CollectorTarget(this));
        }
        addCollectorSource(new CollectorSource(this));

        getParamsDec().add(par);
        getParamsLimitsDec().add(parLimits);
    }

    @Override
    public void clcNode() {
        getCollectorsSource().get(0).setSignal(getParamsDec().get(0));
        for (CollectorTarget collector : getCollectorsTargetConnected()) {
            getCollectorsSource().get(0).setSignal(getCollectorsSource().get(0).getSignal() + collector.getSignal());
        }
        clcCollectorsTargetConnected();
    }
}

