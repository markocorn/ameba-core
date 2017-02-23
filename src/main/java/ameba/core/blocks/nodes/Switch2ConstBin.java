package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

/**
 * Created by marko on 2/1/17.
 */
public class Switch2ConstBin extends Node {

    public Switch2ConstBin(Signal par1, Signal[] par1Limits, Signal par2, Signal[] par2Limits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorInp(Signal.createBoolean(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));
        getParams().add(par1);
        getParamsLimits().add(par1Limits);
        getParams().add(par2);
        getParamsLimits().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (getInpCollectorsBin().get(0).getSignal().getValueBoolean()) {
            getOutCollectorsBin().get(0).setSignal(getParams().get(1));
        } else {
            getOutCollectorsBin().get(0).setSignal(getParams().get(0));
        }
    }
}
