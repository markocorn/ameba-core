package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class Interval2ConstInt extends Node {

    public Interval2ConstInt(Signal par1, Signal[] par1Limits, Signal par2, Signal[] par2Limits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorTarget(Signal.createInteger(), this));
        addOutCollector(new CollectorSource(Signal.createBoolean(), this));

        getParams().add(par1);
        getParamsLimits().add(par1Limits);
        getParams().add(par2);
        getParamsLimits().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        int[] inputs = new int[]{getCollectorsTargetInt().get(0).getSignal().getValueInteger(), getParams().get(0).getValueInteger(), getParams().get(1).getValueInteger()};
        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
            getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(true));
        } else getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(false));
    }
}
