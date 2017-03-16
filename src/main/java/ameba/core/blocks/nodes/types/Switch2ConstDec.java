package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class Switch2ConstDec extends Node {

    public Switch2ConstDec(double par1, double[] par1Limits, double par2, double[] par2Limits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0});

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorSourceDec(new CollectorSourceDec(this));

        setParamsDec(new double[]{par1});
        setParamsLimitsDec(new double[][]{par1Limits});
        setParamsDec(new double[]{par2});
        setParamsLimitsDec(new double[][]{par2Limits});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        if (getCollectorsTargetBin().get(0).getSignal()) {
            getCollectorsSourceDec().get(0).setSignal(getParamsDec()[1]);
        } else {
            getCollectorsSourceDec().get(0).setSignal(getParamsDec()[0]);
        }
    }
}
