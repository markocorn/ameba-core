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

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Marko
 */
public class Delay extends Node {

    private ArrayList<Signal> buffer;
    //Initial value
    private Signal initValue;

    /**
     * @param minOutputEdges
     * @param maxOutputEdges
     * @param initValue
     * @param bufferSize
     */
    public Delay(Signal signalType, int minOutputEdges, int maxOutputEdges, Signal initValue, int bufferSize, int[] bufferSizeLimits) throws Exception {
        super(1, 1, 1, 1);
        this.initValue = initValue;
        getParams().add(Signal.createInteger(bufferSize));
        getParamsLimits().add(new Signal[]{Signal.createInteger(bufferSizeLimits[0]), Signal.createInteger(bufferSizeLimits[1])});

        addInpCollector(new CollectorInp(signalType.clone(), this));
        addOutCollector(new CollectorOut(signalType.clone(), minOutputEdges, maxOutputEdges, this));
        buffer = new ArrayList(Collections.nCopies(bufferSize, initValue));
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
                    buffer.add(getInpCollectors().get(0).getSignal());
                    buffer.remove(buffer.get(0));
                    getOutCollectors().get(0).setSignal(buffer.get(0));
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
        buffer = new ArrayList<>(Collections.nCopies(buffer.size(), initValue));
        getOutCollectors().get(0).setSignal(initValue);
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

