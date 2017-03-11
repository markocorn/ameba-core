package ameba.core.blocks.edges;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;

public class EdgeBin extends Edge {
    /**
     * Source collector of the edge.
     */
    private CollectorSourceBin source;
    /**
     * Target collector of the edge.
     */
    private CollectorTargetBin target;

    private boolean weight;

    public EdgeBin(CollectorSourceBin source, CollectorTargetBin target, boolean weight) {
        super(source, target);
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    public boolean getWeight() {
        return weight;
    }

    public void setWeight(boolean weight) {
        this.weight = weight;
    }

    public boolean getSignal() {
        setSignalTransmitted(true);
        return source.getSignal();
    }
}
