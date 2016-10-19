package blocks;

import java.io.Serializable;

public class Edge implements Serializable {
    /**
     * Source node of the edge.
     */
    private Node source;
    /**
     * Target node of the edge.
     */
    private Node target;

    /**
     * Weight parameter of the edge.
     */
    private double weight;

    /**
     * Flag that indicates signal was signalSend trough the edge.
     */
    private boolean signalSend;

    /**
     * @param source Source node of the edge.
     * @param target Target node of the edge.
     * @param weight Weight parameter of the edge.
     */
    public Edge(Node source, Node target, double weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
        this.signalSend = false;
    }

    /**
     * Get edge's source node.
     *
     * @return
     */
    public Node getSource() {
        return source;
    }

    /**
     * Set edge's source node.
     *
     * @param source New source node.
     */
    public void setSource(Node source) {
        this.source = source;
    }

    /**
     * Get edge's target node.
     *
     * @return
     */
    public Node getTarget() {
        return target;
    }

    /**
     * Set edge's target node.
     *
     * @param target New target node.
     */
    public void setTarget(Node target) {
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
     * Get output value of the source node multiplied by the edge's weight.
     *
     * @return
     */
    public double getSignal() {
        if (isSignalReady()) {
            signalSend = true;
            return source.getSignal() * weight;
        } else return 0;
    }

    /**
     * Reset edge's send flag.
     */
    public void rstEdge() {
        signalSend = false;
    }

    /**
     * Get weight parameter of the edge.
     *
     * @return
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Set weight parameter of the edge.
     *
     * @param weight
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * Check if the signal has been send trough edge.
     *
     * @return
     */
    public boolean isSignalSend() {
        return signalSend;
    }
}
