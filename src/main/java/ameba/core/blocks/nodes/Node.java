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

    private boolean signalReady;
    private boolean signalClcDone;

    private double[] paramsDec;
    private double[][] paramsLimitsDec;
    private int[] paramsInt;
    private int[][] paramsLimitsInt;
    private boolean[] paramsBin;
    private boolean[][] paramsLimitsBin;


    public Node(int[] inpColLimitDec, int[] inpColLimitInt, int[] inpColLimitBin, int[] outColLimitDec, int[] outColLimitInt, int[] outColLimitBin) {
        this.collectorTargetLimitsDec = inpColLimitDec;
        this.collectorTargetLimitsInt = inpColLimitInt;
        this.collectorTargetLimitsBin = inpColLimitBin;
        this.collectorSourceLimitsDec = outColLimitDec;
        this.collectorSourceLimitsInt = outColLimitInt;
        this.collectorSourceLimitsBin = outColLimitBin;

        collectorsTarget = new ArrayList<>();
        collectorSources = new ArrayList<>();
        collectorsTargetDec = new ArrayList<>();
        collectorsTargetInt = new ArrayList<>();
        collectorsTargetBin = new ArrayList<>();
        collectorsSourceDec = new ArrayList<>();
        collectorsSourceInt = new ArrayList<>();
        collectorsSourceBin = new ArrayList<>();

        paramsDec = new double[]{0.0};
        paramsLimitsDec = new double[][]{{0.0}};
        paramsInt = new int[]{0};
        paramsLimitsInt = new int[][]{{1}};
        paramsBin = new boolean[]{false};
        paramsLimitsBin = new boolean[][]{{false}};

        signalReady = false;
        signalClcDone = false;
    }

    public int[] getCollectorTargetLimit(Class type) throws Exception {
        if (type.isAssignableFrom(Double.class)) return collectorTargetLimitsDec;
        if (type.isAssignableFrom(Integer.class)) return collectorTargetLimitsInt;
        if (type.isAssignableFrom(Boolean.class)) return collectorTargetLimitsBin;
        throw new Exception("No input collectors limits of type: " + type.getSimpleName());
    }

    public int[] getColSourceLimit(Class type) throws Exception {
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

    public void addCollectorTargetDec(CollectorTargetDec collector) throws Exception {
        if (collectorsTargetDec.size() < collectorTargetLimitsDec[1]) {
            collectorsTargetDec.add(collector);
            collectorsTarget.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
    }

    public void addCollectorTargetInt(CollectorTargetInt collector) throws Exception {
        if (collectorsTargetInt.size() < collectorTargetLimitsInt[1]) {
            collectorsTargetInt.add(collector);
            collectorsTarget.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
    }

    public void addCollectorTargetBin(CollectorTargetBin collector) throws Exception {
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

    public ArrayList<CollectorTarget> getCollectorsTargetConnected(Class type) {
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

    public ArrayList<CollectorTargetDec> getCollectorsTargetConnectedDec() {
        ArrayList<CollectorTargetDec> collectorInps = new ArrayList<>();
        for (CollectorTargetDec collectorInp : collectorsTargetDec) {
            if (collectorInp.getEdges().size() > 0) {
                collectorInps.add(collectorInp);
            }
        }
        return collectorInps;
    }

    public ArrayList<CollectorTargetInt> getCollectorsTargetConnectedInt() {
        ArrayList<CollectorTargetInt> collectorInps = new ArrayList<>();
        for (CollectorTargetInt collectorInp : collectorsTargetInt) {
            if (collectorInp.getEdges().size() > 0) {
                collectorInps.add(collectorInp);
            }
        }
        return collectorInps;
    }

    public ArrayList<CollectorTargetBin> getCollectorsTargetConnectedBin() {
        ArrayList<CollectorTargetBin> collectorInps = new ArrayList<>();
        for (CollectorTargetBin collectorInp : collectorsTargetBin) {
            if (collectorInp.getEdges().size() > 0) {
                collectorInps.add(collectorInp);
            }
        }
        return collectorInps;
    }

    public ArrayList<CollectorTarget> getCollectorsTargetConnected() {
        ArrayList<CollectorTarget> collectorInps = new ArrayList<>();
        for (CollectorTarget collectorInp : collectorsTarget) {
            if (collectorInp.getEdges().size() > 0) {
                collectorInps.add(collectorInp);
            }
        }
        return collectorInps;
    }

    public ArrayList<CollectorSource> getCollectorsSourceConnected() {
        ArrayList<CollectorSource> collectorOuts = new ArrayList<>();
        for (CollectorSource collectorOut : collectorSources) {
            if (collectorOut.getEdges().size() > 0) {
                collectorOuts.add(collectorOut);
            }
        }
        return collectorOuts;
    }

    public ArrayList<CollectorSource> getCollectorsSourceConnected(Class type) {
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

    public ArrayList<CollectorTarget> getCollectorsTargetMin(Class type) {
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

    public ArrayList<CollectorTarget> getCollectorsTargetMinConnect() {
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

    public ArrayList<CollectorSource> getCollectorSources() {
        return collectorSources;
    }

    public ArrayList<CollectorTargetDec> getCollectorsTargetDec() {
        return collectorsTargetDec;
    }

    public ArrayList<CollectorTargetInt> getCollectorsTargetInt() {
        return collectorsTargetInt;
    }

    public ArrayList<CollectorTargetBin> getCollectorsTargetBin() {
        return collectorsTargetBin;
    }

    public ArrayList<CollectorSourceDec> getCollectorsSourceDec() {
        return collectorsSourceDec;
    }

    public ArrayList<CollectorSourceInt> getCollectorsSourceInt() {
        return collectorsSourceInt;
    }

    public ArrayList<CollectorSourceBin> getCollectorsSourceBin() {
        return collectorsSourceBin;
    }

    public boolean isSourceConnected() {
        for (CollectorSource collector : collectorSources) {
            if (collector.getEdges().size() > 0) {
                return true;
            }
        }
        return false;
    }

    public double[] getParamsDec() {
        return paramsDec;
    }

    public void setParamsDec(double[] paramsDec) {
        this.paramsDec = paramsDec;
    }

    public double[][] getParamsLimitsDec() {
        return paramsLimitsDec;
    }

    public void setParamsLimitsDec(double[][] paramsLimitsDec) {
        this.paramsLimitsDec = paramsLimitsDec;
    }

    public int[] getParamsInt() {
        return paramsInt;
    }

    public void setParamsInt(int[] paramsInt) {
        this.paramsInt = paramsInt;
    }

    public int[][] getParamsLimitsInt() {
        return paramsLimitsInt;
    }

    public void setParamsLimitsInt(int[][] paramsLimitsInt) {
        this.paramsLimitsInt = paramsLimitsInt;
    }

    public boolean[] getParamsBin() {
        return paramsBin;
    }

    public void setParamsBin(boolean[] paramsBin) {
        this.paramsBin = paramsBin;
    }

    public boolean[][] getParamsLimitsBin() {
        return paramsLimitsBin;
    }

    public void setParamsLimitsBin(boolean[][] paramsLimitsBin) {
        this.paramsLimitsBin = paramsLimitsBin;
    }

    public void processNode() throws Exception {
        if (isSignalInputsReady()) {
            clcNode();
            setSignalClcDone(true);
            setSignalReady(true);
        }
    }

    public void clearNode() {
        rstNode();
    }

    public void rstNode() {
        signalReady = false;
        signalClcDone = false;
    }

    public void clcNode() throws Exception {

    }

    public boolean isSignalInputsReady() {
        for (CollectorTarget collectorInp : collectorsTarget) {
            for (Edge edge : collectorInp.getEdges()) {
                if (!edge.isSignalReady()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isSignalClcDone() {
        return signalClcDone;
    }

    public void setSignalClcDone(boolean signalClcDone) {
        this.signalClcDone = signalClcDone;
    }


    public Node clone() {
        Cloner cloner = new Cloner();
        return cloner.deepClone(this);
    }
}

