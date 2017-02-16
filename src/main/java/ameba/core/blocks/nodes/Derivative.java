package ameba.core.blocks.nodes;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Signal;

/**
 * @author Marko
 */
public class Derivative extends Node {

    //Initial value
    private Signal initValue;
    private Signal signalOld;

    public Derivative(Signal initial, Signal par, Signal[] parLimits) throws Exception {
        super(1, 1, 1, 1);
        this.initValue = initial;
        signalOld = initial.clone();
        addInpCollector(new CollectorInp(initial.clone(), this));
        addOutCollector(new CollectorOut(initial.clone(), this));


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
                    if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                        getOutCollectors().get(0).getSignal().setValueDouble((getInpCollectorsConn().get(0).getSignal().getValueDouble() - signalOld.getValueDouble()) / getParams().get(0).getValueDouble());
                        signalOld = getInpCollectorsConn().get(0).getSignal();
                    }
                    if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                        getOutCollectors().get(0).getSignal().setValueInteger((getInpCollectorsConn().get(0).getSignal().getValueInteger() - signalOld.getValueInteger()) / getParams().get(0).getValueInteger());
                        signalOld = getInpCollectorsConn().get(0).getSignal();
                    }
                    if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Boolean.class)) {
                        getOutCollectors().get(0).getSignal().setValueBoolean(getParams().get(0).getValueBoolean());
                        if (!signalOld.getValueBoolean().equals(getInpCollectorsConn().get(0).getSignal().getValueBoolean())) {
                            getOutCollectors().get(0).getSignal().setValueBoolean(!getParams().get(0).getValueBoolean());
                        }
                        signalOld = getInpCollectorsConn().get(0).getSignal();
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