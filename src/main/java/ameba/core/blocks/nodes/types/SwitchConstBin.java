package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchConstBin extends Node {

    public SwitchConstBin(Signal par, Signal[] par2Limits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{2, 2}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorTarget(Signal.createBoolean(), this));
        addInpCollector(new CollectorTarget(Signal.createBoolean(), this));
        addOutCollector(new CollectorSource(Signal.createBoolean(), this));
        getParams().add(par);
        getParamsLimits().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (!getCollectorsTargetBin().get(0).getSignal().getValueBoolean()) {
            getCollectorsSourceBin().get(0).setSignal(getCollectorsTargetBin().get(1).getSignal());
        } else {
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getCollectorsTargetBin().get(1).getSignal();
            getCollectorsSourceBin().get(0).setSignal(getParams().get(0));
        }
    }
}
