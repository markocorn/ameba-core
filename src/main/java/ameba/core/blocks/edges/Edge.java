package ameba.core.blocks.edges;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;

public class Edge implements Serializable {
    /**
     * Source collector of the edge.
     */
    private CollectorSource source;
    /**
     * Target collector of the edge.
     */
    private CollectorTarget target;

    private boolean lockSource = false;

    private boolean lockTarget = false;

    private boolean lockWeight = false;

    private boolean signalTransmitted;

    private double weight;
    private Double[] limitsWeight;

    public Edge(CollectorSource source, CollectorTarget target, double weight, Double[] limitsWeight) {
        setSource(source);
        setTarget(target);
        this.weight = weight;
        this.limitsWeight = limitsWeight;
    }


    public double getSignal() {
        setSignalTransmitted(true);
        return source.getSignal() * weight;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
        if (weight < limitsWeight[0]) this.weight = limitsWeight[0];
        if (weight > limitsWeight[1]) this.weight = limitsWeight[1];
    }

    public CollectorSource getSource() {
        return source;
    }

    public void setSource(CollectorSource source) {
        this.source = source;
    }

    /**
     * Get edge's target node.
     *
     * @return
     */
    public CollectorTarget getTarget() {
        return target;
    }

    /**
     * Set edge's target node.
     *
     * @param target New target node.
     */
    public void setTarget(CollectorTarget target) {
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

    public boolean isSignalTransmitted() {
        return signalTransmitted;
    }

    public void setSignalTransmitted(boolean signalTransmitted) {
        this.signalTransmitted = signalTransmitted;
    }

    public void rstEdge() {
        signalTransmitted = false;
    }

    public Cell.ParType getType() {
        return null;
    }

    public boolean isLockSource() {
        return lockSource;
    }

    public void setLockSource(boolean lockSource) {
        this.lockSource = lockSource;
    }

    public boolean isLockTarget() {
        return lockTarget;
    }

    public void setLockTarget(boolean lockTarget) {
        this.lockTarget = lockTarget;
    }

    public boolean isLockWeight() {
        return lockWeight;
    }

    public void setLockWeight(boolean lockWeight) {
        this.lockWeight = lockWeight;
    }

    public Edge clone() {
        return (Edge) SerializationUtils.clone(this);
    }
}
