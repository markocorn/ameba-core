package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/1/17.
 */
public class Switch2ConstDec extends Node {

    public Switch2ConstDec(Double par1, Double[] par1Limits, Double par2, Double[] par2Limits) throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0});

        addCollectorTargetBin(new CollectorTargetBin(this));
        addCollectorSourceDec(new CollectorSourceDec(this));

        setParamsDec(new Double[]{par1});
        setParamsLimitsDec(new Double[][]{par1Limits});
        setParamsDec(new Double[]{par2});
        setParamsLimitsDec(new Double[][]{par2Limits});
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
