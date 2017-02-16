package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Signal;

public class Multiply extends Node {

    public Multiply(Signal type, int minInpCollectors, int maxInpCollectors) throws Exception {
        super(minInpCollectors, maxInpCollectors, 1, 1);
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorInp(type.clone(), this));
        }
        addOutCollector(new CollectorOut(type.clone(), this));
        if (type.gettClass().isAssignableFrom(Boolean.class))
            throw new Exception("Multiply node is not allowed as Double type node");

    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    if (getOutCollectors().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                        getOutCollectors().get(0).getSignal().setValueDouble(1.0);
                        for (CollectorInp collectorInp : getInpCollectorsConn()) {
                            //Add all sources signals together.
                            getOutCollectors().get(0).getSignal().setValueDouble(
                                    getOutCollectors().get(0).getSignal().getValueDouble() *
                                            collectorInp.getSignal().getValueDouble());
                        }
                    }
                    if (getOutCollectors().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                        getOutCollectors().get(0).getSignal().setValueInteger(1);
                        for (CollectorInp collectorInp : getInpCollectorsConn()) {
                            //Add all sources signals together.
                            getOutCollectors().get(0).getSignal().setValueInteger(
                                    getOutCollectors().get(0).getSignal().getValueInteger() *
                                            collectorInp.getSignal().getValueInteger());
                        }
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