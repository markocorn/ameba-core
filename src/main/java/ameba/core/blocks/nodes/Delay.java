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

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Marko
 */
public class Delay extends Node {

    private ArrayList<Signal> buffer;
    //Initial value
    private Signal initValue;

    public Delay(Signal initial, int par, int[] parLimits) throws Exception {
        super(1, 1, 1, 1);
        this.initValue = initial;
        getParams().add(Signal.createInteger(par));
        getParamsLimits().add(new Signal[]{Signal.createInteger(parLimits[0]), Signal.createInteger(parLimits[1])});

        addInpCollector(new CollectorInp(initial.clone(), this));
        addOutCollector(new CollectorOut(initial.clone(), this));
        buffer = new ArrayList(Collections.nCopies(par, initial));
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
                break;
            case 1:
                if (isSignalSend() || getOutCollectors().get(0).getEdges().size() == 0) {
                    setState(2);
                }
                break;
            case 2:
                if (isSignalInputsReady()) {
                    buffer.add(getInpCollectorsConn().get(0).getSignal());
                    buffer.remove(buffer.get(0));
                    getOutCollectors().get(0).setSignal(buffer.get(0));
                    setState(3);
                    setSignalClcDone(true);
                }
                break;
            case 3:
                if (isSignalClcDone()) {
                    setState(4);
                }
                break;
            case 4:
                break;
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

