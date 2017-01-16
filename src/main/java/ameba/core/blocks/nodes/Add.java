package ameba.core.blocks.nodes;

import ameba.core.blocks.edges.Edge;

public class Add extends Node {

    public Add(int minInputEdges, int maxInputEdges, int minOutputEdges, int maxOutputEdges) {
        super(minInputEdges, maxInputEdges, minOutputEdges, maxOutputEdges);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        if (getInputEdges().size() > 0) {
            //Signal from source nodes have to be ready and not send.
            if (isNodeClcReady()) {
                setSignal(0.0);
                for (Edge edge : getInputEdges()) {
                    //Add all sources signals together.
                    setSignal(edge.getSignal() + getSignal());
                }
                setSignalReady(true);
            }
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

