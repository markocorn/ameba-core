package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorInp;
import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;


public class Add extends Node {

    public Add(Signal outSignal, int minInpCollectors, int maxInpCollectors, int minOutputEdges, int maxOutputEdges) throws Exception {
        super(minInpCollectors, maxInpCollectors, 1, 1);
        addOutCollector(new CollectorOut(outSignal.clone(), minOutputEdges, maxOutputEdges, this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    if (getOutCollectors().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                        getOutCollectors().get(0).getSignal().setValueDouble(0.0);
                        for (CollectorInp collectorInp : getInpCollectors()) {
                            //Add all sources signals together.
                            getOutCollectors().get(0).getSignal().setValueDouble(
                                    getOutCollectors().get(0).getSignal().getValueDouble() +
                                            collectorInp.getSignal().getValueDouble());
                        }
                    }
                    if (getOutCollectors().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                        getOutCollectors().get(0).getSignal().setValueInteger(0);
                        for (CollectorInp collectorInp : getInpCollectors()) {
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
                if (isSignalSend()) {
                    setState(4);
                }
            case 4:
        }
    }
}

