package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.nodes.INodeInputBin;
import ameba.core.blocks.nodes.NodeMem;

public class InputBin extends NodeMem implements INodeInputBin {
    /**
     * Construct InputDec object.
     */
    public InputBin() throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});
        addCollectorSourceBin(new CollectorSourceBin(this));
    }

    @Override
    public void importSignal(boolean signal) {
        getCollectorsSourceBin().get(0).setSignal(signal);
        setSignalReady(true);
    }
}
