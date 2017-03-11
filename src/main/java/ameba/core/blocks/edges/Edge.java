package ameba.core.blocks.edges;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;

public class Edge implements Cloneable {
    /**
     * Source collector of the edge.
     */
    private CollectorSource source;
    /**
     * Target collector of the edge.
     */
    private CollectorTarget target;

    private boolean signalTransmitted;


    public Edge(CollectorSource source, CollectorTarget target){
        this.source = source;
        this.target = target;
        this.signalTransmitted=false;
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

    public void rstEdge(){
        signalTransmitted=false;
    }
}
