package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

import java.util.ArrayList;

/**
 * Created by marko on 2/6/17.
 */
public class MuxDec extends Node {


    public MuxDec(int minInpCollectors, int maxInpCollectors, Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorInp(Signal.createDouble(), this));
        }
        addOutCollector(new CollectorOut(Signal.createDouble(), this));

        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        ArrayList<CollectorInp> col = getInpCollectorsInt();
        col.addAll(getInpCollectorsConnected(Double.class));
        Signal[] list = new Signal[col.size()];
        for (int i = 0; i < col.size(); i++) {
            list[i] = col.get(i).getSignal();
        }
        if (list[0].getValueInteger() >= 0 && list[0].getValueInteger() < col.size() - 1) {
            getOutCollectorsDec().get(0).setSignal(col.get(list[0].getValueInteger() + 1).getSignal());
        } else {
            getOutCollectorsDec().get(0).setSignal(getParams().get(0));
        }
        if (getOutCollectorsDec().get(0).getSignal() == null) {
            int t = 0;
        }
    }
}
