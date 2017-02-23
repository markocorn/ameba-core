package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class InputBin extends NodeMem implements INodeInput {
    /**
     * Construct InputDec object.
     */
    public InputBin() throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));
    }

    @Override
    public void importSignal(Signal signal) {
        getOutCollectorsBin().get(0).setSignal(signal);
        setSignalReady(true);
    }
}
