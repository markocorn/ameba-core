package ameba.core.blocks.nodes;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.*;
import ameba.core.blocks.edges.Edge;
import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Base node class.
 */
public class Node implements Serializable {
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

    private ArrayList<CollectorTarget> collectorsTargetCon;
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

    private boolean[] lockDec;
    private boolean[] lockInt;
    private boolean[] lockBin;

    private boolean lock;


    public Node(int[] inpColLimitDec, int[] inpColLimitInt, int[] inpColLimitBin, int[] outColLimitDec, int[] outColLimitInt, int[] outColLimitBin, int paramsDec, int paramsInt, int paramsBin) {
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

        collectorsTargetCon = new ArrayList<>();
        collectorsTargetConDec = new ArrayList<>();
        collectorsTargetConInt = new ArrayList<>();
        collectorsTargetConBin = new ArrayList<>();

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
        collectorsTargetConDec.clear();
        for (CollectorTargetDec c : collectorsTargetDec) {
            if (c.getEdges().size() > 0) {
                collectorsTargetConDec.add(c);
            }
        }
        return collectorsTargetConDec;
    }

    public ArrayList<CollectorTargetInt> getCollectorsTargetConnectedInt() {
        collectorsTargetConInt.clear();
        for (CollectorTargetInt collectorInp : collectorsTargetInt) {
            if (collectorInp.getEdges().size() > 0) {
                collectorsTargetConInt.add(collectorInp);
            }
        }
        return collectorsTargetConInt;
    }

    public ArrayList<CollectorTargetBin> getCollectorsTargetConnectedBin() {
        collectorsTargetConBin.clear();
        for (CollectorTargetBin collectorInp : collectorsTargetBin) {
            if (collectorInp.getEdges().size() > 0) {
                collectorsTargetConBin.add(collectorInp);
            }
        }
        return collectorsTargetConBin;
    }

    public void simOptimization() {
        //Performance optimization
        collectorsTargetCon.clear();
        collectorsTargetCon.addAll(getCollectorsTargetConnected());
    }

    //Optimisation calls
    public ArrayList<CollectorTargetDec> getCollectorsTargetConnectedDecSim() {
        return collectorsTargetConDec;
    }

    public ArrayList<CollectorTargetInt> getCollectorsTargetConnectedIntSim() {
        return collectorsTargetConInt;
    }

    public ArrayList<CollectorTargetBin> getCollectorsTargetConnectedBinSim() {
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
        ArrayList<CollectorTargetDec> collectorsFree = new ArrayList<>();

        for (CollectorTargetDec collector : collectorsTargetDec) {
            if (collector.getEdges().size() == 0) {
                collectorsFree.add(collector);
            } else {
                collectors.add(collector);
            }
        }
        int n = getCollectorTargetLimitsDec()[0] - collectors.size();
        if (n > 0 && collectorsFree.size() >= n) {
            return new ArrayList<CollectorTargetDec>(collectorsFree.subList(0, n));
        }
        return new ArrayList<>();
    }

    public ArrayList<CollectorTargetInt> getCollectorsTargetToConnectInt() {
        ArrayList<CollectorTargetInt> collectors = new ArrayList<>();
        ArrayList<CollectorTargetInt> collectorsFree = new ArrayList<>();

        for (CollectorTargetInt collector : collectorsTargetInt) {
            if (collector.getEdges().size() == 0) {
                collectorsFree.add(collector);
            } else {
                collectors.add(collector);
            }
        }
        int n = getCollectorTargetLimitsInt()[0] - collectors.size();
        if (n > 0 && collectorsFree.size() >= n) {
            return new ArrayList<CollectorTargetInt>(collectorsFree.subList(0, n));
        }
        return new ArrayList<>();
    }

    public ArrayList<CollectorTargetBin> getCollectorsTargetToConnectBin() {
        ArrayList<CollectorTargetBin> collectors = new ArrayList<>();
        ArrayList<CollectorTargetBin> collectorsFree = new ArrayList<>();

        for (CollectorTargetBin collector : collectorsTargetBin) {
            if (collector.getEdges().size() == 0) {
                collectorsFree.add(collector);
            } else {
                collectors.add(collector);
            }
        }
        int n = getCollectorTargetLimitsBin()[0] - collectors.size();
        if (n > 0 && collectorsFree.size() >= n) {
            return new ArrayList<CollectorTargetBin>(collectorsFree.subList(0, n));
        }
        return new ArrayList<>();
    }

    public ArrayList<? extends CollectorTarget> getCollectorsTargetFree() {
        ArrayList<CollectorTarget> list = new ArrayList<>();
        list.addAll(getCollectorsTargetFreeDec());
        list.addAll(getCollectorsTargetFreeInt());
        list.addAll(getCollectorsTargetFreeBin());
        return list;
    }

    public ArrayList<CollectorTargetDec> getCollectorsTargetFreeDec() {
        ArrayList<CollectorTargetDec> collectorsFree = new ArrayList<>();
        for (CollectorTargetDec collector : collectorsTargetDec) {
            if (collector.getEdges().size() == 0) {
                collectorsFree.add(collector);
            }
        }
        return collectorsFree;
    }

    public ArrayList<CollectorTargetInt> getCollectorsTargetFreeInt() {
        ArrayList<CollectorTargetInt> collectorsFree = new ArrayList<>();
        for (CollectorTargetInt collector : collectorsTargetInt) {
            if (collector.getEdges().size() == 0) {
                collectorsFree.add(collector);
            }
        }
        return collectorsFree;
    }

    public ArrayList<CollectorTargetBin> getCollectorsTargetFreeBin() {
        ArrayList<CollectorTargetBin> collectorsFree = new ArrayList<>();
        for (CollectorTargetBin collector : collectorsTargetBin) {
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
    }

    public void clearCollectors() {
        for (CollectorSourceBin c : getCollectorsSourceBin()) {
            c.setSignal(false);
        }
        for (CollectorSourceInt c : getCollectorsSourceInt()) {
            c.setSignal(0);
        }
        for (CollectorSourceDec c : getCollectorsSourceDec()) {
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

    public ArrayList<Cell.Signal> getParTypes() {
        ArrayList<Cell.Signal> types = new ArrayList<>();
        if (hasParDec()) types.add(Cell.Signal.DECIMAL);
        if (hasParInt()) types.add(Cell.Signal.INTEGER);
        if (hasParBin()) types.add(Cell.Signal.BOOLEAN);
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

    public boolean hasParamsUnlocked(Cell.Signal type) {
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
        for (Collector collector : collectorSources) {
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

