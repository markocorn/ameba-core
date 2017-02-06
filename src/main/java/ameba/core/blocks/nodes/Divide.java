package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorInp;
import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;

public class Divide extends Node {

    public Divide(Signal outSignal, int minInpCollectors, int maxInpCollectors, int minOutputEdges, int maxOutputEdges) throws Exception {
        super(minInpCollectors, maxInpCollectors, 1, 1);
        addOutCollector(new CollectorOut(outSignal.clone(), minOutputEdges, maxOutputEdges, this));
        if (outSignal.gettClass().isAssignableFrom(Boolean.class))
            throw new Exception("Multiply node is not allowed as Boolean type node");
        if (outSignal.gettClass().isAssignableFrom(Integer.class))
            throw new Exception("Multiply node is not allowed as Integer type node");

    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    if (getOutCollectors().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                        getOutCollectors().get(0).getSignal().setValueDouble(1.0);
                        for (CollectorInp collectorInp : getInpCollectors()) {
                            //Add all sources signals together.
                            if (collectorInp.getSignal().getValueDouble().equals(0.0)) {
                                getOutCollectors().get(0).getSignal().setValueDouble(0.0);
                                break;
                            } else {
                                getOutCollectors().get(0).getSignal().setValueDouble(getOutCollectors().get(0).getSignal().getValueDouble() / collectorInp.getSignal().getValueDouble());
                            }
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