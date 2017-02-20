package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class AndBin extends Node {

    public AndBin(int minInpCollectors, int maxInpCollectors) throws Exception {
        super(minInpCollectors, maxInpCollectors, 1, 1);
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorInp(Signal.createBoolean(), this));
        }
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    getOutCollectors().get(0).getSignal().setValueBoolean(true);
                    for (CollectorInp collectorInp : getInpCollectorsConn()) {
                        //Add all sources signals together.
                        if (!collectorInp.getSignal().getValueBoolean())
                            getOutCollectors().get(0).getSignal().setValueBoolean(false);
                    }
                    setSignalClcDone(true);
                    setState(1);
                }
                break;
            case 1:
                if (isSignalClcDone()) {
                    setSignalReady(true);
                    setState(2);
                }
                break;
            case 2:
                if (isSignalReady()) {
                    setState(3);
                }
                break;
            case 3:
                if (isSignalSend() || getOutCollectors().get(0).getEdges().size() == 0) {
                    setState(4);
                }
                break;
            case 4:
        }
    }
}
