package ameba.core.blocks.nodes;

import ameba.core.blocks.Edge;
import ameba.core.blocks.Node;

public class Divide extends Node {

    public Divide(int minInputEdges, int maxInputEdges, int minOutputEdges, int maxOutputEdges) {
        super(minInputEdges, maxInputEdges, minOutputEdges, maxOutputEdges);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        if (getInputEdges().size() > 0) {
            //Signal from source nodes have to be ready and not send.
            if (isNodeClcReady()) {
                setSignal(1.0);
                for (Edge edge : getInputEdges()) {
                    //multiply inverses of source input signals.
                    if (edge.getSignal() == 0) {
                        setSignal(0);
                    } else {
                        setSignal(getSignal() / edge.getSignal());
                    }
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