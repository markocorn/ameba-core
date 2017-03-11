package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class IntervalConstInt extends Node {

    public IntervalConstInt(Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{2, 2}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorTarget(Signal.createInteger(), this));
        addInpCollector(new CollectorTarget(Signal.createInteger(), this));
        addOutCollector(new CollectorSource(Signal.createBoolean(), this));

        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        int[] inputs = new int[]{getCollectorsTargetInt().get(0).getSignal().getValueInteger(), getCollectorsTargetInt().get(1).getSignal().getValueInteger(), getParams().get(0).getValueInteger()};
        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
            getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(true));
        } else getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(false));
    }
}
