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
public class DelayInt extends Node {

    private ArrayList<Integer> buffer;
    //Maximum length of buffer
    private int maxBuffer;
    //Initial value
    private int initValue = 0;

    /**
     * @param minOutputEdges
     * @param maxOutputEdges
     * @param initValue
     * @param bufferSize
     */
    public DelayInt(int minOutputEdges, int maxOutputEdges, int initValue, int bufferSize, int maxBuffer) {
        super();
        addInpCollector(Integer.class, new CollectorInp(1, 1, this));
        addOutCollector(Integer.class, new CollectorOut(minOutputEdges, maxOutputEdges, this));
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
            buffer.add((Integer) getInpCollectors().get(0).getSignal(Integer.class));
            buffer.remove(buffer.get(0));
            setSignalInt(buffer.get(0));
        }
    }

    @Override
    public void clearNode() {
        buffer = new ArrayList<>(Collections.nCopies(buffer.size(), initValue));
        rstNode();
        setSignalInt(initValue);
    }

    @Override
    public void rstNode() {
        setSignalReady(true);
    }
}

