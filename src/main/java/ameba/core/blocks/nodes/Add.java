package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Signal;


public class Add extends Node {

    public Add(Signal type, int minInpCollectors, int maxInpCollectors) throws Exception {
        super(minInpCollectors, maxInpCollectors, 1, 1);

        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorInp(type.clone(), this));
        }
        addOutCollector(new CollectorOut(type.clone(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    if (getOutCollectors().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                        getOutCollectors().get(0).getSignal().setValueDouble(0.0);
                        for (CollectorInp collectorInp : getInpCollectorsConn()) {
                            //Add all sources signals together.
                            getOutCollectors().get(0).getSignal().setValueDouble(
                                    getOutCollectors().get(0).getSignal().getValueDouble() +
                                            collectorInp.getSignal().getValueDouble());
                        }
                    }
                    if (getOutCollectors().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                        getOutCollectors().get(0).getSignal().setValueInteger(0);
                        for (CollectorInp collectorInp : getInpCollectorsConn()) {
                            //Add all sources signals together.
                            getOutCollectors().get(0).getSignal().setValueInteger(
                                    getOutCollectors().get(0).getSignal().getValueInteger() +
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
                if (isSignalSend() || getOutCollectors().get(0).getEdges().size() > 0) {
                    setState(4);
                }
            case 4:
        }
    }
}

