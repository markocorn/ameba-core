package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

/**
 * Created by marko on 2/6/17.
 */
public class MuxInt extends Node {


    public MuxInt(int minInpCollectors, int maxInpCollectors, Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0});
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        for (int i = 0; i < maxInpCollectors - 1; i++) {
            addInpCollector(new CollectorInp(Signal.createInteger(), this));
        }
        addOutCollector(new CollectorOut(Signal.createInteger(), this));

        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        Signal[] list = new Signal[getInpCollectors().size()];
        for (int i = 0; i < getInpCollectors().size(); i++) {
            list[i] = getInpCollectors().get(i).getSignal();
        }
        if (list[0].getValueInteger() >= 0 && list[0].getValueInteger() < getInpCollectors().size() - 1) {
            getOutCollectorsInt().get(0).setSignal(getInpCollectors().get(list[0].getValueInteger() + 1).getSignal());
        } else {
            getOutCollectorsInt().get(0).setSignal(getParams().get(0));
        }
    }
}
