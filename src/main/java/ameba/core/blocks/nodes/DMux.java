package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Signal;

/**
 * Created by marko on 2/6/17.
 */
public class DMux extends Node {

    public DMux(int minOutCollectors, int maxOutCollectors, Signal par, Signal[] parLimits) throws Exception {
        super(2, 2, minOutCollectors, maxOutCollectors);
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addInpCollector(new CollectorInp(par.clone(), this));
        for (int i = 0; i < maxOutCollectors; i++) {
            addOutCollector(new CollectorOut(par.clone(), this));
        }

        getParams().add(par);
        getParamsLimits().add(parLimits);
    }


//    @Override
//    public void addOutCollector(CollectorOut collector) throws Exception {
//        if (getParams().get(0).gettClass().equals(collector.getType())) {
//            super.addOutCollector(new CollectorOut(new Signal(getParams().get(0).gettClass()), this));
//        } else {
//            throw new Exception("Output collector's signal must be of type: " + getParams().get(0).gettClass().getSimpleName());
//        }
//
//    }

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
                    for (int i = 0; i < getOutCollectors().size(); i++) {
                        if (i == list[0].getValueInteger()) {
                            getOutCollectors().get(i).setSignal(list[1]);
                        } else {
                            getOutCollectors().get(i).setSignal(getParams().get(0));
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
