package ameba.core.blocks.nodes.types;


import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.nodes.Node;

public class ConstantInt extends Node {

    public ConstantInt(Integer par, Integer[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, 0, 1, 0);
        addCollectorSourceInt(new CollectorSourceInt(this));
        getParamsInt().add(par);
        getParamsLimitsInt().add(parLimits);
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        getCollectorsSourceInt().get(0).setSignal(getParamsInt().get(0));

    }

    @Override
    public void clearNode() {
        super.clearNode();
        getCollectorsSourceInt().get(0).setSignal(getParamsInt().get(0));
    }
}

