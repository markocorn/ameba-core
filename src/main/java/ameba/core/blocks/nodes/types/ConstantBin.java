package ameba.core.blocks.nodes.types;


import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.NodeMem;

public class ConstantBin extends NodeMem {

    public ConstantBin(Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addOutCollector(new CollectorSource(Signal.createBoolean(), this));
        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        getCollectorsSourceBin().get(0).setSignal(getParams().get(0));
        setState(1);
    }
}

