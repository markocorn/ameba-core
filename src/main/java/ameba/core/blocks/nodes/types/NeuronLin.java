package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.Node;


public class NeuronLin extends Node {

    public NeuronLin(int minInpCollectors, int maxInpCollectors, Double par, Double[] parLimits) throws Exception {
        super(new int[]{minInpCollectors, maxInpCollectors}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, 0, 0, 0);
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTargetDec(new CollectorTargetDec(this));
        }
        addCollectorSourceDec(new CollectorSourceDec(this));

        getParamsDec().add(par);
        getParamsLimitsDec().add(parLimits);
    }

    @Override
    public void clcNode() {
        getCollectorsSourceDec().get(0).setSignal(getParamsDec().get(0));
        for (CollectorTargetDec collector : getCollectorsTargetConnectedDecSim()) {
            getCollectorsSourceDec().get(0).setSignal(getCollectorsSourceDec().get(0).getSignal() + collector.getSignal());
        }
    }
}

