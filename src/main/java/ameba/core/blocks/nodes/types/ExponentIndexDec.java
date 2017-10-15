package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.Node;


public class ExponentIndexDec extends Node {

    public ExponentIndexDec(Double par, Double[] parLimits) throws Exception {
        super(new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, 1, 0, 0);
        addCollectorTargetDec(new CollectorTargetDec(this));
        addCollectorSourceDec(new CollectorSourceDec(this));
        getParamsDec().add(par);
        getParamsLimitsDec().add(parLimits);
    }

    @Override
    public void clcNode() {
        getCollectorsSourceDec().get(0).setSignal(Math.pow(getCollectorsTargetDec().get(0).getSignal(), Math.abs(getParamsDec().get(0))));
    }
}

