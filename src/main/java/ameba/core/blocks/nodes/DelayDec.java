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

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Marko
 */
public class DelayDec extends Node {

    private ArrayList<Double> buffer;
    //Maximum length of buffer
    private int maxBuffer;
    //Initial value
    private double initValue = 0;

    /**
     * @param minOutputEdges
     * @param maxOutputEdges
     * @param initValue
     * @param bufferSize
     */
    public DelayDec(int minOutputEdges, int maxOutputEdges, double initValue, int bufferSize, int maxBuffer) {
        super();
        addInpCollector(Double.class, new CollectorInp(1, 1, this));
        addOutCollector(Double.class, new CollectorOut(minOutputEdges, maxOutputEdges, this));
        this.initValue = initValue;
        setParamsInt(new ArrayList<Integer>(bufferSize));
        buffer = new ArrayList(Collections.nCopies(bufferSize, initValue));
        setSignalDec(initValue);
        setSignalReady(true);
        this.maxBuffer = maxBuffer;
    }

    //Calculate output value
    @Override
    public void clcNode() {
        if (getInpCollectors().get(0).isSignalReady() && getInpCollectors().get(0).getNodeAttached().isSignalSend()) {
            buffer.add((Double) getInpCollectors().get(0).getSignal(Double.class));
            buffer.remove(buffer.get(0));
            setSignalDec(buffer.get(0));
        }
    }

    @Override
    public void clearNode() {
        buffer = new ArrayList<>(Collections.nCopies(buffer.size(), initValue));
        rstNode();
        setSignalDec(initValue);
    }

    @Override
    public void rstNode() {
        setSignalReady(true);
    }
}

