package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class SwitchConstDec extends Node {

    public SwitchConstDec(Signal par, Signal[] par2Limits) throws Exception {
        super(new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addInpCollector(new CollectorTarget(Signal.createBoolean(), this));
        addInpCollector(new CollectorTarget(Signal.createDouble(), this));
        addOutCollector(new CollectorSource(Signal.createDouble(), this));
        getParams().add(par);
        getParamsLimits().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (!getCollectorsTarget().get(0).getSignal().getValueBoolean()) {
            getCollectorsSourceDec().get(0).setSignal(getCollectorsTarget().get(1).getSignal());
        } else {
            //Be carefully allays initiate get method for inputs to set send flag of input nodes.
            getCollectorsTarget().get(1).getSignal();
            getCollectorsSourceDec().get(0).setSignal(getParams().get(0));
        }
    }
}
