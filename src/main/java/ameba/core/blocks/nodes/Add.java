package ameba.core.blocks.nodes;

import ameba.core.blocks.Edge;
import ameba.core.blocks.Node;

public class Add extends Node {

    public Add(int maxInputEdges, int maxOutputEdges) {
        super(maxInputEdges, maxOutputEdges);
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
}

