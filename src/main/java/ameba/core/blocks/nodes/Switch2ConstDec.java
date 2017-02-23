package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

/**
 * Created by marko on 2/1/17.
 */
public class Switch2ConstDec extends Node {

    public Switch2ConstDec(Signal par1, Signal[] par1Limits, Signal par2, Signal[] par2Limits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addInpCollector(new CollectorInp(Signal.createBoolean(), this));
        addOutCollector(new CollectorOut(Signal.createDouble(), this));
        getParams().add(par1);
        getParamsLimits().add(par1Limits);
        getParams().add(par2);
        getParamsLimits().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (getInpCollectorsBin().get(0).getSignal().getValueBoolean()) {
            getOutCollectorsDec().get(0).setSignal(getParams().get(1));
        } else {
            getOutCollectorsDec().get(0).setSignal(getParams().get(0));
        }
    }
}
