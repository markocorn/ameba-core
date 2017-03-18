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

        getParamsDec().add(par1);
        getParamsLimitsDec().add(par1Limits);
        getParamsDec().add(par2);
        getParamsLimitsDec().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (getParamsDec().get(0) <= getCollectorsTargetDec().get(0).getSignal() && getCollectorsTargetDec().get(0).getSignal() <= getParamsDec().get(1)) {
            getCollectorsSourceBin().get(0).setSignal(true);
        } else getCollectorsSourceBin().get(0).setSignal(false);
    }
}
