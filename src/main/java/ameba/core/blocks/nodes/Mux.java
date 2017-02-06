package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorInp;
import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;

/**
 * Created by marko on 2/6/17.
 */
public class Mux extends Node {


    public Mux(int minInpCollectors, int maxInpCollectors, int minOutputEdges, int maxOutputEdges, Signal const1, Signal[] const1Limits) throws Exception {
        super(minInpCollectors, maxInpCollectors, 1, 1);

        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addOutCollector(new CollectorOut(const1.clone(), minOutputEdges, maxOutputEdges, this));

        getParams().add(const1);
        getParamsLimits().add(const1Limits);
    }

    @Override
    public void addInpCollector(CollectorInp collector) throws Exception {
        if (getInpCollectors().size() == 0) {
            super.addInpCollector(collector);
        } else {
            if (getParams().get(0).gettClass().equals(collector.getType())) {
                super.addInpCollector(collector);
            } else {
                throw new Exception("Input collector's signal must be of type: " + getParams().get(0).gettClass().getSimpleName());
            }
        }
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    Signal[] list = new Signal[getInpCollectors().size()];
                    for (int i = 0; i < getInpCollectors().size(); i++) {
                        list[i] = getInpCollectors().get(i).getSignal();
                    }
                    if (list[0].getValueInteger() >= 0 && list[0].getValueInteger() < getInpCollectors().size() - 1) {
                        getOutCollectors().get(0).setSignal(getInpCollectors().get(list[0].getValueInteger() + 1).getSignal());
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
