package ameba.core.blocks.nodes;


import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Signal;

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
            case 1:
                if (isSignalSend() || getOutCollectors().get(0).getEdges().size() > 0) {
                    setState(4);
                }
            case 4:
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

