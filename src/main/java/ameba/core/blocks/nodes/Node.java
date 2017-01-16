package ameba.core.blocks.nodes;

import ameba.core.blocks.Collector;

import java.util.ArrayList;

/**
 * Base node class.
 */
public class Node implements Cloneable {
    /**
     * List of input collectors of the node.
     */
    private ArrayList<Collector<Double>> inpCollectorsDec;
    private ArrayList<Collector<Integer>> inpCollectorsInt;
    private ArrayList<Collector<Boolean>> inpCollectorsBin;
    private ArrayList<Collector<Double>> outCollectorsDec;
    private ArrayList<Collector<Integer>> outCollectorsInt;
    private ArrayList<Collector<Boolean>> outCollectorsBin;

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
     * List of decimal parameters.
     */
    private ArrayList<Double> decimalParameters;
    /**
     * List of integer parameters.
     */
    private ArrayList<Integer> integerParameters;
    /**
     * List of boolean parameters.
     */
    private ArrayList<Boolean> booleanParameters;

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

    public ArrayList<Collector<Double>> getInpCollectorsDec() {
        return inpCollectorsDec;
    }

    public void setInpCollectorsDec(ArrayList<Collector<Double>> inpCollectorsDec) {
        this.inpCollectorsDec = inpCollectorsDec;
    }

    public ArrayList<Collector<Integer>> getInpCollectorsInt() {
        return inpCollectorsInt;
    }

    public void setInpCollectorsInt(ArrayList<Collector<Integer>> inpCollectorsInt) {
        this.inpCollectorsInt = inpCollectorsInt;
    }

    public ArrayList<Collector<Boolean>> getInpCollectorsBin() {
        return inpCollectorsBin;
    }

    public void setInpCollectorsBin(ArrayList<Collector<Boolean>> inpCollectorsBin) {
        this.inpCollectorsBin = inpCollectorsBin;
    }

    public ArrayList<Collector<Double>> getOutCollectorsDec() {
        return outCollectorsDec;
    }

    public void setOutCollectorsDec(ArrayList<Collector<Double>> outCollectorsDec) {
        this.outCollectorsDec = outCollectorsDec;
    }

    public ArrayList<Collector<Integer>> getOutCollectorsInt() {
        return outCollectorsInt;
    }

    public void setOutCollectorsInt(ArrayList<Collector<Integer>> outCollectorsInt) {
        this.outCollectorsInt = outCollectorsInt;
    }

    public ArrayList<Collector<Boolean>> getOutCollectorsBin() {
        return outCollectorsBin;
    }

    public void setOutCollectorsBin(ArrayList<Collector<Boolean>> outCollectorsBin) {
        this.outCollectorsBin = outCollectorsBin;
    }

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
//            collector.removeInpEdge(edge);
//        }
//    }
//
//    public void removeOutputEdge(Edge edge) throws Exception {
//        for (Collector collector : outputCollectors) {
//            collector.removeInpEdge(edge);
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
     * Get decimal parameters of the node.
     *
     * @return decimal parameters of node.
     */
    public ArrayList<Double> getDecimalParameters() {
        return decimalParameters;
    }

    /**
     * Set decimal parameters of the node.
     *
     * @param decimalParameters
     */
    public void setDecimalParameters(ArrayList<Double> decimalParameters) {
        this.decimalParameters = decimalParameters;
    }

    /**
     * Get decimal parameter of the node.
     *
     * @param ind Index of the parameter array.
     * @return decimal parameter of node.
     */
    public double getDecimalParameter(int ind) {
        return decimalParameters.get(ind);
    }

    /**
     * Set decimal parameter of the node.
     *
     * @param ind Index of the parameter array.
     * @param par New value of the parameter.
     */
    public void setDecimalParameter(int ind, double par) {
        this.decimalParameters.set(ind, par);
    }

    /**
     * Get integer parameters of the node.
     *
     * @return
     */
    public ArrayList<Integer> getIntegerParameters() {
        return integerParameters;
    }

    /**
     * Set integer parameters of the node.
     *
     * @param integerParameters
     */
    public void setIntegerParameters(ArrayList<Integer> integerParameters) {
        this.integerParameters = integerParameters;
    }

    /**
     * Get boolean parameters of the node.
     *
     * @return
     */
    public ArrayList<Boolean> getBooleanParameters() {
        return booleanParameters;
    }

    /**
     * Set boolean parameters of the node.
     *
     * @param booleanParameters
     */
    public void setBooleanParameters(ArrayList<Boolean> booleanParameters) {
        this.booleanParameters = booleanParameters;
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
     * Check if input edges didn't transfer their sources nodes output signal.
     *
     * @return
     */
//    public boolean isSignalSend() {
//        for (Edge edge : inputEdges) {
//            if (edge.isSignalSend()) {
//                return false;
//            }
//        }
//        return true;
//    }

    /**
     * Check if node's inputs are ready for node calculation.
     * That includes this node's source nodes has to be signal ready and connecting edges must be transfer free (signal not send via them).
     *
     * @return
     */
//    public boolean isNodeClcReady() {
//        return isSignalReady() && isSignalSend();
//    }
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

    public Object getSignal(Object type) {
        if (type instanceof Double) {
            return signalDec;
        }
        if (type instanceof Integral) {
            return signalInt;
        }
        if (type instanceof Boolean) {
            return signalBin;
        }
        return null;
    }
}

