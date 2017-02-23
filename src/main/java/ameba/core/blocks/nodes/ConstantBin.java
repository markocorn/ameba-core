package ameba.core.blocks.nodes;


import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class ConstantBin extends NodeMem {

    public ConstantBin(Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));
        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        getOutCollectorsDec().get(0).setSignal(getParams().get(0));
        setState(1);
    }
}

