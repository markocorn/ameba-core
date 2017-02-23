package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchConstInt extends Node {

    public SwitchConstInt(Signal par, Signal[] par2Limits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0});
        addInpCollector(new CollectorInp(Signal.createBoolean(), this));
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addOutCollector(new CollectorOut(Signal.createInteger(), this));
        getParams().add(par);
        getParamsLimits().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (!getInpCollectors().get(0).getSignal().getValueBoolean()) {
            getOutCollectorsInt().get(0).setSignal(getInpCollectors().get(1).getSignal());
        } else {
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getInpCollectors().get(1).getSignal();
            getOutCollectorsInt().get(0).setSignal(getParams().get(0));
        }
    }
}
