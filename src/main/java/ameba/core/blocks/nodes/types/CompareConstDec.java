package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

public class CompareConstDec extends Node {

    public CompareConstDec(Signal par1, Signal[] par1Limits, int par2) throws Exception {
        super(new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        addInpCollector(new CollectorTarget(Signal.createDouble(), this));
        addOutCollector(new CollectorSource(Signal.createBoolean(), this));
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
                getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(false));
                if (getCollectorsTargetDec().get(0).getSignal().getValueDouble() > getParams().get(0).getValueDouble()) {
                    getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
            //Less than par
            case 1: {
                getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(false));
                if (getCollectorsTargetDec().get(0).getSignal().getValueDouble() < getParams().get(0).getValueDouble()) {
                    getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
            //Equal to par
            case 2: {
                getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(false));
                if (getCollectorsTargetDec().get(0).getSignal().getValueDouble().equals(getParams().get(0).getValueDouble())) {
                    getCollectorsSourceBin().get(0).setSignal(Signal.createBoolean(true));
                }
            }
            break;
        }
    }
}

