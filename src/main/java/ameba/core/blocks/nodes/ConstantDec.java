package ameba.core.blocks.nodes;


import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class ConstantDec extends NodeMem {

    public ConstantDec(Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addOutCollector(new CollectorOut(Signal.createDouble(), this));
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

