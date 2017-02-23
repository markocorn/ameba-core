package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class CompareInt extends Node {

    public CompareInt(int par) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{2, 2}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));
        getParams().add(Signal.createInteger(par));
        getParamsLimits().add(new Signal[]{Signal.createInteger(0), Signal.createInteger(2)});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {

        switch (getParams().get(0).getValueInteger()) {
            //Greater than par
            case 0: {
                getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(false));
                if (getInpCollectorsInt().get(0).getSignal().getValueInteger() > getInpCollectorsInt().get(1).getSignal().getValueInteger()) {
                    getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
            //Less than par
            case 1: {
                getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(false));
                if (getInpCollectorsInt().get(0).getSignal().getValueInteger() < getInpCollectorsInt().get(1).getSignal().getValueInteger()) {
                    getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
            //Equal to par
            case 2: {
                getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(false));
                if (getInpCollectorsInt().get(0).getSignal().getValueInteger().equals(getInpCollectorsInt().get(1).getSignal().getValueInteger())) {
                    getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
        }
    }
}

