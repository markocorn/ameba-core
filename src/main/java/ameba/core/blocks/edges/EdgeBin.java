package ameba.core.blocks.edges;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;

public class EdgeBin extends Edge {

    private boolean weight;

    public EdgeBin(CollectorSourceBin source, CollectorTargetBin target, boolean weight) {
        super(source, target);
        this.weight = weight;
    }

    public CollectorSourceBin getSourceBin() {
        return (CollectorSourceBin) getSource();
    }

    public CollectorTargetBin getTargetBin() {
        return (CollectorTargetBin) getTarget();
    }

    public boolean getSignal() {
        setSignalTransmitted(true);
        return ((CollectorSourceBin) getSource()).getSignal();
    }

    public boolean getWeight() {
        return weight;
    }

    public void setWeight(boolean weight) {
        this.weight = weight;
    }

    @Override
    public Cell.Signal getType() {
        return Cell.Signal.BOOLEAN;
    }
}
