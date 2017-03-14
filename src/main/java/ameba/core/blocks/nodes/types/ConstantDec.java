package ameba.core.blocks.nodes.types;


import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.nodes.NodeMem;

public class ConstantDec extends NodeMem {

    public ConstantDec(double par, double[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0});
        addCollectorSourceBin(new CollectorSourceBin(this));
        setParamsDec(new double[]{par});
        setParamsLimitsDec(new double[][]{parLimits});
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        getCollectorsSourceDec().get(0).setSignal(getParamsDec()[0]);
    }
}

