package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;

public class Input extends Node implements INodeInput {
    /**
     * Construct InputDec object.
     */
    public Input(Signal signal, int minOutEdges, int maxOutEdges) throws Exception {
        super(0, 0, 1, 1);
        addOutCollector(new CollectorOut(signal.clone(), minOutEdges, maxOutEdges, this));
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
            case 1:
                if (isSignalClcDone()) {
                    setSignalReady(true);
                    setState(2);
                }
            case 2:
                if (isSignalReady()) {
                    setState(3);
                }
            case 3:
                if (isSignalSend()) {
                    setState(4);
                }
            case 4:
        }
    }

    @Override
    public void importSignal(Signal signal) {
        getOutCollectors().get(0).setSignal(signal);
        setSignalInputsReady(true);

    }
}
