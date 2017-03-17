package ameba.core.blocks.edges;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetInt;

public class EdgeInt extends Edge {
    /**
     * Source collector of the edge.
     */
    private CollectorSourceInt source;
    /**
     * Target collector of the edge.
     */
    private CollectorTargetInt target;

    private int weight;

    public EdgeInt(CollectorSourceInt source, CollectorTargetInt target, int weight) {
        super(source, target);
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    @Override
    public CollectorSourceInt getSource() {
        return source;
    }

    public void setSource(CollectorSourceInt source) {
        this.source = source;
    }

    @Override
    public CollectorTargetInt getTarget() {
        return target;
    }

    public void setTarget(CollectorTargetInt target) {
        this.target = target;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getSignal() {
        setSignalTransmitted(true);
        return source.getSignal();
    }

    @Override
    public Cell.Signal getType() {
        return Cell.Signal.INTEGER;
    }
}
