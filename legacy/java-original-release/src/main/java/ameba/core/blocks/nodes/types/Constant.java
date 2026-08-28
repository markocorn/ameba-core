package ameba.core.blocks.nodes.types;


import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.NodeMem;

public class Constant extends NodeMem {

    public Constant(Double par, Double[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{1, 1}, 1, 0, 0);
        addCollectorSource(new CollectorSource(this));
        getParamsDec().add(par);
        getParamsLimitsDec().add(parLimits);
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        getCollectorsSource().get(0).setSignal(getParamsDec().get(0));
    }

    @Override
    public void clearNode() {
        super.clearNode();
        getCollectorsSource().get(0).setSignal(getParamsDec().get(0));
    }
}

