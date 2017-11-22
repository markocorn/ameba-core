package ameba.core.blocks.edges;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetDec;

public class EdgeDec extends Edge {

    private double weight;
    private Double[] limitsWeight;

    public EdgeDec(CollectorSourceDec source, CollectorTargetDec target, double weight, Double[] limitsWeight) {
        setSource(source);
        setTarget(target);
        this.weight = weight;
        this.limitsWeight = limitsWeight;
    }

    public CollectorSourceDec getSourceDec() {
        return (CollectorSourceDec) getSource();
    }

    public CollectorTargetDec getTargetDec() {
        return (CollectorTargetDec) getTarget();
    }

    public double getSignal() {
        setSignalTransmitted(true);
        return ((CollectorSourceDec) getSource()).getSignal() * weight;
    }

    @Override
    public Cell.Signal getType() {
        return Cell.Signal.DECIMAL;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
        if (weight < limitsWeight[0]) this.weight = limitsWeight[0];
        if (weight > limitsWeight[1]) this.weight = limitsWeight[1];
    }

}
