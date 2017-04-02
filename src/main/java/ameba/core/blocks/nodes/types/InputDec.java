package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.nodes.INodeInputDec;
import ameba.core.blocks.nodes.NodeMem;

public class InputDec extends NodeMem implements INodeInputDec {
    /**
     * Construct InputDec object.
     */
    public InputDec() throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0});
        addCollectorSourceDec(new CollectorSourceDec(this));
    }

    @Override
    public void importSignal(double signal) {
        getCollectorsSourceDec().get(0).setSignal(signal);
        setSignalReady(true);

    }
}
