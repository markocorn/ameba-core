package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.NodeMem;

public class InputBin extends NodeMem implements INodeInput {
    /**
     * Construct InputDec object.
     */
    public InputBin() throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addOutCollector(new CollectorSource(Signal.createBoolean(), this));
    }

    @Override
    public void importSignal(Signal signal) {
        getCollectorsSourceBin().get(0).setSignal(signal);
        setSignalReady(true);
    }
}
