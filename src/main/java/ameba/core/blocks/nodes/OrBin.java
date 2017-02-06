package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorInp;
import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;

public class OrBin extends Node {

    public OrBin(int minInpCollectors, int maxInpCollectors, int minOutputEdges, int maxOutputEdges) throws Exception {
        super(minInpCollectors, maxInpCollectors, 1, 1);
        addOutCollector(new CollectorOut(Signal.createBoolean(), minOutputEdges, maxOutputEdges, this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    getOutCollectors().get(0).getSignal().setValueBoolean(false);
                    for (CollectorInp collectorInp : getInpCollectors()) {
                        //Add all sources signals together.
                        if (collectorInp.getSignal().getValueBoolean())
                            getOutCollectors().get(0).getSignal().setValueBoolean(true);
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

