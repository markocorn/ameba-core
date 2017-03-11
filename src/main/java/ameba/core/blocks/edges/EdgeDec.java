package ameba.core.blocks.edges;

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetDec;

public class EdgeDec extends Edge {
    /**
     * Source collector of the edge.
     */
    private CollectorSourceDec source;
    /**
     * Target collector of the edge.
     */
    private CollectorTargetDec target;

    private double weight;

    public EdgeDec(CollectorSourceDec source, CollectorTargetDec target, double weight) {
        super(source, target);
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getSignal() {
        setSignalTransmitted(true);
        return source.getSignal();
    }
}
