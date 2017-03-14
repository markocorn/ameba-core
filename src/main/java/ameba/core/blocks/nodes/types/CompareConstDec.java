package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.Node;

public class CompareConstDec extends Node {

    public CompareConstDec(double par1, double[] par1Limits, int par2) throws Exception {
        super(new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});
        addCollectorTargetDec(new CollectorTargetDec(this));
        addCollectorSourceBin(new CollectorSourceBin(this));
        //AddDec compare against parameter
        setParamsDec(new double[]{par1});
        setParamsLimitsDec(new double[][]{par1Limits});
        //AddDec operation select parameter
        setParamsInt(new int[]{par2});
        setParamsLimitsInt(new int[][]{{0, 2}});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceBin().get(0).setSignal(false);
        switch (getParamsInt()[0]) {
            //Greater than par
            case 0: {
                if (getCollectorsTargetDec().get(0).getSignal() > getParamsDec()[0]) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
            //Less than par
            case 1: {
                if (getCollectorsTargetDec().get(0).getSignal() < getParamsDec()[0]) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
            //Equal to par
            case 2: {
                if (getCollectorsTargetDec().get(0).getSignal() == getParamsDec()[0]) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
        }
    }
}

