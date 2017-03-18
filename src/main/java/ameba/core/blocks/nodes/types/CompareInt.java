package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.Node;

public class CompareInt extends Node {

    public CompareInt(int par) throws Exception {
        super(new int[]{0, 0}, new int[]{2, 2}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1});
        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorTargetInt(new CollectorTargetInt(this));
        addCollectorSourceBin(new CollectorSourceBin(this));
        getParamsInt().add(par);
        getParamsLimitsInt().add(new Integer[]{0, 2});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceBin().get(0).setSignal(false);
        switch (getParamsInt().get(0)) {
            //Greater than par
            case 0: {
                if (getCollectorsTargetInt().get(0).getSignal() > getCollectorsTargetInt().get(1).getSignal()) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
            //Less than par
            case 1: {
                if (getCollectorsTargetInt().get(0).getSignal() < getCollectorsTargetInt().get(1).getSignal()) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
            //Equal to par
            case 2: {
                if (getCollectorsTargetInt().get(0).getSignal() == getCollectorsTargetInt().get(1).getSignal()) {
                    getCollectorsSourceBin().get(0).setSignal(true);
                }
            }
            break;
        }
    }
}

