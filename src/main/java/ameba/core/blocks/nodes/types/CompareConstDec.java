package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.Node;

public class CompareConstDec extends Node {

    public CompareConstDec(Double par1, Double[] par1Limits, Integer par2) throws Exception {
        super(new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, 1, 1, 0);
        addCollectorTargetDec(new CollectorTargetDec(this));
        addCollectorSourceBin(new CollectorSourceBin(this));
        //AddDec compare against parameter
        getParamsDec().add(par1);
        getParamsLimitsDec().add(par1Limits);
        //AddDec operation ISelect parameter
        getParamsInt().add(par2);
        getParamsLimitsInt().add(new Integer[]{0, 2});
    }

    //Calculate output value
    @Override
    public void clcNode() {
        getCollectorsSourceBin().get(0).setSignal(false);
        switch (getParamsInt().get(0)) {
            //Greater than par
            case 0: {
                if (getCollectorsTargetDec().get(0).getSignal() > getParamsDec().get(0)) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
            //Less than par
            case 1: {
                if (getCollectorsTargetDec().get(0).getSignal() < getParamsDec().get(0)) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
            //Equal to par
            case 2: {
                if (getCollectorsTargetDec().get(0).getSignal() == getParamsDec().get(0)) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
        }
    }
}

