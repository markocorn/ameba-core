package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorInp;
import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;

public class CompareConst extends Node {

    public CompareConst(Signal inpSignal, int minOutputEdges, int maxOutputEdges, Signal compareAgainst, Signal[] compareAgainstLimits, int operation) throws Exception {
        super(1, 1, minOutputEdges, maxOutputEdges);
        addInpCollector(new CollectorInp(inpSignal.clone(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), minOutputEdges, maxOutputEdges, this));
        //Add compare against parameter
        getParams().add(compareAgainst);
        getParamsLimits().add(compareAgainstLimits);
        //Add operation select parameter
        getParams().add(Signal.createInteger(operation));
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
                            if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectors().get(0).getSignal().getValueDouble() > getParams().get(0).getValueDouble()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectors().get(0).getSignal().getValueInteger() > getParams().get(0).getValueInteger()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else
                                throw new Exception("Input signals of node " + this.getClass().getSimpleName() + " is not of type Double or Integer.");
                        }
                        break;
                        //Less than par
                        case 1: {
                            if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectors().get(0).getSignal().getValueDouble() < getParams().get(0).getValueDouble()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectors().get(0).getSignal().getValueInteger() < getParams().get(0).getValueInteger()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else
                                throw new Exception("Input signals of node " + this.getClass().getSimpleName() + " is not of type Double or Integer.");
                        }
                        break;
                        //Equal to par
                        case 2: {
                            if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectors().get(0).getSignal().getValueDouble().equals(getParams().get(0).getValueDouble())) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectors().get(0).getSignal().getValueInteger().equals(getParams().get(0).getValueInteger())) {
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
                if (isSignalSend()) {
                    setState(4);
                }
            case 4:
        }
    }
}

