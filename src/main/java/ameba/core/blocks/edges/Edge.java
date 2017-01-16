package ameba.core.blocks.edges;

import ameba.core.blocks.Collector;

public class Edge<T> implements Cloneable {
    /**
     * Source collector of the edge.
     */
    private Collector source;
    /**
     * Target collcetor of the edge.
     */
    private Collector target;

    /**
     * Flag that indicates signal was signalSend trough the edge.
     */
    private boolean signalSend;

    private T weight;

    public Edge(Collector source, Collector target, T weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }


    public Collector getSource() {
        return source;
    }


    public void setSource(Collector source) {
        this.source = source;
    }

    /**
     * Get edge's target node.
     *
     * @return
     */
    public Collector getTarget() {
        return target;
    }

    /**
     * Set edge's target node.
     *
     * @param target New target node.
     */
    public void setTarget(Collector target) {
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

    public T getWeight() {
        return weight;
    }

    public void setWeight(T weight) {
        this.weight = weight;
    }

    public T getSignal() {
        if (isSignalReady()) {
            setSignalSend(true);
            return source.getSignal( < T > Double);
        } else return null;
    }

}
