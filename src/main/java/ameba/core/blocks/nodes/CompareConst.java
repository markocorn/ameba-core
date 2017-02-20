package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

public class CompareConst extends Node {

    public CompareConst(Signal par1, Signal[] par1Limits, int par2) throws Exception {
        super(1, 1, 1, 1);
        addInpCollector(new CollectorInp(par1.clone(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));
        //Add compare against parameter
        getParams().add(par1);
        getParamsLimits().add(par1Limits);
        //Add operation select parameter
        getParams().add(Signal.createInteger(par2));
        getParamsLimits().add(new Signal[]{Signal.createInteger(0), Signal.createInteger(2)});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    switch (getParams().get(1).getValueInteger()) {
                        //Greater than par
                        case 0: {
                            if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectorsConn().get(0).getSignal().getValueDouble() > getParams().get(0).getValueDouble()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectorsConn().get(0).getSignal().getValueInteger() > getParams().get(0).getValueInteger()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else
                                throw new Exception("Input signals of node " + this.getClass().getSimpleName() + " is not of type Double or Integer.");
                        }
                        break;
                        //Less than par
                        case 1: {
                            if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectorsConn().get(0).getSignal().getValueDouble() < getParams().get(0).getValueDouble()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectorsConn().get(0).getSignal().getValueInteger() < getParams().get(0).getValueInteger()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else
                                throw new Exception("Input signals of node " + this.getClass().getSimpleName() + " is not of type Double or Integer.");
                        }
                        break;
                        //Equal to par
                        case 2: {
                            if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectorsConn().get(0).getSignal().getValueDouble().equals(getParams().get(0).getValueDouble())) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectorsConn().get(0).getSignal().getValueInteger().equals(getParams().get(0).getValueInteger())) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else
                                throw new Exception("Input signals of node " + this.getClass().getSimpleName() + " is not of type Double or Integer.");
                        }
                        break;
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

