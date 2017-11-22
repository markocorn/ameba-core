package ameba.core.blocks.edges;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetBin;

public class EdgeBin extends Edge {

    private boolean weight;
    private Boolean[] limitsWeight;


    public EdgeBin(CollectorSourceBin source, CollectorTargetBin target, boolean weight, Boolean[] limitsWeight) {
        setSource(source);
        setTarget(target);
        this.weight = weight;
        this.limitsWeight = limitsWeight;
    }

    public CollectorSourceBin getSourceBin() {
        return (CollectorSourceBin) getSource();
    }

    public CollectorTargetBin getTargetBin() {
        return (CollectorTargetBin) getTarget();
    }

    public boolean getSignal() {
        setSignalTransmitted(true);
        return ((CollectorSourceBin) getSource()).getSignal() ^ weight;
    }

    public boolean getWeight() {
        return weight;
    }

    public void setWeight(boolean weight) {
        this.weight = weight;
        if (limitsWeight[0]) {
            this.weight = true;
        }
        if (!limitsWeight[1]) {
            this.weight = false;
        }
    }

    @Override
    public Cell.Signal getType() {
        return Cell.Signal.BOOLEAN;
    }
}
