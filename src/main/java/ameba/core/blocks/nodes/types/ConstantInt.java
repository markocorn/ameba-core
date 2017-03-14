package ameba.core.blocks.nodes.types;


import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.nodes.Node;

public class ConstantInt extends Node {

    public ConstantInt(int par, int[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0});
        addCollectorSourceInt(new CollectorSourceInt(this));
        setParamsInt(new int[]{par});
        setParamsLimitsInt(new int[][]{parLimits});
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        getCollectorsSourceInt().get(0).setSignal(getParamsInt()[0]);

    }
}

