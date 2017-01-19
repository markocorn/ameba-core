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
public class DelayBin extends Node {

    private ArrayList<Boolean> buffer;
    //Maximum length of buffer
    private int maxBuffer;
    //Initial value
    private boolean initValue = false;

    /**
     * @param minOutputEdges
     * @param maxOutputEdges
     * @param initValue
     * @param bufferSize
     */
    public DelayBin(int minOutputEdges, int maxOutputEdges, boolean initValue, int bufferSize, int maxBuffer) {
        super();
        addInpCollector(Boolean.class, new CollectorInp(1, 1, this));
        addOutCollector(Boolean.class, new CollectorOut(minOutputEdges, maxOutputEdges, this));
        this.initValue = initValue;
        setParamsBin(new ArrayList<Boolean>(bufferSize));
        buffer = new ArrayList(Collections.nCopies(bufferSize, initValue));
        setSignalBin(initValue);
        setSignalReady(true);
        this.maxBuffer = maxBuffer;
    }

    //Calculate output value
    @Override
    public void clcNode() {
        if (getInpCollectors().get(0).isSignalReady() && getInpCollectors().get(0).getNodeAttached().isSignalSend()) {
            buffer.add((Boolean) getInpCollectors().get(0).getSignal(Boolean.class));
            buffer.remove(buffer.get(0));
            setSignalBin(buffer.get(0));
        }
    }

    @Override
    public void clearNode() {
        buffer = new ArrayList<>(Collections.nCopies(buffer.size(), initValue));
        rstNode();
        setSignalBin(initValue);
    }

    @Override
    public void rstNode() {
        setSignalReady(true);
    }
}

