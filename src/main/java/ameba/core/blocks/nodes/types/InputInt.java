package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.nodes.INodeInputInt;
import ameba.core.blocks.nodes.NodeMem;

public class InputInt extends NodeMem implements INodeInputInt {
    /**
     * Construct InputDec object.
     */
    public InputInt() throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, 0, 0, 0);
        addCollectorSourceInt(new CollectorSourceInt(this));
    }

    @Override
    public void importSignal(int signal) {
        getCollectorsSourceInt().get(0).setSignal(signal);
        setSignalReady(true);

    }
}
