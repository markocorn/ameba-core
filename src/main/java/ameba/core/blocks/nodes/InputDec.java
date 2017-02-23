package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class InputDec extends NodeMem implements INodeInput {
    /**
     * Construct InputDec object.
     */
    public InputDec() throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addOutCollector(new CollectorOut(Signal.createDouble(), this));
    }

    @Override
    public void importSignal(Signal signal) {
        getOutCollectorsDec().get(0).setSignal(signal);
        setSignalReady(true);

    }
}
