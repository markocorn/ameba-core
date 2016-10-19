package blocks.nodes;


import blocks.Node;

public class Constant extends Node {
    /**
     * Node of type Constant.
     *
     * @param constant       Node decimal parameter.
     * @param maxOutputEdges Maximum number of output edges.
     */
    public Constant(double constant, int maxOutputEdges) {
        super(0, maxOutputEdges);
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
}

