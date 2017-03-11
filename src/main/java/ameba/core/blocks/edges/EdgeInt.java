package ameba.core.blocks.edges;

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

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getSignal() throws Exception {
        setSignalTransmitted(true);
        return source.getSignal();
    }
}
