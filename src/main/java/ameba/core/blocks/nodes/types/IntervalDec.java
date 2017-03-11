package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class IntervalDec extends Node {

    public IntervalDec() throws Exception {
        super(new Integer[]{3, 3}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorTarget(Signal.createDouble(), this));
        addInpCollector(new CollectorTarget(Signal.createDouble(), this));
        addInpCollector(new CollectorTarget(Signal.createDouble(), this));
        addOutCollector(new CollectorSource(Signal.createBoolean(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        double[] inputs = new double[]{getCollectorsTarget().get(0).getSignal().getValueDouble(), getCollectorsTarget().get(1).getSignal().getValueDouble(), getCollectorsTarget().get(2).getSignal().getValueDouble()};
        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
            getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(true));
        } else getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(false));
    }
}
