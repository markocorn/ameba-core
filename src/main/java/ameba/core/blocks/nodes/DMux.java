package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorInp;
import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;

/**
 * Created by marko on 2/6/17.
 */
public class DMux extends Node {
    int minOutputEdgesPerOutputCollector;
    int maxOutputEdgesPerOutputCollector;

    public DMux(int minOutCollectors, int maxOutCollectors, int minOutputEdgesPerOutputCollector, int maxOutputEdgesPerOutputCollector, Signal const1, Signal[] const1Limits) throws Exception {
        super(2, 2, minOutCollectors, maxOutCollectors);
        this.minOutputEdgesPerOutputCollector = minOutputEdgesPerOutputCollector;
        this.maxOutputEdgesPerOutputCollector = maxOutputEdgesPerOutputCollector;
        addInpCollector(new CollectorInp(Signal.createInteger(), this));
        addInpCollector(new CollectorInp(const1.clone(), this));

        getParams().add(const1);
        getParamsLimits().add(const1Limits);
    }


    @Override
    public void addOutCollector(CollectorOut collector) throws Exception {
        if (getParams().get(0).gettClass().equals(collector.getType())) {
            super.addOutCollector(new CollectorOut(new Signal(getParams().get(0).gettClass()), minOutputEdgesPerOutputCollector, maxOutputEdgesPerOutputCollector, this));
        } else {
            throw new Exception("Output collector's signal must be of type: " + getParams().get(0).gettClass().getSimpleName());
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
