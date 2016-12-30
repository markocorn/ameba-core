package ameba.core.blocks.nodes;

import ameba.core.blocks.Node;

public class Input extends Node {
    /**
     * Flag that indicates signal has been imported.
     */
    private boolean importSignal;

    /**
     * Construct Input object.
     */
    public Input(int minOutputEdges, int maxOutputEdges) {
        super(0, 0, minOutputEdges, maxOutputEdges);
        importSignal = false;
    }

    /**
     * Method to set input signal imported from outside of the agent.
     *
     * @param signal Value to be imported.
     */
    public void importSignal(double signal) {
        setSignal(signal);
        importSignal = true;
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        if (importSignal) {
            setSignalReady(true);
        }
    }

    /**
     * Reset input node and it's imported signal flag.
     */
    @Override
    public void rstNode() {
        setSignalReady(false);
        importSignal = false;
    }

    @Override
    public void clearNode() {
        rstNode();
        setSignal(0.0);
    }


}
