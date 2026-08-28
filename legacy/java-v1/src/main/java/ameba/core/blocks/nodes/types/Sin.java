package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.Node;

public class Sin extends Node {

    public Sin(Double phase, Double[] phaseLimits) throws Exception {
        super(new int[]{1, 1}, new int[]{1, 1}, 1, 0, 0);
        addCollectorTarget(new CollectorTarget(this));
        addCollectorSource(new CollectorSource(this));
        getParamsDec().add(phase);
        getParamsLimitsDec().add(phaseLimits);
    }

    @Override
    public void clcNode() {
        getCollectorsSource().get(0).setSignal(Math.sin(getCollectorsTarget().get(0).getSignal() + getParamsDec().get(0)));
    }
}
