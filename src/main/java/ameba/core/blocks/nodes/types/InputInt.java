package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.NodeMem;

public class InputInt extends NodeMem implements INodeInput {
    /**
     * Construct InputDec object.
     */
    public InputInt() throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0});
        addOutCollector(new CollectorSource(Signal.createInteger(), this));
    }

    @Override
    public void importSignal(Signal signal) {
        getCollectorsSourceInt().get(0).setSignal(signal);
        setSignalReady(true);

    }
}
