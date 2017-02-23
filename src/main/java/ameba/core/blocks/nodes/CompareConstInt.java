package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class CompareConstInt extends Node {

    public CompareConstInt(Signal par1, Signal[] par1Limits, int par2) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));
        //AddDec compare against parameter
        getParams().add(par1);
        getParamsLimits().add(par1Limits);
        //AddDec operation select parameter
        getParams().add(Signal.createInteger(par2));
        getParamsLimits().add(new Signal[]{Signal.createInteger(0), Signal.createInteger(2)});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getParams().get(1).getValueInteger()) {
            //Greater than par
            case 0: {
                getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(false));
                if (getInpCollectorsInt().get(0).getSignal().getValueInteger() > getParams().get(0).getValueInteger()) {
                    getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
            //Less than par
            case 1: {
                getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(false));
                if (getInpCollectorsInt().get(0).getSignal().getValueInteger() < getParams().get(0).getValueInteger()) {
                    getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
            //Equal to par
            case 2: {
                getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(false));
                if (getInpCollectorsInt().get(0).getSignal().getValueInteger().equals(getParams().get(0).getValueInteger())) {
                    getOutCollectorsBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
        }
    }
}

