package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchInt extends Node {

    public SwitchInt() throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{2, 2}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0});
        addInpCollector(new CollectorTarget(Signal.createBoolean(), this));
        addInpCollector(new CollectorTarget(Signal.createInteger(), this));
        addInpCollector(new CollectorTarget(Signal.createInteger(), this));
        addOutCollector(new CollectorSource(Signal.createInteger(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (!getCollectorsTarget().get(0).getSignal().getValueBoolean()) {
            getCollectorsSourceInt().get(0).setSignal(getCollectorsTarget().get(1).getSignal());
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getCollectorsTarget().get(2).getSignal();
        } else {
            getCollectorsSourceInt().get(0).setSignal(getCollectorsTarget().get(2).getSignal());
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getCollectorsTarget().get(1).getSignal();
        }
    }
}
