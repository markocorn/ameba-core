package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class Switch2ConstDec extends Node {

    public Switch2ConstDec(Signal par1, Signal[] par1Limits, Signal par2, Signal[] par2Limits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addInpCollector(new CollectorTarget(Signal.createBoolean(), this));
        addOutCollector(new CollectorSource(Signal.createDouble(), this));
        getParams().add(par1);
        getParamsLimits().add(par1Limits);
        getParams().add(par2);
        getParamsLimits().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (getCollectorsTargetBin().get(0).getSignal().getValueBoolean()) {
            getCollectorsSourceDec().get(0).setSignal(getParams().get(1));
        } else {
            getCollectorsSourceDec().get(0).setSignal(getParams().get(0));
        }
    }
}
