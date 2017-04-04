package ameba.core.blocks.nodes;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.*;
import ameba.core.blocks.edges.Edge;
import com.rits.cloning.Cloner;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Base node class.
 */
public class Node implements Cloneable, Serializable {
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

    private ArrayList<CollectorTargetDec> collectorsTargetConDec;
    private ArrayList<CollectorTargetInt> collectorsTargetConInt;
    private ArrayList<CollectorTargetBin> collectorsTargetConBin;

    private int[] collectorTargetLimitsDec;
    private int[] collectorTargetLimitsInt;
    private int[] collectorTargetLimitsBin;

    private int[] collectorSourceLimitsDec;
    private int[] collectorSourceLimitsInt;
    private int[] collectorSourceLimitsBin;

    private boolean signalReady;
    private boolean signalClcDone;

    private ArrayList<Double> paramsDec;
    private ArrayList<Double[]> paramsLimitsDec;
    private ArrayList<Integer> paramsInt;
    private ArrayList<Integer[]> paramsLimitsInt;
    private ArrayList<Boolean> paramsBin;
    private ArrayList<Boolean[]> paramsLimitsBin;


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

        collectorsTargetConDec = new ArrayList<>();
        collectorsTargetConInt = new ArrayList<>();
        collectorsTargetConBin = new ArrayList<>();

        paramsDec = new ArrayList<>();
        paramsLimitsDec = new ArrayList<>();
        paramsInt = new ArrayList<>();
        paramsLimitsInt = new ArrayList<>();
        paramsBin = new ArrayList<>();
        paramsLimitsBin = new ArrayList<>();

        signalReady = false;
        signalClcDone = false;
    }

    //For optimization purposes
    public void loadConnectedEdges() {
        collectorsTargetConDec.clear();
        for (CollectorTargetDec collectorInp : collectorsTargetDec) {
            if (collectorInp.getEdges().size() > 0) {
                collectorsTargetConDec.add(collectorInp);
            }
        }
        collectorsTargetConInt.clear();
        for (CollectorTargetInt collectorInp : collectorsTargetInt) {
            if (collectorInp.getEdges().size() > 0) {
                collectorsTargetConInt.add(collectorInp);
            }
        }
        collectorsTargetConBin.clear();
        for (CollectorTargetBin collectorInp : collectorsTargetBin) {
            if (collectorInp.getEdges().size() > 0) {
                collectorsTargetConBin.add(collectorInp);
            }
        }
    }

    public int[] getCollectorTargetLimit(Cell.Signal type) {
        switch (type) {
            case DECIMAL:
                return getCollectorTargetLimitsDec();
            case INTEGER:
                return getCollectorTargetLimitsInt();
            case BOOLEAN:
                return getCollectorTargetLimitsBin();
        }
        return null;
    }

    public int[] getCollectorSourceLimit(Cell.Signal type) {
        switch (type) {
            case DECIMAL:
                return getCollectorSourceLimitsDec();
            case INTEGER:
                return getCollectorSourceLimitsInt();
            case BOOLEAN:
                return getCollectorSourceLimitsBin();
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

    public ArrayList<CollectorTarget> getCollectorsTargetConnected() {
        ArrayList<CollectorTarget> collectorInps = new ArrayList<>();
        collectorInps.addAll(getCollectorsTargetConnectedDec());
        collectorInps.addAll(getCollectorsTargetConnectedInt());
        collectorInps.addAll(getCollectorsTargetConnectedBin());
        return collectorInps;
    }

    public ArrayList<? extends CollectorTarget> getCollectorsTargetConnected(Cell.Signal type) {
        switch (type) {
            case DECIMAL:
                return getCollectorsTargetConnectedDec();
            case INTEGER:
                return getCollectorsTargetConnectedInt();
            case BOOLEAN:
                return getCollectorsTargetConnectedBin();
        }
        return new ArrayList<>();
    }

    public ArrayList<CollectorTargetDec> getCollectorsTargetConnectedDec() {
        return collectorsTargetConDec;
    }

    public ArrayList<CollectorTargetInt> getCollectorsTargetConnectedInt() {
        return collectorsTargetConInt;
    }

    public ArrayList<CollectorTargetBin> getCollectorsTargetConnectedBin() {
        return collectorsTargetConBin;
    }

    public ArrayList<CollectorSource> getCollectorsSourceConnected() {
        ArrayList<CollectorSource> collectorOuts = new ArrayList<>();
        collectorOuts.addAll(getCollectorsSourceConnectedDec());
        collectorOuts.addAll(getCollectorsSourceConnectedInt());
        collectorOuts.addAll(getCollectorsSourceConnectedBin());

        return collectorOuts;
    }

    public ArrayList<? extends CollectorSource> getCollectorsSourceConnected(Cell.Signal type) {
        switch (type) {
            case DECIMAL:
                return getCollectorsSourceConnectedDec();
            case INTEGER:
                return getCollectorsSourceConnectedInt();
            case BOOLEAN:
                return getCollectorsSourceConnectedBin();
        }
        return new ArrayList<>();
    }

    public ArrayList<CollectorSourceDec> getCollectorsSourceConnectedDec() {
        ArrayList<CollectorSourceDec> collectors = new ArrayList<>();
        for (CollectorSourceDec collector : collectorsSourceDec) {
            if (collector.getEdges().size() > 0) {
                collectors.add(collector);
            }
        }
        return collectors;
    }

    public ArrayList<CollectorSourceInt> getCollectorsSourceConnectedInt() {
        ArrayList<CollectorSourceInt> collectors = new ArrayList<>();
        for (CollectorSourceInt collector : collectorsSourceInt) {
            if (collector.getEdges().size() > 0) {
                collectors.add(collector);
            }
        }
        return collectors;
    }

    public ArrayList<CollectorSourceBin> getCollectorsSourceConnectedBin() {
        ArrayList<CollectorSourceBin> collectors = new ArrayList<>();
        for (CollectorSourceBin collector : collectorsSourceBin) {
            if (collector.getEdges().size() > 0) {
                collectors.add(collector);
            }
        }
        return collectors;
    }

    public ArrayList<? extends CollectorTarget> getCollectorsTargetToConnect() {
        ArrayList<CollectorTarget> list = new ArrayList<>();
        list.addAll(getCollectorsTargetToConnectDec());
        list.addAll(getCollectorsTargetToConnectInt());
        list.addAll(getCollectorsTargetToConnectBin());
        return list;
    }

    public ArrayList<? extends CollectorTarget> getCollectorsTargetToConnect(Cell.Signal type) {
        if (type == Cell.Signal.DECIMAL) {
            return getCollectorsTargetToConnectDec();
        }
        if (type == Cell.Signal.INTEGER) {
            return getCollectorsTargetToConnectInt();
        }
        if (type == Cell.Signal.BOOLEAN) {
            return getCollectorsTargetToConnectBin();
        }
        return new ArrayList<>();
    }

    public ArrayList<CollectorTargetDec> getCollectorsTargetToConnectDec() {
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

    public ArrayList<CollectorTargetInt> getCollectorsTargetToConnectInt() {
        ArrayList<CollectorTargetInt> collectors = new ArrayList<>();
        int min = 0;
        for (CollectorTargetInt collector : collectorsTargetInt) {
            min++;
            if (collector.getEdges().size() == 0) {
                collectors.add(collector);
            }
            if (min >= getCollectorTargetLimitsInt()[0]) {
                break;
            }
        }
        return collectors;
    }

    public ArrayList<CollectorTargetBin> getCollectorsTargetToConnectBin() {
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

    public boolean isSourceConnected() {
        for (CollectorSource collector : collectorSources) {
            if (collector.getEdges().size() > 0) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Double> getParamsDec() {
        return paramsDec;
    }

    public void setParamsDec(ArrayList<Double> paramsDec) {
        this.paramsDec = paramsDec;
    }

    public ArrayList<Double[]> getParamsLimitsDec() {
        return paramsLimitsDec;
    }

    public void setParamsLimitsDec(ArrayList<Double[]> paramsLimitsDec) {
        this.paramsLimitsDec = paramsLimitsDec;
    }

    public ArrayList<Integer> getParamsInt() {
        return paramsInt;
    }

    public void setParamsInt(ArrayList<Integer> paramsInt) {
        this.paramsInt = paramsInt;
    }

    public ArrayList<Integer[]> getParamsLimitsInt() {
        return paramsLimitsInt;
    }

    public void setParamsLimitsInt(ArrayList<Integer[]> paramsLimitsInt) {
        this.paramsLimitsInt = paramsLimitsInt;
    }

    public ArrayList<Boolean> getParamsBin() {
        return paramsBin;
    }

    public void setParamsBin(ArrayList<Boolean> paramsBin) {
        this.paramsBin = paramsBin;
    }

    public ArrayList<Boolean[]> getParamsLimitsBin() {
        return paramsLimitsBin;
    }

    public void setParamsLimitsBin(ArrayList<Boolean[]> paramsLimitsBin) {
        this.paramsLimitsBin = paramsLimitsBin;
    }

    public void processNode() {
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

    public void clcNode() {

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

    public boolean hasParDec() {
        if (paramsDec.size() > 0) return true;
        return false;
    }

    public boolean hasParInt() {
        if (paramsInt.size() > 0) return true;
        return false;
    }

    public boolean hasParBin() {
        if (paramsBin.size() > 0) return true;
        return false;
    }

    public ArrayList<Cell.Signal> getParTypes() {
        ArrayList<Cell.Signal> types = new ArrayList<>();
        if (hasParDec()) types.add(Cell.Signal.DECIMAL);
        if (hasParInt()) types.add(Cell.Signal.INTEGER);
        if (hasParBin()) types.add(Cell.Signal.BOOLEAN);
        return types;
    }
}

