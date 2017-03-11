package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

import java.util.ArrayList;

/**
 * Created by marko on 2/6/17.
 */
public class MuxInt extends Node {


    public MuxInt(int minInpCollectors, int maxInpCollectors, Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{minInpCollectors, maxInpCollectors}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0});
        addInpCollector(new CollectorTarget(Signal.createInteger(), this));
        for (int i = 0; i < maxInpCollectors - 1; i++) {
            addInpCollector(new CollectorTarget(Signal.createInteger(), this));
        }
        addOutCollector(new CollectorSource(Signal.createInteger(), this));

        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        ArrayList<CollectorTarget> col = getInpCollectorsConnected(Integer.class);
        Signal[] list = new Signal[col.size()];
        for (int i = 0; i < col.size(); i++) {
            list[i] = col.get(i).getSignal();
        }
        if (list[0].getValueInteger() >= 0 && list[0].getValueInteger() < col.size() - 1) {
            getCollectorsSourceInt().get(0).setSignal(col.get(list[0].getValueInteger() + 1).getSignal());
        } else {
            getCollectorsSourceInt().get(0).setSignal(getParams().get(0));
        }
    }
}
