package ameba.core.blocks.nodes;


import ameba.core.blocks.Node;

public class Constant extends Node {
    /**
     *
     * @param minOutputEdges
     * @param maxOutputEdges
     * @param constant
     */
    public Constant(int minOutputEdges, int maxOutputEdges, double constant) {
        super(0, 0, minOutputEdges, maxOutputEdges);
        setDecimalParameters(new double[]{constant});
    }

    /**
     * Calculate node's output signal.
     */
    @Override
    public void clcNode() {
        if (!isSignalReady()) {
            setSignal(getDecimalParameters()[0]);
            setSignalReady(true);
        }
    }

    @Override
    public void clearNode() {
        rstNode();
        setSignal(0.0);
    }

    @Override
    public void rstNode() {
        setSignalReady(false);
    }
}

