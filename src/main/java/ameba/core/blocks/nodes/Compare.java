package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Signal;

public class Compare extends Node {

    public Compare(Signal type, int par) throws Exception {
        super(1, 2, 1, 1);
        addInpCollector(new CollectorInp(type.clone(), this));
        addInpCollector(new CollectorInp(type.clone(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));
        //Add operation select parameter
        getParams().add(Signal.createInteger(par));
        getParamsLimits().add(new Signal[]{Signal.createInteger(0), Signal.createInteger(2)});
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    switch (getParams().get(0).getValueInteger()) {
                        //Greater than par
                        case 0: {
                            if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectorsConn().get(0).getSignal().getValueDouble() > getInpCollectorsConn().get(1).getSignal().getValueDouble()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectorsConn().get(0).getSignal().getValueInteger() > getInpCollectorsConn().get(1).getSignal().getValueInteger()) {
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
                                if (getInpCollectorsConn().get(0).getSignal().getValueDouble() < getInpCollectorsConn().get(1).getSignal().getValueDouble()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectorsConn().get(0).getSignal().getValueInteger() < getInpCollectorsConn().get(1).getSignal().getValueInteger()) {
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
                                if (getInpCollectorsConn().get(0).getSignal().getValueDouble().equals(getInpCollectorsConn().get(1).getSignal().getValueDouble())) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectorsConn().get(0).getSignal().getValueInteger().equals(getInpCollectorsConn().get(1).getSignal().getValueInteger())) {
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

