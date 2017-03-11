package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

public class CompareDec extends Node {

    public CompareDec(int par) throws Exception {
        super(new Integer[]{2, 2}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorTarget(Signal.createDouble(), this));
        addInpCollector(new CollectorTarget(Signal.createDouble(), this));
        addOutCollector(new CollectorSource(Signal.createBoolean(), this));
        getParams().add(Signal.createInteger(par));
        getParamsLimits().add(new Signal[]{Signal.createInteger(0), Signal.createInteger(2)});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getParams().get(0).getValueInteger()) {
            //Greater than par
            case 0: {
                getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(false));
                if (getCollectorsTargetDec().get(0).getSignal().getValueDouble() > getCollectorsTargetDec().get(1).getSignal().getValueDouble()) {
                    getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
            //Less than par
            case 1: {
                getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(false));
                if (getCollectorsTargetDec().get(0).getSignal().getValueDouble() < getCollectorsTargetDec().get(1).getSignal().getValueDouble()) {
                    getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
            //Equal to par
            case 2: {
                getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(false));
                if (getCollectorsTargetDec().get(0).getSignal().getValueDouble().equals(getCollectorsTargetDec().get(1).getSignal().getValueDouble())) {
                    getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
        }
    }
}

