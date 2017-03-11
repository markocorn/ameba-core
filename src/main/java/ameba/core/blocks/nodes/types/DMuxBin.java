package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/6/17.
 */
public class DMuxBin extends Node {

    public DMuxBin(int minOutCollectors, int maxOutCollectors, Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{minOutCollectors, maxOutCollectors});
        addInpCollector(new CollectorTarget(Signal.createInteger(), this));
        addInpCollector(new CollectorTarget(Signal.createBoolean(), this));
        for (int i = 0; i < maxOutCollectors; i++) {
            addOutCollector(new CollectorSource(Signal.createBoolean(), this));
        }
        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        Signal[] list = new Signal[getCollectorsTarget().size()];
        list[0] = getCollectorsTargetInt().get(0).getSignal();
        for (int i = 1; i < getCollectorsTarget().size(); i++) {
            list[i] = getCollectorsTarget().get(i).getSignal();
        }
        for (int i = 0; i < getCollectorsSourceDec().size(); i++) {
            if (i == list[0].getValueInteger()) {
                getCollectorsSourceDec().get(i).setSignal(list[1]);
            } else {
                getCollectorsSourceDec().get(i).setSignal(getParams().get(0));
            }
        }
    }
}
