package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

import java.util.ArrayList;

/**
 * Created by marko on 2/6/17.
 */
public class MuxBin extends Node {


    public MuxBin(int minInpCollectors, int maxInpCollectors, Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        for (int i = 0; i < maxInpCollectors - 1; i++) {
            addInpCollector(new CollectorInp(Signal.createBoolean(), this));
        }
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));

        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        ArrayList<CollectorInp> col = getInpCollectorsInt();
        col.addAll(getInpCollectorsConnected(Boolean.class));
        Signal[] list = new Signal[col.size()];
        for (int i = 0; i < col.size(); i++) {
            list[i] = col.get(i).getSignal();
        }
        if (list[0].getValueInteger() >= 0 && list[0].getValueInteger() < col.size() - 1) {
            getOutCollectorsBin().get(0).setSignal(col.get(list[0].getValueInteger() + 1).getSignal());
        } else {
            getOutCollectorsBin().get(0).setSignal(getParams().get(0));
        }
    }
}
