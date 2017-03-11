package ameba.core.blocks.nodes.types;


import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.NodeMem;

public class ConstantDec extends NodeMem {

    public ConstantDec(Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addOutCollector(new CollectorSource(Signal.createDouble(), this));
        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        getCollectorsSourceDec().get(0).setSignal(getParams().get(0));
        setState(1);
    }
}

