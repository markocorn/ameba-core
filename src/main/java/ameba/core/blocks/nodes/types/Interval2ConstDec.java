package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class Interval2ConstDec extends Node {

    public Interval2ConstDec(Signal par1, Signal[] par1Limits, Signal par2, Signal[] par2Limits) throws Exception {
        super(new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorTarget(Signal.createDouble(), this));
        addOutCollector(new CollectorSource(Signal.createBoolean(), this));

        getParams().add(par1);
        getParamsLimits().add(par1Limits);
        getParams().add(par2);
        getParamsLimits().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        double[] inputs = new double[]{getCollectorsTargetDec().get(0).getSignal().getValueDouble(), getParams().get(0).getValueDouble(), getParams().get(1).getValueDouble()};
        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
            getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(true));
        } else getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(false));
    }
}
