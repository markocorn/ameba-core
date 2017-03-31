package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.Node;

public class CompareDec extends Node {

    public CompareDec(Integer par) throws Exception {
        super(new int[]{2, 2}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});
        addCollectorTargetDec(new CollectorTargetDec(this));
        addCollectorTargetDec(new CollectorTargetDec(this));
        addCollectorSourceBin(new CollectorSourceBin(this));
        getParamsInt().add(par);
        getParamsLimitsInt().add(new Integer[]{0, 2});
    }

    //Calculate output value
    @Override
    public void clcNode() {
        getCollectorsSourceBin().get(0).setSignal(false);
        switch (getParamsInt().get(0)) {
            //Greater than par
            case 0: {
                if (getCollectorsTargetDec().get(0).getSignal() > getCollectorsTargetDec().get(1).getSignal()) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
            //Less than par
            case 1: {
                if (getCollectorsTargetDec().get(0).getSignal() < getCollectorsTargetDec().get(1).getSignal()) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
            //Equal to par
            case 2: {
                if (getCollectorsTargetDec().get(0).getSignal() == getCollectorsTargetDec().get(1).getSignal()) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
        }
    }
}

