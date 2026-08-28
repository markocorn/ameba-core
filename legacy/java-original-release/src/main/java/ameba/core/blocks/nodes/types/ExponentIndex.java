package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.Node;


public class ExponentIndex extends Node {

    public ExponentIndex(Double par, Double[] parLimits) throws Exception {
        super(new int[]{1, 1}, new int[]{1, 1}, 1, 0, 0);
        addCollectorTarget(new CollectorTarget(this));
        addCollectorSource(new CollectorSource(this));
        getParamsDec().add(par);
        getParamsLimitsDec().add(parLimits);
    }

    @Override
    public void clcNode() {
        getCollectorsSource().get(0).setSignal(Math.pow(getCollectorsTarget().get(0).getSignal(), Math.abs(getParamsDec().get(0))));
    }
}

