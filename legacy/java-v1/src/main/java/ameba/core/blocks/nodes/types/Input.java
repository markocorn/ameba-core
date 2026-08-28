package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.NodeMem;

public class Input extends NodeMem {
    /**
     * Construct InputDec object.
     */
    public Input() throws Exception {
        super(new int[]{0, 0}, new int[]{1, 1}, 0, 0, 0);
        addCollectorSource(new CollectorSource(this));
    }

    public void importSignal(double signal) {
        getCollectorsSource().get(0).setSignal(signal);
        setSignalReady(true);

    }
}
