package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.INodeInput;
import ameba.core.blocks.nodes.NodeMem;

public class InputDec extends NodeMem implements INodeInput {
    /**
     * Construct InputDec object.
     */
    public InputDec() throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addOutCollector(new CollectorSource(Signal.createDouble(), this));
    }

    @Override
    public void importSignal(Signal signal) {
        getCollectorsSourceDec().get(0).setSignal(signal);
        setSignalReady(true);

    }
}
