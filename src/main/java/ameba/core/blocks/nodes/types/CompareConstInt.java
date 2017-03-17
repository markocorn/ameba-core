package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

public class CompareConstInt extends Node {

    public CompareConstInt(Integer par1, Integer[] par1Limits, Integer par2) throws Exception {
        super(new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});
        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorSourceBin(new CollectorSourceBin(this));
        //compare against parameter
        setParamsInt(new Integer[]{par1});
        setParamsLimitsInt(new Integer[][]{par1Limits});
        //operation select parameter
        setParamsInt(new Integer[]{par2});
        setParamsLimitsInt(new Integer[][]{{0, 2}});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceBin().get(0).setSignal(false);
        switch (getParamsInt()[0]) {
            //Greater than par
            case 0: {
                if (getCollectorsTargetInt().get(0).getSignal() > getParamsInt()[0]) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
            //Less than par
            case 1: {
                if (getCollectorsTargetInt().get(0).getSignal() < getParamsInt()[0]) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
            //Equal to par
            case 2: {
                if (getCollectorsTargetInt().get(0).getSignal() == getParamsInt()[0]) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
        }
    }
}

