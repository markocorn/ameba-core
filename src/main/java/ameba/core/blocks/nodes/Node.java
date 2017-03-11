package ameba.core.blocks.nodes;

import ameba.core.blocks.collectors.*;
import ameba.core.blocks.edges.Edge;
import com.rits.cloning.Cloner;

import java.util.ArrayList;

/**
 * Base node class.
 */
public class Node implements Cloneable {
    /**
     * List of input collectors of the node.
     */
    private ArrayList<CollectorTarget> collectorsTarget;
    private ArrayList<CollectorSource> collectorSources;
    private ArrayList<CollectorTargetDec> collectorsTargetDec;
    private ArrayList<CollectorTargetInt> collectorsTargetInt;
    private ArrayList<CollectorTargetBin> collectorsTargetBin;
    private ArrayList<CollectorSourceDec> collectorsSourceDec;
    private ArrayList<CollectorSourceInt> collectorsSourceInt;
    private ArrayList<CollectorSourceBin> collectorsSourceBin;

    private int[] collectorTargetLimitsDec;
    private int[] collectorTargetLimitsInt;
    private int[] collectorTargetLimitsBin;

    private int[] collectorSourceLimitsDec;
    private int[] collectorSourceLimitsInt;
    private int[] collectorSourceLimitsBin;

    /**
     * Flag that indicates signal is reads to be sent to other ameba.core.blocks.nodes.
     */
    private boolean signalInputsReady;
    private boolean signalReady;
    private boolean signalClcDone;

    private int state;

    private double paramsDec;
    private double[] paramsLimitsDec;
    private int paramsInt;
    private int[] paramsLimitsInt;
    private boolean paramsBin;
    private boolean[] paramsLimitsBin;


    public Node(int[] inpColLimitDec, int[] inpColLimitInt, int[] inpColLimitBin, int[] outColLimitDec, int[] outColLimitInt, int[] outColLimitBin, double paramsDec, double[] paramsLimitsDec, int paramsInt, int[] paramsLimitsInt, boolean paramsBin, boolean[] paramsLimitsBin) {
        this.collectorTargetLimitsDec = inpColLimitDec;
        this.collectorTargetLimitsInt = inpColLimitInt;
        this.collectorTargetLimitsBin = inpColLimitBin;
        this.collectorSourceLimitsDec = outColLimitDec;
        this.collectorSourceLimitsInt = outColLimitInt;
        this.collectorSourceLimitsBin = outColLimitBin;
        this.paramsDec = paramsDec;
        this.paramsLimitsDec = paramsLimitsDec;
        this.paramsInt = paramsInt;
        this.paramsLimitsInt = paramsLimitsInt;
        this.paramsBin = paramsBin;
        this.paramsLimitsBin = paramsLimitsBin;

        collectorsTarget = new ArrayList<>();
        collectorSources = new ArrayList<>();
        collectorsTargetDec = new ArrayList<>();
        collectorsTargetInt = new ArrayList<>();
        collectorsTargetBin = new ArrayList<>();
        collectorsSourceDec = new ArrayList<>();
        collectorsSourceInt = new ArrayList<>();
        collectorsSourceBin = new ArrayList<>();

        signalReady = false;
        signalInputsReady = false;
        signalClcDone = false;
        state = 0;
    }

    public int[] getInpColLimit(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) return collectorTargetLimitsDec;
        if (type.isAssignableFrom(Integer.class)) return collectorTargetLimitsInt;
        if (type.isAssignableFrom(Boolean.class)) return collectorTargetLimitsBin;
        throw new Exception("No input collectors limits of type: " + type.getSimpleName());
    }

    public int[] getOutColLimit(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) return collectorSourceLimitsDec;
        if (type.isAssignableFrom(Integer.class)) return collectorSourceLimitsInt;
        if (type.isAssignableFrom(Boolean.class)) return collectorSourceLimitsBin;
        throw new Exception("No output collectors limits of type: " + type.getSimpleName());
    }

    public int[] getCollectorTargetLimitsDec() {
        return collectorTargetLimitsDec;
    }

    public int[] getCollectorTargetLimitsInt() {
        return collectorTargetLimitsInt;
    }

    public int[] getCollectorTargetLimitsBin() {
        return collectorTargetLimitsBin;
    }

    public int[] getCollectorSourceLimitsDec() {
        return collectorSourceLimitsDec;
    }

    public int[] getCollectorSourceLimitsInt() {
        return collectorSourceLimitsInt;
    }

    public int[] getCollectorSourceLimitsBin() {
        return collectorSourceLimitsBin;
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

    public void addCollectorTagetDec(CollectorTargetDec collector) throws Exception {
        if (collectorsTargetDec.size() < collectorTargetLimitsDec[1]) {
            collectorsTargetDec.add(collector);
            collectorsTarget.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
    }

    public void addCollectorTagetInt(CollectorTargetInt collector) throws Exception {
        if (collectorsTargetInt.size() < collectorTargetLimitsInt[1]) {
            collectorsTargetInt.add(collector);
            collectorsTarget.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
    }

    public void addCollectorTagetBin(CollectorTargetBin collector) throws Exception {
        if (collectorsTargetBin.size() < collectorTargetLimitsBin[1]) {
            collectorsTargetBin.add(collector);
            collectorsTarget.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
    }

    public void addCollectorSourceDec(CollectorSourceDec collector) throws Exception {
        if (collectorsSourceDec.size() < collectorSourceLimitsDec[1]) {
            collectorsSourceDec.add(collector);
            collectorSources.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum output collectors limitation of node: " + this.getClass().getSimpleName());
    }

    public void addCollectorSourceInt(CollectorSourceInt collector) throws Exception {
        if (collectorsSourceInt.size() < collectorSourceLimitsInt[1]) {
            collectorsSourceInt.add(collector);
            collectorSources.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum output collectors limitation of node: " + this.getClass().getSimpleName());
    }

    public void addCollectorSourceBin(CollectorSourceBin collector) throws Exception {
        if (collectorsSourceBin.size() < collectorSourceLimitsBin[1]) {
            collectorsSourceBin.add(collector);
            collectorSources.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum output collectors limitation of node: " + this.getClass().getSimpleName());
    }


    public ArrayList<CollectorTarget> getInpCollectorsConnected(Class type) {
        ArrayList<CollectorTarget> collectorInps = new ArrayList<>();
        if (type.isAssignableFrom(Double.class)) {
            for (CollectorTarget collectorInp : collectorsTargetDec) {
                if (collectorInp.getEdges().size() > 0) {
                    collectorInps.add(collectorInp);
                }
            }
        }
        if (type.isAssignableFrom(Integer.class)) {
            for (CollectorTarget collectorInp : collectorsTargetInt) {
                if (collectorInp.getEdges().size() > 0) {
                    collectorInps.add(collectorInp);
                }
            }
        }
        if (type.isAssignableFrom(Boolean.class)) {
            for (CollectorTarget collectorInp : collectorsTargetBin) {
                if (collectorInp.getEdges().size() > 0) {
                    collectorInps.add(collectorInp);
                }
            }

        }
        return collectorInps;
    }

    public ArrayList<CollectorTarget> getInpCollectorsConnected() {
        ArrayList<CollectorTarget> collectorInps = new ArrayList<>();
        for (CollectorTarget collectorInp : collectorsTarget) {
            if (collectorInp.getEdges().size() > 0) {
                collectorInps.add(collectorInp);
            }
        }
        return collectorInps;
    }

    public ArrayList<CollectorSource> getOutCollectorsConnected() {
        ArrayList<CollectorSource> collectorOuts = new ArrayList<>();
        for (CollectorSource collectorOut : collectorSources) {
            if (collectorOut.getEdges().size() > 0) {
                collectorOuts.add(collectorOut);
            }
        }
        return collectorOuts;
    }


    public ArrayList<CollectorSource> getOutCollectorsConnected(Class type) {
        ArrayList<CollectorSource> collectors = new ArrayList<>();
        if (type.isAssignableFrom(Double.class)) {
            for (CollectorSource collector : collectorsSourceDec) {
                if (collector.getEdges().size() > 0) {
                    collectors.add(collector);
                }
            }
        }
        if (type.isAssignableFrom(Integer.class)) {
            for (CollectorSource collector : collectorsSourceInt) {
                if (collector.getEdges().size() > 0) {
                    collectors.add(collector);
                }
            }
        }
        if (type.isAssignableFrom(Boolean.class)) {
            for (CollectorSource collector : collectorsSourceBin) {
                if (collector.getEdges().size() > 0) {
                    collectors.add(collector);
                }
            }
        }
        return collectors;
    }

    public ArrayList<CollectorTarget> getInpCollectorsMin(Class type) {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        int num = 0;
        if (type.isAssignableFrom(Double.class)) {
            num = getCollectorTargetLimitsDec()[0];
            for (CollectorTarget collector : collectorsTargetDec) {
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
            num = getCollectorTargetLimitsInt()[0];
            for (CollectorTarget collector : collectorsTargetInt) {
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
            num = getCollectorTargetLimitsBin()[0];
            for (CollectorTarget collector : collectorsTargetBin) {
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

    public ArrayList<CollectorTarget> getInpCollectorsMinConnect() {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        int min = 0;
        for (CollectorTarget collector : collectorsTargetBin) {
            min++;
            if (collector.getEdges().size() == 0) {
                collectors.add(collector);
            }
            if (min >= getCollectorTargetLimitsBin()[0]) {
                break;
            }
        }
        min = 0;
        for (CollectorTarget collector : collectorsTargetInt) {
            min++;
            if (collector.getEdges().size() == 0) {
                collectors.add(collector);
            }
            if (min >= getCollectorTargetLimitsInt()[0]) {
                break;
            }
        }
        min = 0;
        for (CollectorTarget collector : collectorsTargetDec) {
            min++;
            if (collector.getEdges().size() == 0) {
                collectors.add(collector);
            }
            if (min >= getCollectorTargetLimitsDec()[0]) {
                break;
            }
        }
        return collectors;
    }

    public ArrayList<CollectorTarget> getCollectorsTarget() {
        return collectorsTarget;
    }

    public ArrayList<CollectorTarget> getInpCollectors(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) return collectorsTargetDec;
        if (type.isAssignableFrom(Integer.class)) return collectorsTargetInt;
        if (type.isAssignableFrom(Boolean.class)) return collectorsTargetBin;
        throw new Exception("No input collectors of type: " + type.getSimpleName());
    }

    public ArrayList<CollectorSource> getCollectorSources() {
        return collectorSources;
    }

    public ArrayList<CollectorSource> getOutCollectors(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) return collectorsSourceDec;
        if (type.isAssignableFrom(Integer.class)) return collectorsSourceInt;
        if (type.isAssignableFrom(Boolean.class)) return collectorsSourceBin;
        throw new Exception("No output collectors of type: " + type.getSimpleName());
    }

    public ArrayList<CollectorTarget> getCollectorsTargetDec() {
        return collectorsTargetDec;
    }

    public ArrayList<CollectorTarget> getCollectorsTargetInt() {
        return collectorsTargetInt;
    }

    public ArrayList<CollectorTarget> getCollectorsTargetBin() {
        return collectorsTargetBin;
    }

    public ArrayList<CollectorSource> getCollectorsSourceDec() {
        return collectorsSourceDec;
    }

    public ArrayList<CollectorSource> getCollectorsSourceInt() {
        return collectorsSourceInt;
    }

    public ArrayList<CollectorSource> getCollectorsSourceBin() {
        return collectorsSourceBin;
    }

    public boolean isOutConnected() {
        for (CollectorSource collector : collectorSources) {
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
        for (CollectorTarget collectorInp : collectorsTarget) {
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

