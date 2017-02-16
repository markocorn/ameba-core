package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Edge;
import ameba.core.blocks.conectivity.Signal;

import java.util.ArrayList;

/**
 * Base node class.
 */
public class Node implements INode, Cloneable {
    /**
     * List of input collectors of the node.
     */
    private ArrayList<CollectorInp> inpCollectors;
    private ArrayList<CollectorOut> outCollectors;

    /**
     * Flag that indicates signal is reads to be sent to other ameba.core.blocks.nodes.
     */
    private boolean signalInputsReady;
    private boolean signalReady;
    private boolean signalSend;
    private boolean signalClcDone;

    private int state;

    private ArrayList<Signal> params;
    private ArrayList<Signal[]> paramsLimits;

    private int minInpCollectors;
    private int maxInpCollectors;
    private int minOutCollectors;
    private int maxOutCollectors;

    public Node(int minInpCollectors, int maxInpCollectors, int minOutCollectors, int maxOutCollectors) {
        this.minInpCollectors = minInpCollectors;
        this.maxInpCollectors = maxInpCollectors;
        this.minOutCollectors = minOutCollectors;
        this.maxOutCollectors = maxOutCollectors;

        inpCollectors = new ArrayList<>();
        outCollectors = new ArrayList<>();

        signalReady = false;
        signalInputsReady = false;
        signalClcDone = false;
        signalSend = false;
        state = 0;

        params = new ArrayList<>();
        paramsLimits = new ArrayList<>();
    }

    public int getMinInpCollectors() {
        return minInpCollectors;
    }

    public void setMinInpCollectors(int minInpCollectors) {
        this.minInpCollectors = minInpCollectors;
    }

    public int getMaxInpCollectors() {
        return maxInpCollectors;
    }

    public void setMaxInpCollectors(int maxInpCollectors) {
        this.maxInpCollectors = maxInpCollectors;
    }

    public int getMinOutCollectors() {
        return minOutCollectors;
    }

    public void setMinOutCollectors(int minOutCollectors) {
        this.minOutCollectors = minOutCollectors;
    }

    public int getMaxOutCollectors() {
        return maxOutCollectors;
    }

    public void setMaxOutCollectors(int maxOutCollectors) {
        this.maxOutCollectors = maxOutCollectors;
    }

    /**
     * Check if node's flag to indicate that node has prepared it's output signal is set.
     *
     * @return
     */

    public boolean isSignalReady() {
        return signalReady;
    }

    /**
     * Set node's flag to indicate that node has prepared it's output signal.
     *
     * @param signalReady Value od signal read flag. True signal is ready. False signal isn't ready.
     */
    public void setSignalReady(boolean signalReady) {
        this.signalReady = signalReady;
    }

    public void addInpCollector(CollectorInp collector) throws Exception {
        if (inpCollectors.size() < maxInpCollectors) {
            inpCollectors.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
    }

    public void addInpCollector(Signal signal) throws Exception {
        addInpCollector(new CollectorInp(signal, this));
    }

    public void addOutCollector(CollectorOut collector) throws Exception {
        if (outCollectors.size() < maxOutCollectors) {
            outCollectors.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum output collectors limitation of node: " + this.getClass().getSimpleName());
    }

    public void addOutCollector(Signal signal) throws Exception {
        addOutCollector(new CollectorOut(signal, this));
    }

    public ArrayList<CollectorInp> getInpCollectorsConn() {
        ArrayList<CollectorInp> collectorInps = new ArrayList<>();
        for (CollectorInp collectorInp : inpCollectors) {
            if (collectorInp.getEdges().size() > 0) {
                collectorInps.add(collectorInp);
            }
        }
        return collectorInps;
    }

    public ArrayList<CollectorInp> getInpCollectors() {
        return inpCollectors;
    }

    public void setInpCollectors(ArrayList<CollectorInp> inpCollectors) {
        this.inpCollectors = inpCollectors;
    }

    public ArrayList<CollectorOut> getOutCollectors() {
        return outCollectors;
    }

    public void setOutCollectors(ArrayList<CollectorOut> outCollectors) {
        this.outCollectors = outCollectors;
    }

    public ArrayList<Signal> getParams() {
        return params;
    }

    public void setParams(ArrayList<Signal> params) {
        this.params = params;
    }

    public ArrayList<Signal[]> getParamsLimits() {
        return paramsLimits;
    }

    public void setParamsLimits(int index, Signal[] paramsLimits) {
        this.paramsLimits.set(index, paramsLimits);
    }

    /**
     * Was signal of the node already send.
     *
     * @return
     */
    public boolean isSignalSend() {
        return signalSend;
    }

    public void setSignalSend(boolean signalSend) {
        this.signalSend = signalSend;
    }

    @Override
    public void clcNode() throws Exception {
    }

    @Override
    public void clearNode() {
        rstNode();
    }

    @Override
    public void rstNode() {
        signalInputsReady = false;
        signalReady = false;
        signalSend = false;
        signalClcDone = false;
        state = 0;
    }

    public boolean isSignalInputsReady() {
        signalInputsReady = true;
        for (CollectorInp collectorInp : inpCollectors) {
            for (Edge edge : collectorInp.getEdges()) {
                if (!edge.isSignalReady()) {
                    signalInputsReady = false;
                }
            }
        }
        return signalInputsReady;
    }

    public void setSignalInputsReady(boolean signalInputsReady) {
        this.signalInputsReady = signalInputsReady;
    }

    public boolean isSignalClcDone() {
        return signalClcDone;
    }

    public void setSignalClcDone(boolean signalClcDone) {
        this.signalClcDone = signalClcDone;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}

