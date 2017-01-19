package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorOut;

public class InputBin extends Node implements INodeInput {
    /**
     * Flag that indicates signal has been imported.
     */

    private boolean signalImported;

    /**
     * Construct InputDec object.
     */

    public InputBin(int minOutEdges, int maxOutEdges) {
        super();
        addOutCollector(Boolean.class, new CollectorOut(minOutEdges, maxOutEdges, this));
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        setSignalReady(true);
        if (hasOutCollector(Boolean.class)) {
            if (!signalImported) {
                setSignalReady(false);
            }
        }
    }

    /**
     * Reset input node and it's imported signal flag.
     */
    @Override
    public void rstNode() {
        setSignalReady(false);
        signalImported = false;
    }


    @Override
    public <T> void importSignal(Class<T> tClass, Object signal) {
        if (tClass.isAssignableFrom(Boolean.class)) {
            setSignalBin((Boolean) signal);
            signalImported = true;
        }
    }
}
