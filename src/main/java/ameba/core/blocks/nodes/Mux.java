package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Signal;

/**
 * Created by marko on 2/6/17.
 */
public class Mux extends Node {


    public Mux(int minInpCollectors, int maxInpCollectors, Signal par, Signal[] parLimits) throws Exception {
        super(minInpCollectors, maxInpCollectors + 1, 1, 1);
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        for (int i = 0; i < maxInpCollectors; i++) {
            addInpCollector(new CollectorInp(par.clone(), this));
        }
        addOutCollector(new CollectorOut(par.clone(), this));

        getParams().add(par);
        getParamsLimits().add(parLimits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    Signal[] list = new Signal[getInpCollectorsConn().size()];
                    for (int i = 0; i < getInpCollectorsConn().size(); i++) {
                        list[i] = getInpCollectorsConn().get(i).getSignal();
                    }
                    if (list[0].getValueInteger() >= 0 && list[0].getValueInteger() < getInpCollectorsConn().size() - 1) {
                        getOutCollectors().get(0).setSignal(getInpCollectorsConn().get(list[0].getValueInteger() + 1).getSignal());
                    } else {
                        getOutCollectors().get(0).setSignal(getParams().get(0));
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
