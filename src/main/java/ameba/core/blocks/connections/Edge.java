package ameba.core.blocks.connections;

public class Edge<T1> implements IEdge, Cloneable {
    /**
     * Source collector of the edge.
     */
    private CollectorOut source;
    /**
     * Target collector of the edge.
     */
    private CollectorInp target;

    /**
     * Flag that indicates signal was signalSend trough the edge.
     */
    private boolean signalSend;

    private T1 weight;

    public Edge(CollectorOut source, CollectorInp target, T1 weight) {
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

    /**
     * Reset edge's send flag.
     */
    public void rstEdge() {
        signalSend = false;
    }

    /**
     * Check if the signal has been send trough edge.
     *
     * @return
     */
    public boolean isSignalSend() {
        return signalSend;
    }

    /**
     * Set signal send flag.
     *
     * @param signalSend New value of send flag.
     */
    public void setSignalSend(boolean signalSend) {
        this.signalSend = signalSend;
    }


    @Override
    public <T> T getWeight() {
        return (T) weight;
    }

    @Override
    public <T> void setWeight(T weight) {
        this.weight = (T1) weight;
    }

    @Override
    public <T> T getSignal(Class<T> tClass) {
        if (isSignalReady()) {
            setSignalSend(true);
            if (tClass.isAssignableFrom(Double.class)) {
                Double out = (Double) getSource().getSignal(tClass) * (Double) weight;
                return (T) out;
            }
            if (tClass.isAssignableFrom(Integer.class)) {
                Integer out = (Integer) getSource().getSignal(tClass) * (Integer) weight;
                return (T) out;
            }
            if (tClass.isAssignableFrom(Boolean.class)) {
                Boolean out = (Boolean) getSource().getSignal(tClass);
                if ((Boolean) weight) {
                    out = !out;
                }
                return (T) out;
            }
        }
        return null;
    }
}
