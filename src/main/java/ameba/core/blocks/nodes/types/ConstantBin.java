package ameba.core.blocks.nodes.types;


import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.nodes.NodeMem;

public class ConstantBin extends NodeMem {

    public ConstantBin(boolean par, boolean[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});
        addCollectorSourceBin(new CollectorSourceBin(this));
        setParamsBin(new boolean[]{par});
        setParamsLimitsBin(new boolean[][]{parLimits});
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        getCollectorsSourceBin().get(0).setSignal(getParamsBin()[0]);
    }
}

