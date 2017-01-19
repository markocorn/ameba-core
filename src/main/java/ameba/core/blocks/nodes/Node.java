package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.*;

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
    private ArrayList<ICollector<Double>> inpICollectorsDec;
    private ArrayList<ICollector<Integer>> inpICollectorsInt;
    private ArrayList<ICollector<Boolean>> inpICollectorsBin;
    private ArrayList<ICollector<Double>> outICollectorsDec;
    private ArrayList<ICollector<Integer>> outICollectorsInt;
    private ArrayList<ICollector<Boolean>> outICollectorsBin;

    /**
     * Mapped value of the node's signal.
     */
    private double signalDec;
    private int signalInt;
    private boolean signalBin;
    /**
     * Flag that indicates signal is reads to be sent to other ameba.core.blocks.nodes.
     */
    private boolean signalReady;
    /**
     * List of decimal paramsDec.
     */
    private ArrayList<Double> paramsDec;
    /**
     * List of integer paramsDec.
     */
    private ArrayList<Integer> paramsInt;
    /**
     * List of boolean paramsDec.
     */
    private ArrayList<Boolean> paramsBin;

    public Node() {
        inpCollectors = new ArrayList<>();
        outCollectors = new ArrayList<>();
        inpICollectorsDec = new ArrayList<>();
        inpICollectorsInt = new ArrayList<>();
        inpICollectorsBin = new ArrayList<>();
        outICollectorsDec = new ArrayList<>();
        outICollectorsInt = new ArrayList<>();
        outICollectorsBin = new ArrayList<>();

        signalDec = 0.0;
        signalInt = 0;
        signalBin = false;

        signalReady = false;

        paramsDec = new ArrayList<>();
        paramsInt = new ArrayList<>();
        paramsBin = new ArrayList<>();
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

    public <T> void addInpCollector(Class<T> tClass, CollectorInp collector) {
        inpCollectors.add(collector);
        if (tClass.isAssignableFrom(Double.class)) {
            inpICollectorsDec.add(collector);
        }
        if (tClass.isAssignableFrom(Integer.class)) {
            inpICollectorsInt.add(collector);
        }
        if (tClass.isAssignableFrom(Boolean.class)) {
            inpICollectorsBin.add(collector);
        }
    }

    public <T> void addOutCollector(Class<T> tClass, CollectorOut collector) {
        outCollectors.add(collector);
        if (tClass.isAssignableFrom(Double.class)) {
            outICollectorsDec.add(collector);
        }
        if (tClass.isAssignableFrom(Integer.class)) {
            outICollectorsInt.add(collector);
        }
        if (tClass.isAssignableFrom(Boolean.class)) {
            outICollectorsBin.add(collector);
        }
    }

    public ArrayList<CollectorInp> getInpCollectors() {
        return inpCollectors;
    }

    public ArrayList<CollectorOut> getOutCollectors() {
        return outCollectors;
    }

//    public ArrayList<Collector<Boolean>> getInpCollectorsBin() {
//        return inpCollectorsBin;
//    }
//
//    public ArrayList<Collector<Double>> getOutCollectorsDec() {
//        return outCollectorsDec;
//    }
//
//
//    public ArrayList<Collector<Integer>> getOutCollectorsInt() {
//        return outCollectorsInt;
//    }
//
//    public void setOutCollectorsInt(ArrayList<Collector<Integer>> outCollectorsInt) {
//        this.outCollectorsInt = outCollectorsInt;
//    }
//
//    public ArrayList<Collector<Boolean>> getOutCollectorsBin() {
//        return outCollectorsBin;
//    }
//
//    public void setOutCollectorsBin(ArrayList<Collector<Boolean>> outCollectorsBin) {
//        this.outCollectorsBin = outCollectorsBin;
//    }

    //    public void addInpEdge(int collector, Edge edge) throws Exception {
//        if (edge instanceof Edge<Double>){
//
//        }
//
//        inputCollectors.get(collector).addInpEdge(edge);
//    }
//
//    public void addOutEdge(int collector, Edge edge) throws Exception {
//        outputCollectors.get(collector).addOutEdge(edge);
//    }
//
//    public void removeInputEdge(Edge edge) throws Exception {
//        for (Collector collector : inputCollectors) {
//            collector.removeEdge(edge);
//        }
//    }
//
//    public void removeOutputEdge(Edge edge) throws Exception {
//        for (Collector collector : outputCollectors) {
//            collector.removeEdge(edge);
//        }
//    }

//    public ArrayList<Collector> getInputCollectors() {
//        return inputCollectors;
//    }
//
//    public ArrayList<Collector> getOutputCollectors() {
//        return outputCollectors;
//    }

    /**
     * Get decimal paramsDec of the node.
     *
     * @return decimal paramsDec of node.
     */
    public ArrayList<Double> getParamsDec() {
        return paramsDec;
    }

    /**
     * Set decimal paramsDec of the node.
     *
     * @param paramsDec
     */
    public void setParamsDec(ArrayList<Double> paramsDec) {
        this.paramsDec = paramsDec;
    }

    /**
     * Get decimal parameter of the node.
     *
     * @param ind Index of the parameter array.
     * @return decimal parameter of node.
     */
    public double getDecimalParameter(int ind) {
        return paramsDec.get(ind);
    }

    /**
     * Set decimal parameter of the node.
     *
     * @param ind Index of the parameter array.
     * @param par New value of the parameter.
     */
    public void setDecimalParameter(int ind, double par) {
        this.paramsDec.set(ind, par);
    }

    /**
     * Get integer paramsDec of the node.
     *
     * @return
     */
    public ArrayList<Integer> getParamsInt() {
        return paramsInt;
    }

    /**
     * Set integer paramsDec of the node.
     *
     * @param paramsInt
     */
    public void setParamsInt(ArrayList<Integer> paramsInt) {
        this.paramsInt = paramsInt;
    }

    /**
     * Get boolean paramsDec of the node.
     *
     * @return
     */
    public ArrayList<Boolean> getParamsBin() {
        return paramsBin;
    }

    /**
     * Set boolean paramsDec of the node.
     *
     * @param paramsBin
     */
    public void setParamsBin(ArrayList<Boolean> paramsBin) {
        this.paramsBin = paramsBin;
    }

    /**
     * Check if all nodes connected as source have signal ready for this node's output calculation.
     *
     * @return
     */
//    public boolean isInputSignalReady() {
//        for (Edge edge : inputEdges) {
//            if (!edge.isSignalReady()) {
//                return false;
//            }
//        }
//        return true;
//    }

    /**
     * Check if input connections didn't transfer their sources nodes output signal.
     *
     * @return
     */
    public boolean isSignalSend() {
        for (CollectorInp collectorInp : inpCollectors) {
            for (Edge edge : collectorInp.getEdges()) {
                if (edge.isSignalSend()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if node's inputs are ready for node calculation.
     * That includes this node's source nodes has to be signal ready and connecting connections must be transfer free (signal not send via them).
     *
     * @return
     */
    public boolean isNodeClcReady() {
        return isSignalReady() && isSignalSend();
    }


    public double getSignalDec() {
        return signalDec;
    }

    public void setSignalDec(double signalDec) {
        this.signalDec = signalDec;
    }

    public int getSignalInt() {
        return signalInt;
    }

    public void setSignalInt(int signalInt) {
        this.signalInt = signalInt;
    }

    public boolean getSignalBin() {
        return signalBin;
    }

    public void setSignalBin(boolean signalBin) {
        this.signalBin = signalBin;
    }

    public <T> T getSignal(Class<T> tClass) {
        if (tClass.isAssignableFrom(Double.class)) {
            return tClass.cast(signalDec);
        }
        if (tClass.isAssignableFrom(Integer.class)) {
            return tClass.cast(signalInt);
        }
        if (tClass.isAssignableFrom(Boolean.class)) {
            return tClass.cast(signalBin);
        }
        return null;
    }

    @Override
    public void clcNode() {

    }

    @Override
    public void clearNode() {
        rstNode();
        setSignalDec(0.0);
        setSignalInt(0);
        setSignalBin(false);
    }

    @Override
    public void rstNode() {

    }

    public <T> boolean hasInpCollector(Class<T> tClass) {
        if (tClass.isAssignableFrom(Double.class)) {
            if (inpICollectorsDec.size() > 0) return true;
            else return false;
        }
        if (tClass.isAssignableFrom(Integer.class)) {
            if (inpICollectorsInt.size() > 0) return true;
            else return false;
        }
        if (tClass.isAssignableFrom(Boolean.class)) {
            if (inpICollectorsBin.size() > 0) return true;
            else return false;
        }
        return false;
    }

    public <T> boolean hasOutCollector(Class<T> tClass) {
        if (tClass.isAssignableFrom(Double.class)) {
            if (outICollectorsDec.size() > 0) return true;
            else return false;
        }
        if (tClass.isAssignableFrom(Integer.class)) {
            if (outICollectorsInt.size() > 0) return true;
            else return false;
        }
        if (tClass.isAssignableFrom(Boolean.class)) {
            if (outICollectorsBin.size() > 0) return true;
            else return false;
        }
        return false;
    }

    public boolean hasAllEdgesSend() {
        for (Collector collector : inpCollectors) {
            for (Edge edge : collector.getEdges()) {
                if (!edge.isSignalSend()) {
                    return false;
                }
            }
        }
        return true;
    }
}

