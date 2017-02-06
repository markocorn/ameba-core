package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorInp;
import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;

public class Compare extends Node {

    public Compare(Signal inpSignal, int minOutputEdges, int maxOutputEdges, int operation) throws Exception {
        super(1, 2, minOutputEdges, maxOutputEdges);
        addInpCollector(new CollectorInp(inpSignal.clone(), this));
        addInpCollector(new CollectorInp(inpSignal.clone(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), minOutputEdges, maxOutputEdges, this));
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
                    switch (getParams().get(0).getValueInteger()) {
                        //Greater than par
                        case 0: {
                            if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectors().get(0).getSignal().getValueDouble() > getInpCollectors().get(1).getSignal().getValueDouble()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectors().get(0).getSignal().getValueInteger() > getInpCollectors().get(1).getSignal().getValueInteger()) {
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
                                if (getInpCollectors().get(0).getSignal().getValueDouble() < getInpCollectors().get(1).getSignal().getValueDouble()) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectors().get(0).getSignal().getValueInteger() < getInpCollectors().get(1).getSignal().getValueInteger()) {
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
                                if (getInpCollectors().get(0).getSignal().getValueDouble().equals(getInpCollectors().get(1).getSignal().getValueDouble())) {
                                    getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                                }
                            } else if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                                getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                                if (getInpCollectors().get(0).getSignal().getValueInteger().equals(getInpCollectors().get(1).getSignal().getValueInteger())) {
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

