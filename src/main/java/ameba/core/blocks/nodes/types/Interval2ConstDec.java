package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class Interval2ConstDec extends Node {

    public Interval2ConstDec(Double par1, Double[] par1Limits, Double par2, Double[] par2Limits) throws Exception {
        super(new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});

        addCollectorTargetDec(new CollectorTargetDec(this));
        addCollectorSourceBin(new CollectorSourceBin(this));

        setParamsDec(new Double[]{par1});
        setParamsLimitsDec(new Double[][]{par1Limits});
        setParamsDec(new Double[]{par2});
        setParamsLimitsDec(new Double[][]{par2Limits});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (getParamsDec()[0] <= getCollectorsTargetDec().get(0).getSignal() && getCollectorsTargetDec().get(0).getSignal() <= getParamsDec()[1]) {
            getCollectorsSourceBin().get(0).setSignal(true);
        } else getCollectorsSourceBin().get(0).setSignal(false);
    }
}
