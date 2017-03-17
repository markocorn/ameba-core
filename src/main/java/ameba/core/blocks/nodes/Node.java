package ameba.core.blocks.nodes;

import ameba.core.blocks.Cell;
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

    public int[] getCollectorTargetLimit(Cell.Signal type) {
        switch (type) {
            case DECIMAL:
                return collectorTargetLimitsDec;
            case INTEGER:
                return collectorTargetLimitsInt;
            case BOOLEAN:
                return collectorTargetLimitsBin;
        }
        return null;
    }

    public int[] getColSourceLimit(Cell.Signal type) throws Exception {
        switch (type) {
            case DECIMAL:
                return collectorSourceLimitsDec;
            case INTEGER:
                return collectorSourceLimitsInt;
            case BOOLEAN:
                return collectorSourceLimitsBin;
        }
        return null;
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

    public ArrayList<CollectorTarget> getCollectorsTargetConnected(Cell.Signal type) {
        ArrayList<CollectorTarget> collectorInps = new ArrayList<>();
        switch (type) {
            case DECIMAL:
                for (CollectorTarget collectorInp : collectorsTargetDec) {
                    if (collectorInp.getEdges().size() > 0) {
                        collectorInps.add(collectorInp);
                    }
                }
                break;
            case INTEGER:
                for (CollectorTarget collectorInp : collectorsTargetInt) {
                    if (collectorInp.getEdges().size() > 0) {
                        collectorInps.add(collectorInp);
                    }
                }
                break;
            case BOOLEAN:
                for (CollectorTarget collectorInp : collectorsTargetBin) {
                    if (collectorInp.getEdges().size() > 0) {
                        collectorInps.add(collectorInp);
                    }
                }
                break;
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

    public ArrayList<CollectorSource> getCollectorsSourceConnected(Cell.Signal type) {
        ArrayList<CollectorSource> collectors = new ArrayList<>();
        switch (type) {
            case DECIMAL:
                for (CollectorSource collector : collectorsSourceDec) {
                    if (collector.getEdges().size() > 0) {
                        collectors.add(collector);
                    }
                }
                break;
            case INTEGER:
                for (CollectorSource collector : collectorsSourceInt) {
                    if (collector.getEdges().size() > 0) {
                        collectors.add(collector);
                    }
                }
                break;
            case BOOLEAN:
                for (CollectorSource collector : collectorsSourceBin) {
                    if (collector.getEdges().size() > 0) {
                        collectors.add(collector);
                    }
                }
                break;
        }
        return collectors;
    }


    public ArrayList<CollectorTarget> getCollectorsTargetMin(Cell.Signal type) {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        int num;
        if (type == Cell.Signal.DECIMAL) {
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
        if (type == Cell.Signal.INTEGER) {
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
        if (type == Cell.Signal.BOOLEAN) {
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

    public ArrayList<CollectorTargetDec> getCollectorsTargetMinConnectDec() {
        ArrayList<CollectorTargetDec> collectors = new ArrayList<>();
        int min = 0;
        for (CollectorTargetDec collector : collectorsTargetDec) {
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

    public ArrayList<CollectorTargetInt> getCollectorsTargetMinConnectInt() {
        ArrayList<CollectorTargetInt> collectors = new ArrayList<>();
        int min = 0;
        for (CollectorTargetInt collector : collectorsTargetInt) {
            min++;
            if (collector.getEdges().size() == 0) {
                collectors.add(collector);
            }
            if (min >= getCollectorTargetLimitsBin()[0]) {
                break;
            }
        }
        return collectors;
    }

    public ArrayList<CollectorTarget> getCollectorsTarget() {
        return collectorsTarget;
    }

    public void setCollectorsTarget(ArrayList<CollectorTarget> collectorsTarget) {
        this.collectorsTarget = collectorsTarget;
    }

    public ArrayList<? extends CollectorTarget> getCollectorsTarget(Cell.Signal type) {
        if (type == Cell.Signal.BOOLEAN) return collectorsTargetBin;
        if (type == Cell.Signal.INTEGER) return collectorsTargetInt;
        if (type == Cell.Signal.DECIMAL) return collectorsTargetDec;
        return new ArrayList<>();
    }

    public ArrayList<CollectorSource> getCollectorsSource() {
        return collectorSources;
    }

    public void setCollectorSources(ArrayList<CollectorSource> collectorSources) {
        this.collectorSources = collectorSources;
    }

    public ArrayList<? extends CollectorSource> getCollectorsSource(Cell.Signal type) {
        if (type == Cell.Signal.BOOLEAN) return collectorsSourceBin;
        if (type == Cell.Signal.INTEGER) return collectorsSourceInt;
        if (type == Cell.Signal.DECIMAL) return collectorsSourceDec;
        return new ArrayList<>();
    }

    public ArrayList<CollectorTargetDec> getCollectorsTargetDec() {
        return collectorsTargetDec;
    }

    public void setCollectorsTargetDec(ArrayList<CollectorTargetDec> collectorsTargetDec) {
        this.collectorsTargetDec = collectorsTargetDec;
    }

    public ArrayList<CollectorTargetInt> getCollectorsTargetInt() {
        return collectorsTargetInt;
    }

    public void setCollectorsTargetInt(ArrayList<CollectorTargetInt> collectorsTargetInt) {
        this.collectorsTargetInt = collectorsTargetInt;
    }

    public ArrayList<CollectorTargetBin> getCollectorsTargetBin() {
        return collectorsTargetBin;
    }

    public void setCollectorsTargetBin(ArrayList<CollectorTargetBin> collectorsTargetBin) {
        this.collectorsTargetBin = collectorsTargetBin;
    }

    public ArrayList<CollectorSourceDec> getCollectorsSourceDec() {
        return collectorsSourceDec;
    }

    public void setCollectorsSourceDec(ArrayList<CollectorSourceDec> collectorsSourceDec) {
        this.collectorsSourceDec = collectorsSourceDec;
    }

    public ArrayList<CollectorSourceInt> getCollectorsSourceInt() {
        return collectorsSourceInt;
    }

    public void setCollectorsSourceInt(ArrayList<CollectorSourceInt> collectorsSourceInt) {
        this.collectorsSourceInt = collectorsSourceInt;
    }

    public ArrayList<CollectorSourceBin> getCollectorsSourceBin() {
        return collectorsSourceBin;
    }

    public void setCollectorsSourceBin(ArrayList<CollectorSourceBin> collectorsSourceBin) {
        this.collectorsSourceBin = collectorsSourceBin;
    }

    public ArrayList<CollectorTargetBin> getCollectorsTargetMinConnectBin() {
        ArrayList<CollectorTargetBin> collectors = new ArrayList<>();
        int min = 0;
        for (CollectorTargetBin collector : collectorsTargetBin) {
            min++;
            if (collector.getEdges().size() == 0) {
                collectors.add(collector);
            }
            if (min >= getCollectorTargetLimitsBin()[0]) {
                break;
            }
        }
        return collectors;
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

