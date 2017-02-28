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
    private ArrayList<CollectorInp> inpCollectorsDec;
    private ArrayList<CollectorInp> inpCollectorsInt;
    private ArrayList<CollectorInp> inpCollectorsBin;
    private ArrayList<CollectorOut> outCollectorsDec;
    private ArrayList<CollectorOut> outCollectorsInt;
    private ArrayList<CollectorOut> outCollectorsBin;

    private Integer[] inpColLimitDec;
    private Integer[] inpColLimitInt;
    private Integer[] inpColLimitBin;

    private Integer[] outColLimitDec;
    private Integer[] outColLimitInt;
    private Integer[] outColLimitBin;

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


    public Node(Integer[] inpColLimitDec, Integer[] inpColLimitInt, Integer[] inpColLimitBin, Integer[] outColLimitDec, Integer[] outColLimitInt, Integer[] outColLimitBin) {
        this.inpColLimitDec = inpColLimitDec;
        this.inpColLimitInt = inpColLimitInt;
        this.inpColLimitBin = inpColLimitBin;
        this.outColLimitDec = outColLimitDec;
        this.outColLimitInt = outColLimitInt;
        this.outColLimitBin = outColLimitBin;

        inpCollectors = new ArrayList<>();
        outCollectors = new ArrayList<>();
        inpCollectorsDec = new ArrayList<>();
        inpCollectorsInt = new ArrayList<>();
        inpCollectorsBin = new ArrayList<>();
        outCollectorsDec = new ArrayList<>();
        outCollectorsInt = new ArrayList<>();
        outCollectorsBin = new ArrayList<>();

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

    public Integer[] getInpColLimit(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) return inpColLimitDec;
        if (type.isAssignableFrom(Integer.class)) return inpColLimitInt;
        if (type.isAssignableFrom(Boolean.class)) return inpColLimitBin;
        throw new Exception("No input collectors limits of type: " + type.getSimpleName());
    }

    public Integer[] getOutColLimit(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) return outColLimitDec;
        if (type.isAssignableFrom(Integer.class)) return outColLimitInt;
        if (type.isAssignableFrom(Boolean.class)) return outColLimitBin;
        throw new Exception("No output collectors limits of type: " + type.getSimpleName());
    }



    public Integer[] getInpColLimitInt() {
        return inpColLimitInt;
    }


    public Integer[] getInpColLimitBin() {
        return inpColLimitBin;
    }


    public Integer[] getOutColLimitDec() {
        return outColLimitDec;
    }


    public Integer[] getOutColLimitInt() {
        return outColLimitInt;
    }


    public Integer[] getOutColLimitBin() {
        return outColLimitBin;
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
            if (inpCollectorsDec.size() < inpColLimitDec[1]) {
                inpCollectorsDec.add(collector);
                inpCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
        }
        if (collector.getType().isAssignableFrom(Integer.class)) {
            if (inpCollectorsInt.size() < inpColLimitInt[1]) {
                inpCollectorsInt.add(collector);
                inpCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
        }
        if (collector.getType().isAssignableFrom(Boolean.class)) {
            if (inpCollectorsBin.size() < inpColLimitBin[1]) {
                inpCollectorsBin.add(collector);
                inpCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
        }

    }

    public void addOutCollector(CollectorOut collector) throws Exception {
        if (collector.getType().isAssignableFrom(Double.class)) {
            if (outCollectorsDec.size() < outColLimitDec[1]) {
                outCollectorsDec.add(collector);
                outCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum output collectors limitation of node: " + this.getClass().getSimpleName());
        }
        if (collector.getType().isAssignableFrom(Integer.class)) {
            if (outCollectorsInt.size() < outColLimitInt[1]) {
                outCollectorsInt.add(collector);
                outCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum output collectors limitation of node: " + this.getClass().getSimpleName());
        }
        if (collector.getType().isAssignableFrom(Boolean.class)) {
            if (outCollectorsBin.size() < outColLimitBin[1]) {
                outCollectorsBin.add(collector);
                outCollectors.add(collector);
            } else
                throw new Exception("Collector can't be added. Maximum output collectors limitation of node: " + this.getClass().getSimpleName());
        }
    }


    public ArrayList<CollectorInp> getInpCollectorsConnected(Class type) {
        ArrayList<CollectorInp> collectorInps = new ArrayList<>();
        if (type.isAssignableFrom(Double.class)) {
            for (CollectorInp collectorInp : inpCollectorsDec) {
                if (collectorInp.getEdges().size() > 0) {
                    collectorInps.add(collectorInp);
                }
            }
        }
        if (type.isAssignableFrom(Integer.class)) {
            for (CollectorInp collectorInp : inpCollectorsInt) {
                if (collectorInp.getEdges().size() > 0) {
                    collectorInps.add(collectorInp);
                }
            }
        }
        if (type.isAssignableFrom(Boolean.class)) {
            for (CollectorInp collectorInp : inpCollectorsBin) {
                if (collectorInp.getEdges().size() > 0) {
                    collectorInps.add(collectorInp);
                }
            }

        }
        return collectorInps;
    }


    public ArrayList<CollectorOut> getOutCollectorsConnected(Class type) {
        ArrayList<CollectorOut> collectors = new ArrayList<>();
        if (type.isAssignableFrom(Double.class)) {
            for (CollectorOut collector : outCollectorsDec) {
                if (collector.getEdges().size() > 0) {
                    collectors.add(collector);
                }
            }
        }
        if (type.isAssignableFrom(Double.class)) {
            for (CollectorOut collector : outCollectorsInt) {
                if (collector.getEdges().size() > 0) {
                    collectors.add(collector);
                }
            }
        }
        if (type.isAssignableFrom(Double.class)) {
            for (CollectorOut collector : outCollectorsBin) {
                if (collector.getEdges().size() > 0) {
                    collectors.add(collector);
                }
            }
        }
        return collectors;
    }

    public ArrayList<CollectorInp> getInpCollectorsMin(Class type) {
        ArrayList<CollectorInp> collectors = new ArrayList<>();
        int num = 0;
        if (type.isAssignableFrom(Double.class)) {
            num = getInpColLimitDec()[0];
            for (CollectorInp collector : inpCollectorsDec) {
                if (collector.getEdges().size() == 0) {
                    collectors.add(collector);
                    num--;
                }
                if (num <= 0) {
                    break;
                }
            }
        }
        if (type.isAssignableFrom(Integer.class)) {
            num = getInpColLimitInt()[0];
            for (CollectorInp collector : inpCollectorsInt) {
                if (collector.getEdges().size() == 0) {
                    collectors.add(collector);
                    num--;
                }
                if (num <= 0) {
                    break;
                }
            }
        }
        if (type.isAssignableFrom(Boolean.class)) {
            num = getInpColLimitBin()[0];
            for (CollectorInp collector : inpCollectorsBin) {
                if (collector.getEdges().size() == 0) {
                    collectors.add(collector);
                    num--;
                }
                if (num <= 0) {
                    break;
                }
            }
        }

        return collectors;
    }

    public ArrayList<CollectorInp> getInpCollectorsMinConnect() {
        ArrayList<CollectorInp> collectors = new ArrayList<>();
        int min = 0;
        for (CollectorInp collector : inpCollectorsBin) {
            min++;
            if (collector.getEdges().size() == 0) {
                collectors.add(collector);
            }
            if (min >= getInpColLimitBin()[0]) {
                break;
            }
        }
        min = 0;
        for (CollectorInp collector : inpCollectorsInt) {
            min++;
            if (collector.getEdges().size() == 0) {
                collectors.add(collector);
            }
            if (min >= getInpColLimitInt()[0]) {
                break;
            }
        }
        min = 0;
        for (CollectorInp collector : inpCollectorsDec) {
            min++;
            if (collector.getEdges().size() == 0) {
                collectors.add(collector);
            }
            if (min >= getInpColLimitDec()[0]) {
                break;
            }
        }
        return collectors;
    }

    public ArrayList<CollectorInp> getInpCollectors() {
        return inpCollectors;
    }

    public ArrayList<CollectorInp> getInpCollectors(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) return inpCollectorsDec;
        if (type.isAssignableFrom(Integer.class)) return inpCollectorsInt;
        if (type.isAssignableFrom(Boolean.class)) return inpCollectorsBin;
        throw new Exception("No input collectors of type: " + type.getSimpleName());
    }

    public ArrayList<CollectorOut> getOutCollectors() {
        return outCollectors;
    }

    public ArrayList<CollectorOut> getOutCollectors(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) return outCollectorsDec;
        if (type.isAssignableFrom(Integer.class)) return outCollectorsInt;
        if (type.isAssignableFrom(Boolean.class)) return outCollectorsBin;
        throw new Exception("No output collectors of type: " + type.getSimpleName());
    }

    public ArrayList<CollectorInp> getInpCollectorsDec() {
        return inpCollectorsDec;
    }

    public ArrayList<CollectorInp> getInpCollectorsInt() {
        return inpCollectorsInt;
    }

    public ArrayList<CollectorInp> getInpCollectorsBin() {
        return inpCollectorsBin;
    }

    public ArrayList<CollectorOut> getOutCollectorsDec() {
        return outCollectorsDec;
    }

    public ArrayList<CollectorOut> getOutCollectorsInt() {
        return outCollectorsInt;
    }

    public ArrayList<CollectorOut> getOutCollectorsBin() {
        return outCollectorsBin;
    }

    public boolean isOutConnected() {
        for (CollectorOut collector : outCollectors) {
            if (collector.getEdges().size() > 0) {
                return true;
            }
        }
        return false;
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
    public void processNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    clcNode();
                    setSignalClcDone(true);
                    setState(1);
                }
                break;
            case 1:
                if (isSignalClcDone()) {
                    setSignalReady(true);
                    setState(2);
                }
                break;
            case 2:
                if (isSignalReady()) {
                    setState(3);
                }
                break;
            case 3:
                if (isSignalSend() || !isOutConnected()) {
                    setState(4);
                }

                break;
            case 4:
        }
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

    @Override
    public void clcNode() throws Exception {

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

