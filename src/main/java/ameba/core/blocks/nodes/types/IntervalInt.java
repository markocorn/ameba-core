package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class IntervalInt extends Node {

    public IntervalInt() throws Exception {
        super(new int[]{0, 0}, new int[]{3, 3}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, 0, 0, 0);

        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorSourceBin(new CollectorSourceBin(this));
    }

    //Calculate output value
    @Override
    public void clcNode() {
        int[] inputs = new int[]{getCollectorsTargetInt().get(0).getSignal(), getCollectorsTargetInt().get(1).getSignal(), getCollectorsTargetInt().get(2).getSignal()};
        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
            getCollectorsSourceBin().get(0).setSignal(true);
        } else getCollectorsSourceBin().get(0).setSignal(false);
    }
}
