package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceInt;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/6/17.
 */
public class MuxInt extends Node {


    public MuxInt(int minInpCollectors, int maxInpCollectors, Integer par, Integer[] parLimits) throws Exception {
        super(new int[]{0, 0}, new int[]{minInpCollectors, maxInpCollectors}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0});
        addCollectorTargetInt(new CollectorTargetInt(this));
        for (int i = 0; i < maxInpCollectors - 1; i++) {
            addCollectorTargetInt(new CollectorTargetInt(this));
        }
        addCollectorSourceInt(new CollectorSourceInt(this));

        getParamsInt().add(par);
        getParamsLimitsInt().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        int ind = getCollectorsTargetInt().get(0).getSignal();
        int[] inp = new int[getCollectorsTargetConnectedIntSim().size()];
        for (int i = 0; i < getCollectorsTargetConnectedIntSim().size(); i++) {
            inp[i] = getCollectorsTargetConnectedIntSim().get(i).getSignal();
        }
        if (ind < inp.length && ind >= 0) {
            getCollectorsSourceInt().get(0).setSignal(inp[ind]);
        } else {
            getCollectorsSourceInt().get(0).setSignal(getParamsInt().get(0));
        }
    }
}
