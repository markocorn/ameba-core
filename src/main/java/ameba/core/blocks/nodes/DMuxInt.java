package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

/**
 * Created by marko on 2/6/17.
 */
public class DMuxInt extends Node {

    public DMuxInt(int minOutCollectors, int maxOutCollectors, Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{2, 2}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{minOutCollectors, maxOutCollectors}, new Integer[]{0, 0});
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        for (int i = 0; i < maxOutCollectors; i++) {
            addOutCollector(new CollectorOut(Signal.createInteger(), this));
        }
        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        Signal[] list = new Signal[getInpCollectors().size()];
        list[0] = getInpCollectorsInt().get(0).getSignal();
        for (int i = 1; i < getInpCollectors().size(); i++) {
            list[i] = getInpCollectors().get(i).getSignal();
        }
        for (int i = 0; i < getOutCollectorsDec().size(); i++) {
            if (i == list[0].getValueInteger()) {
                getOutCollectorsDec().get(i).setSignal(list[1]);
            } else {
                getOutCollectorsDec().get(i).setSignal(getParams().get(0));
            }
        }
    }
}
