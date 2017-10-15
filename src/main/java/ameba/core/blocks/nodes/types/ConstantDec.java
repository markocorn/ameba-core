package ameba.core.blocks.nodes.types;


import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.nodes.NodeMem;

public class ConstantDec extends NodeMem {

    public ConstantDec(Double par, Double[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, 0, 0, 0);
        addCollectorSourceDec(new CollectorSourceDec(this));
        getParamsDec().add(par);
        getParamsLimitsDec().add(parLimits);
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        getCollectorsSourceDec().get(0).setSignal(getParamsDec().get(0));
    }
}

