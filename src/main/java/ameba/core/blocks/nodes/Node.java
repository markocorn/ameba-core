package ameba.core.blocks.nodes;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.Collector;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Base node class.
 */
strictfp public class Node implements Serializable {
    /**
     * List of input collectors of the node.
     */
    private ArrayList<CollectorTarget> collectorsTarget;
    private ArrayList<CollectorSource> collectorsSource;

    private ArrayList<CollectorTarget> collectorsTargetCon;

    private int[] collectorTargetLimits;

    private int[] collectorSourceLimits;

    private boolean signalReady;
    private boolean signalClcDone;

    private ArrayList<Double> paramsDec;
    private ArrayList<Double[]> paramsLimitsDec;
    private ArrayList<Integer> paramsInt;
    private ArrayList<Integer[]> paramsLimitsInt;
    private ArrayList<Boolean> paramsBin;
    private ArrayList<Boolean[]> paramsLimitsBin;

    private boolean[] lockDec;
    private boolean[] lockInt;
    private boolean[] lockBin;

    private boolean lock;


    public Node(int[] inpColLimit, int[] outColLimit, int paramsDec, int paramsInt, int paramsBin) {
        this.collectorTargetLimits = inpColLimit;
        this.collectorSourceLimits = outColLimit;

        collectorsTarget = new ArrayList<>();
        collectorsSource = new ArrayList<>();


        collectorsTargetCon = new ArrayList<>();

        this.paramsDec = new ArrayList<>(paramsDec);
        this.paramsLimitsDec = new ArrayList<>(paramsDec);
        this.paramsInt = new ArrayList<>(paramsInt);
        this.paramsLimitsInt = new ArrayList<>(paramsInt);
        this.paramsBin = new ArrayList<>(paramsBin);
        this.paramsLimitsBin = new ArrayList<>(paramsBin);

        lockDec = new boolean[paramsDec];
        Arrays.fill(lockDec, false);

        lockInt = new boolean[paramsInt];
        Arrays.fill(lockInt, false);

        lockBin = new boolean[paramsBin];
        Arrays.fill(lockBin, false);

        lock = false;

        signalReady = false;
        signalClcDone = false;
    }


    public int[] getCollectorTargetLimit() {
        return collectorTargetLimits;
    }

    public int[] getCollectorSourceLimit() {
        return collectorSourceLimits;
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


    public void addCollectorTarget(CollectorTarget collector) throws Exception {
        if (collectorsTarget.size() < collectorTargetLimits[1]) {
            collectorsTarget.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum input collectors limitation of node: " + this.getClass().getSimpleName());
    }

    public void addCollectorSource(CollectorSource collector) throws Exception {
        if (collectorsSource.size() < collectorSourceLimits[1]) {
            collectorsSource.add(collector);
        } else
            throw new Exception("Collector can't be added. Maximum output collectors limitation of node: " + this.getClass().getSimpleName());
    }


    public void simOptimization() {
        //Performance optimization
        clcCollectorsTargetConnected();
    }

    public ArrayList<CollectorTarget> clcCollectorsTargetConnected() {
        collectorsTargetCon.clear();
        for (CollectorTarget c : collectorsTarget) {
            if (c.getEdges().size() > 0) {
                collectorsTargetCon.add(c);
            }
        }
        return collectorsTargetCon;
    }

    public ArrayList<CollectorTarget> getCollectorsTargetConnected() {
        return collectorsTargetCon;
    }

    public ArrayList<CollectorTarget> getCollectorsTargetToConnect() {
        ArrayList<CollectorTarget> collectors = new ArrayList<>();
        ArrayList<CollectorTarget> collectorsFree = new ArrayList<>();

        for (CollectorTarget collector : collectorsTarget) {
            if (collector.getEdges().size() == 0) {
                collectorsFree.add(collector);
            } else {
                collectors.add(collector);
            }
        }
        int n = getCollectorTargetLimit()[0] - collectors.size();
        if (n > 0 && collectorsFree.size() >= n) {
            return new ArrayList<CollectorTarget>(collectorsFree.subList(0, n));
        }
        return new ArrayList<>();
    }

    public ArrayList<CollectorTarget> getCollectorsTargetFree() {
        ArrayList<CollectorTarget> collectorsFree = new ArrayList<>();
        for (CollectorTarget collector : collectorsTarget) {
            if (collector.getEdges().size() == 0) {
                collectorsFree.add(collector);
            }
        }
        return collectorsFree;
    }

    public ArrayList<CollectorTarget> getCollectorsTarget() {
        return collectorsTarget;
    }

    public void setCollectorsTarget(ArrayList<CollectorTarget> collectorsTarget) {
        this.collectorsTarget = collectorsTarget;
    }


    public ArrayList<CollectorSource> getCollectorsSource() {
        return collectorsSource;
    }

    public void setCollectorSources(ArrayList<CollectorSource> collectorSources) {
        this.collectorsSource = collectorSources;
    }

    public ArrayList<CollectorSource> getCollectorsSourceConnected() {
        ArrayList<CollectorSource> collectors = new ArrayList<>();
        for (CollectorSource collector : collectorsSource) {
            if (collector.getEdges().size() > 0) {
                collectors.add(collector);
            }
        }
        return collectors;
    }

    public boolean isSourceConnected() {
        for (CollectorSource collector : collectorsSource) {
            if (collector.getEdges().size() > 0) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Double> getParamsDec() {
        return paramsDec;
    }

    public ArrayList<Double> getParamsUnlockedDec() {
        ArrayList<Double> pars = new ArrayList<>();
        for (int i = 0; i < paramsDec.size(); i++) {
            if (!lockDec[i]) {
                pars.add(paramsDec.get(i));
            }
        }
        return pars;
    }

    public void setParamUnlockedDec(int ind, Double paramDec) {
        ArrayList<Double> unlocked = new ArrayList<>();
        for (int i = 0; i < paramsDec.size(); i++) {
            if (!lockDec[i]) {
                unlocked.add(paramsDec.get(i));
            }
        }
        unlocked.set(ind, paramDec);
        if (paramDec < paramsLimitsDec.get(ind)[0]) paramDec = paramsLimitsDec.get(ind)[0];
        if (paramDec > paramsLimitsDec.get(ind)[1]) paramDec = paramsLimitsDec.get(ind)[1];
        this.paramsDec.set(ind, paramDec);
    }

    public void setParamsDec(ArrayList<Double> paramsDec) {
        this.paramsDec = paramsDec;
    }

    public void setParamDec(int ind, Double paramDec) {
        if (paramDec < paramsLimitsDec.get(ind)[0]) paramDec = paramsLimitsDec.get(ind)[0];
        if (paramDec > paramsLimitsDec.get(ind)[1]) paramDec = paramsLimitsDec.get(ind)[1];
        this.paramsDec.set(ind, paramDec);
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

    public ArrayList<Integer> getParamsUnlockedInt() {
        ArrayList<Integer> pars = new ArrayList<>();
        for (int i = 0; i < paramsInt.size(); i++) {
            if (!lockInt[i]) {
                pars.add(paramsInt.get(i));
            }
        }
        return pars;
    }

    public void setParamsInt(ArrayList<Integer> paramsInt) {
        this.paramsInt = paramsInt;
    }

    public void setParamUnlockedInt(int ind, Integer paramInt) {
        ArrayList<Integer> unlocked = new ArrayList<>();
        for (int i = 0; i < paramsInt.size(); i++) {
            if (!lockInt[i]) {
                unlocked.add(paramsInt.get(i));
            }
        }
        unlocked.set(ind, paramInt);
        if (paramInt < paramsLimitsInt.get(ind)[0]) paramInt = paramsLimitsInt.get(ind)[0];
        if (paramInt > paramsLimitsInt.get(ind)[1]) paramInt = paramsLimitsInt.get(ind)[1];
        this.paramsInt.set(ind, paramInt);
    }

    public void setParamInt(int ind, Integer paramInt) {
        if (paramInt < paramsLimitsInt.get(ind)[0]) paramInt = paramsLimitsInt.get(ind)[0];
        if (paramInt > paramsLimitsInt.get(ind)[1]) paramInt = paramsLimitsInt.get(ind)[1];
        this.paramsInt.set(ind, paramInt);
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

    public ArrayList<Boolean> getParamsUnlockedBin() {
        ArrayList<Boolean> pars = new ArrayList<>();
        for (int i = 0; i < paramsBin.size(); i++) {
            if (!lockBin[i]) {
                pars.add(paramsBin.get(i));
            }
        }
        return pars;
    }

    public void setParamsBin(ArrayList<Boolean> paramsBin) {
        this.paramsBin = paramsBin;
    }

    public void setParamUnlockedBin(int ind, Boolean paramBin) {
        ArrayList<Boolean> unlocked = new ArrayList<>();
        for (int i = 0; i < paramsBin.size(); i++) {
            if (!lockBin[i]) {
                unlocked.add(paramsBin.get(i));
            }
        }
        unlocked.set(ind, paramBin);
        if (!paramsLimitsBin.get(ind)[1]) paramBin = false;
        if (paramsLimitsBin.get(ind)[0]) paramBin = true;
        this.paramsBin.set(ind, paramBin);
    }

    public void setParamBin(int ind, Boolean paramBin) {
        if (!paramsLimitsBin.get(ind)[1]) paramBin = false;
        if (paramsLimitsBin.get(ind)[0]) paramBin = true;
        this.paramsBin.set(ind, paramBin);
    }

    public ArrayList<Boolean[]> getParamsLimitsBin() {
        return paramsLimitsBin;
    }

    public void setParamsLimitsBin(ArrayList<Boolean[]> paramsLimitsBin) {
        this.paramsLimitsBin = paramsLimitsBin;
    }

    public void processNode() {
        if (!isSignalReady()) {//Optimisation process don't verify node clc if node has already been calculated
            if (isSignalInputsReady()) {
                clcNode();
                setSignalClcDone(true);
                setSignalReady(true);
            }
        }
    }

    public void clearNode() {
        rstNode();
        clcCollectorsTargetConnected();
    }

    public void clearCollectors() {
        for (CollectorSource c : getCollectorsSource()) {
            c.setSignal(0.0);
        }
    }

    public void rstNode() {
        signalReady = false;
        signalClcDone = false;
    }

    public void clcNode() {

    }

    public boolean isSignalInputsReady() {
        for (CollectorTarget collectorInp : collectorsTargetCon) {
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
        return (Node) SerializationUtils.clone(this);
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

    public ArrayList<Cell.ParType> getParTypes() {
        ArrayList<Cell.ParType> types = new ArrayList<>();
        if (hasParDec()) types.add(Cell.ParType.DECIMAL);
        if (hasParInt()) types.add(Cell.ParType.INTEGER);
        if (hasParBin()) types.add(Cell.ParType.BOOLEAN);
        return types;
    }

    public boolean[] getLockDec() {
        return lockDec;
    }

    public void setLockDec(int ind, boolean lockDec) {
        this.lockDec[ind] = lockDec;
    }

    public boolean[] getLockInt() {
        return lockInt;
    }

    public void setLockInt(int ind, boolean lockInt) {
        this.lockInt[ind] = lockInt;
    }

    public boolean[] getLockBin() {
        return lockBin;
    }

    public void setLockBin(int ind, boolean lockBin) {
        this.lockBin[ind] = lockBin;
    }

    public boolean hasParamsUnlocked(Cell.ParType type) {
        switch (type) {
            case DECIMAL:
                return hasParamsDecUnlocked();
            case INTEGER:
                return hasParamsIntUnlocked();
            case BOOLEAN:
                return hasParamsBinUnlocked();
        }
        return false;
    }

    public boolean hasParamsDecUnlocked() {
        for (int i = 0; i < lockDec.length; i++) {
            if (!lockDec[i]) {
                return true;
            }
        }
        return false;
    }

    public boolean hasParamsIntUnlocked() {
        for (int i = 0; i < lockInt.length; i++) {
            if (!lockInt[i]) {
                return true;
            }
        }
        return false;
    }

    public boolean hasParamsBinUnlocked() {
        for (int i = 0; i < lockBin.length; i++) {
            if (!lockBin[i]) {
                return true;
            }
        }
        return false;
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    public boolean hasLockedEdgesSource() {
        for (Collector collector : collectorsSource) {
            for (Edge edge : collector.getEdges()) {
                if (edge.isLockSource()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasLockedEdgesTarget() {
        for (Collector collector : collectorsTarget) {
            for (Edge edge : collector.getEdges()) {
                if (edge.isLockTarget()) {
                    return true;
                }
            }
        }
        return false;
    }
}

