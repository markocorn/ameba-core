package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/6/17.
 */
public class MuxDec extends Node {

    public MuxDec(int minInpCollectors, int maxInpCollectors, Double par, Double[] parLimits) throws Exception {
        super(new int[]{minInpCollectors, maxInpCollectors}, new int[]{1, 1}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0});

        addCollectorTargetInt(new CollectorTargetInt(this));
        for (int i = 0; i < maxInpCollectors; i++) {
            addCollectorTargetDec(new CollectorTargetDec(this));
        }
        addCollectorSourceDec(new CollectorSourceDec(this));

        setParamsDec(new Double[]{par});
        setParamsLimitsDec(new Double[][]{parLimits});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        int ind = getCollectorsTargetInt().get(0).getSignal();
        double[] inp = new double[getCollectorsTargetConnectedDec().size()];
        for (int i = 0; i < getCollectorsTargetConnectedDec().size(); i++) {
            inp[i] = getCollectorsTargetConnectedDec().get(i).getSignal();
        }
        if (ind < inp.length && ind >= 0) {
            getCollectorsSourceDec().get(0).setSignal(inp[ind]);
        } else {
            getCollectorsSourceDec().get(0).setSignal(getParamsDec()[0]);
        }
    }
}
