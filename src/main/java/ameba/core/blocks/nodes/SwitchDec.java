package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchDec extends Node {

    public SwitchDec() throws Exception {
        super(new Integer[]{2, 2}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addInpCollector(new CollectorInp(Signal.createBoolean(), this));
        addInpCollector(new CollectorInp(Signal.createDouble(), this));
        addInpCollector(new CollectorInp(Signal.createDouble(), this));
        addOutCollector(new CollectorOut(Signal.createDouble(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (!getInpCollectors().get(0).getSignal().getValueBoolean()) {
            getOutCollectorsDec().get(0).setSignal(getInpCollectors().get(1).getSignal());
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getInpCollectors().get(2).getSignal();
        } else {
            getOutCollectorsDec().get(0).setSignal(getInpCollectors().get(2).getSignal());
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getInpCollectors().get(1).getSignal();
        }
    }
}
