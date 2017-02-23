package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchInt extends Node {

    public SwitchInt() throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{2, 2}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0});
        addInpCollector(new CollectorInp(Signal.createBoolean(), this));
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addOutCollector(new CollectorOut(Signal.createInteger(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (!getInpCollectors().get(0).getSignal().getValueBoolean()) {
            getOutCollectorsInt().get(0).setSignal(getInpCollectors().get(1).getSignal());
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getInpCollectors().get(2).getSignal();
        } else {
            getOutCollectorsInt().get(0).setSignal(getInpCollectors().get(2).getSignal());
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getInpCollectors().get(1).getSignal();
        }
    }
}
