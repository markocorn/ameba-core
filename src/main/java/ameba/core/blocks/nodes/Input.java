package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class Input extends Node implements INodeInput {
    /**
     * Construct InputDec object.
     */
    public Input(Signal type) throws Exception {
        super(0, 0, 1, 1);
        addOutCollector(new CollectorOut(type.clone(), this));
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    setSignalClcDone(true);
                    setState(1);
                }
                break;
            case 1:
                if (isSignalClcDone()) {
                    setSignalReady(true);
                    setState(2);
                }
                break;
            case 2:
                if (isSignalReady()) {
                    setState(3);
                }
                break;
            case 3:
                if (isSignalSend() || getOutCollectors().get(0).getEdges().size() == 0) {
                    setState(4);
                }
                break;
            case 4:
        }
    }

    @Override
    public void importSignal(Signal signal) {
        getOutCollectors().get(0).setSignal(signal);
        setSignalInputsReady(true);

    }
}
