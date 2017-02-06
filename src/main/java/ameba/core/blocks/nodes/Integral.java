package ameba.core.blocks.nodes;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.connections.CollectorInp;
import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;

/**
 * @author Marko
 */
public class Integral extends Node {
    private Signal initValue;
    private Signal signalOld;


    public Integral(Signal signalType, int minOutputEdges, int maxOutputEdges, Signal initValue, Signal par, Signal[] parLimits) throws Exception {
        super(1, 1, minOutputEdges, maxOutputEdges);
        this.initValue = initValue;
        signalOld = initValue.clone();
        addInpCollector(new CollectorInp(signalType.clone(), this));
        addOutCollector(new CollectorOut(signalType.clone(), minOutputEdges, maxOutputEdges, this));


        getParams().add(par);
        getParamsLimits().add(parLimits);

        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalReady()) {
                    setState(1);
                }
            case 1:
                if (isSignalSend()) {
                    setState(2);
                }
            case 2:
                if (isSignalInputsReady()) {
                    if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                        getOutCollectors().get(0).getSignal().setValueDouble(getInpCollectors().get(0).getSignal().getValueDouble() * getParams().get(0).getValueDouble() + signalOld.getValueDouble());
                        signalOld = getOutCollectors().get(0).getSignal().clone();
                    }
                    if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                        getOutCollectors().get(0).getSignal().setValueInteger(getInpCollectors().get(0).getSignal().getValueInteger() * getParams().get(0).getValueInteger() + signalOld.getValueInteger());
                        signalOld = getOutCollectors().get(0).getSignal().clone();
                    }
                    if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Boolean.class)) {
                        if (getInpCollectors().get(0).getSignal().getValueBoolean().equals(true)) {
                            getOutCollectors().get(0).getSignal().setValueBoolean(!getParams().get(0).getValueBoolean());
                            signalOld.setValueBoolean(true);
                        }
                        if (getParams().get(0).getValueBoolean()) {
                            getOutCollectors().get(0).getSignal().setValueBoolean(!signalOld.getValueBoolean());
                        } else {
                            getOutCollectors().get(0).getSignal().setValueBoolean(signalOld.getValueBoolean());
                        }
                    }
                    setState(3);
                    setSignalClcDone(true);
                }
            case 3:
                if (isSignalClcDone()) {
                    setState(4);
                }
            case 4:
        }
    }

    @Override
    public void clearNode() {
        rstNode();
        try {
            getOutCollectors().get(0).setSignal(initValue);
            signalOld.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void rstNode() {
        setSignalInputsReady(false);
        setSignalReady(true);
        setSignalSend(false);
        setSignalClcDone(false);
        setState(0);
    }
}