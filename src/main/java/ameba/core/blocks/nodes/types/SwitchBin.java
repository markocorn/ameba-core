package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchBin extends Node {

    public SwitchBin() throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{3, 3}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorTarget(Signal.createBoolean(), this));
        addInpCollector(new CollectorTarget(Signal.createBoolean(), this));
        addInpCollector(new CollectorTarget(Signal.createBoolean(), this));
        addOutCollector(new CollectorSource(Signal.createBoolean(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (!getCollectorsTarget().get(0).getSignal().getValueBoolean()) {
            getCollectorsSourceBin().get(0).setSignal(getCollectorsTarget().get(1).getSignal());
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getCollectorsTarget().get(2).getSignal();
        } else {
            getCollectorsSourceBin().get(0).setSignal(getCollectorsTarget().get(2).getSignal());
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getCollectorsTarget().get(1).getSignal();
        }
    }
}
