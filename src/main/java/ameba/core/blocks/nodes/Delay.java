package ameba.core.blocks.nodes;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Marko
 */
public class Delay extends Node implements Serializable {

    private ArrayList<Double> buffer;
    //Maximum length of buffer
    private int maxBuffer;
    //Initial value
    private double initValue = 0;

    public Delay(int maxOutputEdges, double initValue, int bufferSize) {
        super(1, maxOutputEdges);
        this.initValue = initValue;
        setIntegerParameters(new int[]{bufferSize});
        buffer = new ArrayList<Double>(Collections.nCopies(bufferSize, new Double(initValue)));
        setSignal(initValue);
        setSignalReady(true);
    }

    //Calculate output value
    @Override
    public void clcNode() {
        if (getInputEdges().size() > 0) {
            //Signal from source node has to be ready and not send.
            if (getInputEdges().get(0).isSignalReady() && !getInputEdges().get(0).isSignalSend()) {
                setSignal(buffer.get(0));
                buffer.add(getInputEdges().get(0).getSignal());
                buffer.remove(0);
            }
        }
    }

    @Override
    public void clearNode() {
        buffer = new ArrayList<Double>(Collections.nCopies(buffer.size(), new Double(initValue)));
        rstNode();
        setSignal(initValue);
    }

    @Override
    public void rstNode() {
        setSignalReady(true);
        setSignal(buffer.get(0));
    }
}

