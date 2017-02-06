package ameba.core.blocks.nodes;


import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;

public class Constant extends Node {

    public Constant(Signal outSignal, int minOutputEdges, int maxOutputEdges, Signal constant) throws Exception {
        super(0, 0, 0, 10);
        addOutCollector(new CollectorOut(outSignal, minOutputEdges, maxOutputEdges, this));
        getOutCollectors().get(0).setSignal(constant);
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        switch (getState()) {
            case 0:
                if (isSignalReady()) {
                    setState(1);
                }
            case 1:
                if (isSignalSend()) {
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

