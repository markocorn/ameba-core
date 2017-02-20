package ameba.core.blocks.nodes;


import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class Constant extends Node {

    public Constant(Signal par, Signal[] parLimits) throws Exception {
        super(0, 0, 0, 10);
        addOutCollector(new CollectorOut(par.clone(), this));


        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        switch (getState()) {
            case 0:
                if (isSignalReady()) {
                    getOutCollectors().get(0).setSignal(getParams().get(0));
                    setState(1);
                }
                break;
            case 1:
                if (isSignalSend() || getOutCollectors().get(0).getEdges().size() == 0) {
                    setState(4);
                }
                break;
            case 4:
                break;
        }
    }

    @Override
    public void clearNode() {
        rstNode();
    }

    @Override
    public void rstNode() {
        setSignalReady(true);
        setSignalSend(false);
        setState(0);
    }
}

