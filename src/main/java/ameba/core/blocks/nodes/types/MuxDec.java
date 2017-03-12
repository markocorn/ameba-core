package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

import java.util.ArrayList;

/**
 * Created by marko on 2/6/17.
 */
public class MuxDec extends Node {


    public MuxDec(int minInpCollectors, int maxInpCollectors, Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addInpCollector(new CollectorTarget(Signal.createInteger(), this));
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorTarget(Signal.createDouble(), this));
        }
        addOutCollector(new CollectorSource(Signal.createDouble(), this));

        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        ArrayList<CollectorTarget> col = getCollectorsTargetInt();
        col.addAll(getCollectorsTargetConnected(Double.class));
        Signal[] list = new Signal[col.size()];
        for (int i = 0; i < col.size(); i++) {
            list[i] = col.get(i).getSignal();
        }
        if (list[0].getValueInteger() >= 0 && list[0].getValueInteger() < col.size() - 1) {
            getCollectorsSourceDec().get(0).setSignal(col.get(list[0].getValueInteger() + 1).getSignal());
        } else {
            getCollectorsSourceDec().get(0).setSignal(getParams().get(0));
        }
        if (getCollectorsSourceDec().get(0).getSignal() == null) {
            int t = 0;
        }
    }
}
