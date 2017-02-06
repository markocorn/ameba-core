package ameba.core.blocks.connections;

public class Edge implements Cloneable {
    /**
     * Source collector of the edge.
     */
    private CollectorOut source;
    /**
     * Target collector of the edge.
     */
    private CollectorInp target;

    private Signal weight;

    public Edge(CollectorOut source, CollectorInp target, Signal weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }


    public CollectorOut getSource() {
        return source;
    }


    public void setSource(CollectorOut source) {
        this.source = source;
    }

    /**
     * Get edge's target node.
     *
     * @return
     */
    public CollectorInp getTarget() {
        return target;
    }

    /**
     * Set edge's target node.
     *
     * @param target New target node.
     */
    public void setTarget(CollectorInp target) {
        this.target = target;
    }

    /**
     * Check if the signal form the source node is ready to be signalSend.
     *
     * @return
     */
    public boolean isSignalReady() {
        return source.isSignalReady();
    }


    public Signal getWeight() {
        return weight;
    }

    public void setWeight(Signal weight) {
        this.weight = weight;
    }

    public Signal getSignal() throws Exception {
        if (isSignalReady()) {
            source.getNodeAttached().setSignalSend(true);
            return Signal.multiplySignal(source.getSignal(), weight);
        }
        return null;
    }
}
