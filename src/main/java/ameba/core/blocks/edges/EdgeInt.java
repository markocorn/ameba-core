package ameba.core.blocks.edges;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetInt;

public class EdgeInt extends Edge {
    private int weight;

    public EdgeInt(CollectorSourceInt source, CollectorTargetInt target, int weight) {
        super(source, target);
        this.weight = weight;
    }

    public CollectorSourceInt getSourceInt() {
        return (CollectorSourceInt) getSource();
    }

    public CollectorTargetInt getTargetInt() {
        return (CollectorTargetInt) getTarget();
    }

    public int getSignal() {
        setSignalTransmitted(true);
        return ((CollectorSourceInt) getSource()).getSignal();
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public Cell.Signal getType() {
        return Cell.Signal.INTEGER;
    }

}
