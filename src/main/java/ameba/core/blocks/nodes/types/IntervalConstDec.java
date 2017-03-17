package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class IntervalConstDec extends Node {

    public IntervalConstDec(Double par, Double[] parLimits) throws Exception {
        super(new int[]{2, 2}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});

        addCollectorTargetDec(new CollectorTargetDec(this));
        addCollectorTargetDec(new CollectorTargetDec(this));
        addCollectorSourceBin(new CollectorSourceBin(this));

        setParamsDec(new Double[]{par});
        setParamsLimitsDec(new Double[][]{parLimits});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        double[] inputs = new double[]{getCollectorsTargetDec().get(0).getSignal(), getCollectorsTargetDec().get(1).getSignal(), getParamsDec()[0]};
        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
            getCollectorsSourceBin().get(0).setSignal(true);
        } else getCollectorsSourceBin().get(0).setSignal(false);
    }
}
