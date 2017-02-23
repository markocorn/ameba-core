package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

/**
 * Created by marko on 2/1/17.
 */
public class IntervalConstInt extends Node {

    public IntervalConstInt(Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{2, 2}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));

        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        int[] inputs = new int[]{getInpCollectorsInt().get(0).getSignal().getValueInteger(), getInpCollectorsInt().get(1).getSignal().getValueInteger(), getParams().get(0).getValueInteger()};
        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
            getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(true));
        } else getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(false));
    }
}
