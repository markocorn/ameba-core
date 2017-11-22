package ameba.core.blocks.edges;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetInt;

public class EdgeInt extends Edge {
    private int weight;
    private Integer[] limitsWeight;

    public EdgeInt(CollectorSourceInt source, CollectorTargetInt target, int weight, Integer[] limitsWeight) {
        setSource(source);
        setTarget(target);
        this.weight = weight;
        this.limitsWeight = limitsWeight;
    }

    public CollectorSourceInt getSourceInt() {
        return (CollectorSourceInt) getSource();
    }

    public CollectorTargetInt getTargetInt() {
        return (CollectorTargetInt) getTarget();
    }

    public int getSignal() {
        setSignalTransmitted(true);
        return ((CollectorSourceInt) getSource()).getSignal() * weight;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
        if (weight < limitsWeight[0]) this.weight = limitsWeight[0];
        if (weight > limitsWeight[1]) this.weight = limitsWeight[1];
    }

    @Override
    public Cell.Signal getType() {
        return Cell.Signal.INTEGER;
    }

}
