package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorInp;
import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;

/**
 * Created by marko on 2/1/17.
 */
public class Switch2Const extends Node {

    public Switch2Const(Signal type, int minOutputEdges, int maxOutputEdges, Signal const1, Signal[] const1Limits, Signal const2, Signal[] const2Limits) throws Exception {
        super(1, 1, 1, 1);
        addInpCollector(new CollectorInp(Signal.createBoolean(), this));
        addOutCollector(new CollectorOut(type, minOutputEdges, maxOutputEdges, this));
        getParams().add(const1);
        getParamsLimits().add(const1Limits);
        getParams().add(const2);
        getParamsLimits().add(const2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    if (getInpCollectors().get(0).getSignal().getValueBoolean()) {
                        getOutCollectors().get(0).setSignal(getParams().get(1));
                    } else {
                        getOutCollectors().get(0).setSignal(getParams().get(0));
                    }
                    setSignalClcDone(true);
                    setState(1);
                }
            case 1:
                if (isSignalClcDone()) {
                    setSignalReady(true);
                    setState(2);
                }
            case 2:
                if (isSignalReady()) {
                    setState(3);
                }
            case 3:
                if (isSignalSend()) {
                    setState(4);
                }
            case 4:
        }
    }
}
