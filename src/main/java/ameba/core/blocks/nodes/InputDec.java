package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorOut;

public class InputDec extends Node implements INodeInput {
    /**
     * Flag that indicates signal has been imported.
     */

    private boolean signalImported;

    /**
     * Construct InputDec object.
     */

    public InputDec(int minOutEdges, int maxOutEdges) {
        super();
        addOutCollector(Double.class, new CollectorOut(minOutEdges, maxOutEdges, this));
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        setSignalReady(true);
        if (hasOutCollector(Double.class)) {
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
        if (tClass.isAssignableFrom(Double.class)) {
            setSignalDec((Double) signal);
            signalImported = true;
        }
    }
}
