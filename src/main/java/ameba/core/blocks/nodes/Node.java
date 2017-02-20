package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Edge;
import ameba.core.blocks.Signal;
import com.rits.cloning.Cloner;

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

    private Integer[] inpColLimitDec;
    private Integer[] inpColLimitInt;
    private Integer[] inpColLimitBin;

    private Integer[] outColLimitDec;
    private Integer[] outColLimitInt;
    private Integer[] outColLimitBin;

    public Node(Integer[] inpColLimitDec, Integer[] inpColLimitInt, Integer[] inpColLimitBin, Integer[] outColLimitDec, Integer[] outColLimitInt, Integer[] outColLimitBin) {
        this.inpColLimitDec = inpColLimitDec;
        this.inpColLimitInt = inpColLimitInt;
        this.inpColLimitBin = inpColLimitBin;
        this.outColLimitDec = outColLimitDec;
        this.outColLimitInt = outColLimitInt;
        this.outColLimitBin = outColLimitBin;

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

    public Integer[] getInpColLimitDec() {
        return inpColLimitDec;
    }

    public void setInpColLimitDec(Integer[] inpColLimitDec) {
        this.inpColLimitDec = inpColLimitDec;
    }

    public Integer[] getInpColLimitInt() {
        return inpColLimitInt;
    }

    public void setInpColLimitInt(Integer[] inpColLimitInt) {
        this.inpColLimitInt = inpColLimitInt;
    }

    public Integer[] getInpColLimitBin() {
        return inpColLimitBin;
    }

    public void setInpColLimitBin(Integer[] inpColLimitBin) {
        this.inpColLimitBin = inpColLimitBin;
    }

    public Integer[] getOutColLimitDec() {
        return outColLimitDec;
    }

    public void setOutColLimitDec(Integer[] outColLimitDec) {
        this.outColLimitDec = outColLimitDec;
    }

    public Integer[] getOutColLimitInt() {
        return outColLimitInt;
    }

    public void setOutColLimitInt(Integer[] outColLimitInt) {
        this.outColLimitInt = outColLimitInt;
    }

    public Integer[] getOutColLimitBin() {
        return outColLimitBin;
    }

    public void setOutColLimitBin(Integer[] outColLimitBin) {
        this.outColLimitBin = outColLimitBin;
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
        if (collector.getType().isAssignableFrom(Double.class)) {
            if (inpCollectors.size() < inpColLimitDec[1]) {
                inpCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
        }
        if (collector.getType().isAssignableFrom(Integer.class)) {
            if (inpCollectors.size() < inpColLimitInt[1]) {
                inpCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
        }
        if (collector.getType().isAssignableFrom(Boolean.class)) {
            if (inpCollectors.size() < inpColLimitBin[1]) {
                inpCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
        }

    }

    public void addInpCollector(Signal signal) throws Exception {
        addInpCollector(new CollectorInp(signal, this));
    }

    public void addOutCollector(CollectorOut collector) throws Exception {
        if (collector.getType().isAssignableFrom(Double.class)) {
            if (outCollectors.size() < outColLimitDec[1]) {
                outCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum output collectors limitation of node: " + this.getClass().getSimpleName());
        }
        if (collector.getType().isAssignableFrom(Integer.class)) {
            if (outCollectors.size() < outColLimitInt[1]) {
                outCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum output collectors limitation of node: " + this.getClass().getSimpleName());
        }
        if (collector.getType().isAssignableFrom(Boolean.class)) {
            if (outCollectors.size() < outColLimitBin[1]) {
                outCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum output collectors limitation of node: " + this.getClass().getSimpleName());
        }
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

    public ArrayList<CollectorInp> getInpCollector(Class type) {
        ArrayList<CollectorInp> collectors = new ArrayList<>();
        for (CollectorInp collector : inpCollectors) {
            if (collector.getType().equals(type)) {
                collectors.add(collector);
            }
        }
        return collectors;
    }

    public ArrayList<CollectorInp> getInpCollectorConnected(Class type) {
        ArrayList<CollectorInp> collectors = new ArrayList<>();
        for (CollectorInp collector : inpCollectors) {
            if (collector.getType().equals(type) && collector.getEdges().size() > 0) {
                collectors.add(collector);
            }
        }
        return collectors;
    }

    public ArrayList<CollectorOut> getOutCollector(Class type) {
        ArrayList<CollectorOut> collectors = new ArrayList<>();
        for (CollectorOut collector : outCollectors) {
            if (collector.getType().equals(type)) {
                collectors.add(collector);
            }
        }
        return collectors;
    }

    public ArrayList<CollectorOut> getOutCollectorConnected(Class type) {
        ArrayList<CollectorOut> collectors = new ArrayList<>();
        for (CollectorOut collector : outCollectors) {
            if (collector.getType().equals(type) && collector.getEdges().size() > 0) {
                collectors.add(collector);
            }
        }
        return collectors;
    }

    public ArrayList<CollectorInp> getInpCollectorMin(Class type) {
        ArrayList<CollectorInp> collectors = new ArrayList<>();
        int num =
        if (type.isAssignableFrom(Double.class))
            for (CollectorInp collector : inpCollectors) {
                if (collector.getType().equals(type) && collector.getEdges().size() > 0) {
                    collectors.add(collector);
                }
            }
        return collectors;
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

    public void setParam(int ind, Signal par) {
        params.set(ind, par);
    }

    public Signal getParam(int ind) {
        return params.get(ind);
    }

    public Node clone() {
        Cloner cloner = new Cloner();
        return cloner.deepClone(this);
    }
}

