package ameba.core.blocks;

import java.util.ArrayList;

/**
 * Base node class.
 */
public class Node implements Cloneable {
    /**
     * Maximum number of input connection of the node.
     */
    private int maxInputEdges;
    /**
     * Minimum number of input connection of the node.
     */
    private int minInputEdges;
    /**
     * Maximum number on output connection of the node.
     */
    private int maxOutputEdges;
    /**
     * Minimum number of output connection of the node.
     */
    private int minOutputEdges;
    /**
     * List of input edges of the node.
     */
    private ArrayList<Edge> inputEdges;
    /**
     * List of output edges of the node.
     */
    private ArrayList<Edge> outputEdges;
    /**
     * Mapped value of the node's signal.
     */
    private double signal;
    /**
     * Flag that indicates signal is reads to be sent to other ameba.core.blocks.nodes.
     */
    private boolean signalReady;
    /**
     * List of decimal parameters.
     */
    private double[] decimalParameters;
    /**
     * List of integer parameters.
     */
    private int[] integerParameters;
    /**
     * List of boolean parameters.
     */
    private boolean[] booleanParameters;

    /**
     * Construct node object with three basic parameters.
     *
     * @param maxInputEdges  Maximum number of input edges.
     * @param maxOutputEdges Maximum number of output edges.
     */
    public Node(int minInputEdges, int maxInputEdges, int minOutputEdges, int maxOutputEdges) {
        this.minInputEdges = minInputEdges;
        this.maxInputEdges = maxInputEdges;
        this.minOutputEdges = minOutputEdges;
        this.maxOutputEdges = maxOutputEdges;
        inputEdges = new ArrayList<Edge>();
        outputEdges = new ArrayList<Edge>();
        signal = 0;
        signalReady = false;
    }

    /**
     * Get input edges.
     *
     * @return
     */
    public ArrayList<Edge> getInputEdges() {
        return inputEdges;
    }

    /**
     * Set input edges.
     *
     * @param inputEdges List of edges to set.
     */
    public void setInputEdges(ArrayList<Edge> inputEdges) {
        this.inputEdges.clear();
        for (int i = 0; i < maxInputEdges || i < inputEdges.size(); i++) {
            this.inputEdges.add(inputEdges.get(i));
        }
    }

    /**
     * Get output edges.
     *
     * @return
     */
    public ArrayList<Edge> getOutputEdges() {
        return outputEdges;
    }

    /**
     * Set output edges.
     *
     * @param outputEdges List of edges to set.
     */
    public void setOutputEdges(ArrayList<Edge> outputEdges) {
        this.outputEdges.clear();
        for (int i = 0; i < maxOutputEdges || i < outputEdges.size(); i++) {
            this.outputEdges.add(outputEdges.get(i));
        }
    }

    /**
     * Get node signal that is a result of node's mapping function.
     *
     * @return
     */
    public double getSignal() {
        return signal;
    }

    /**
     * Set node signal value that represents output signal of the node
     *
     * @param outSignal Value of the signal.
     */
    public void setSignal(double outSignal) {
        this.signal = outSignal;
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


    /**
     * Add edge to the input edges of the node.
     *
     * @param edge ameba.core.blocks.Edge to be added to the input edges of the node.
     */
    public void addInputEdge(Edge edge) {
        if (maxInputEdges > inputEdges.size()) {
            inputEdges.add(edge);
        }
    }

    /**
     * Add edge to the output edges of the node.
     *
     * @param edge ameba.core.blocks.Edge to be added to the output edges of the node.
     */
    public void addOutputEdge(Edge edge) {
        if (maxOutputEdges > outputEdges.size()) {
            outputEdges.add(edge);
        }
    }

    /**
     * Remove edge from the input edges of the node.
     *
     * @param edge ameba.core.blocks.Edge to be removed from the input edges of the node.
     */
    public void removeInputEdge(Edge edge) {
        inputEdges.remove(edge);
    }

    /**
     * Remove edge from the input edges of the node.
     *
     * @param edge ameba.core.blocks.Edge to be removed from the output edges of the node.
     */
    public void removeOutputEdge(Edge edge) {
        outputEdges.remove(edge);
    }

    /**
     * Replace edge from the inputs edges of the node.
     *
     * @param edgeOld ameba.core.blocks.Edge to be replaced.
     * @param edgeNew ameba.core.blocks.Edge for replacement.
     */
    public void replaceInputEdge(Edge edgeOld, Edge edgeNew) {
        inputEdges.set(inputEdges.indexOf(edgeOld), edgeNew);
    }

    /**
     * Replace edge from the outputs edges of the node.
     *
     * @param edgeOld ameba.core.blocks.Edge to be replaced.
     * @param edgeNew ameba.core.blocks.Edge for replacement.
     */
    public void replaceOutputEdge(Edge edgeOld, Edge edgeNew) {
        outputEdges.set(outputEdges.indexOf(edgeOld), edgeNew);
    }

    /**
     * Get maximum number of input edges of the node.
     *
     * @return Maximum number of input edges.
     */
    public int getMaxInputEdges() {
        return maxInputEdges;
    }

    /**
     * Set maximum number of input edges of the node.
     *
     * @param maxInputEdges Maximum number of edges.
     */
    public void setMaxInputEdges(int maxInputEdges) {
        this.maxInputEdges = maxInputEdges;
    }

    /**
     * Get minimum number of input edges.
     *
     * @return Minimum number of input edges.
     */
    public int getMinInputEdges() {
        return minInputEdges;
    }

    /**
     * Set minimum number of input edges
     *
     * @param minInputEdges Minimum number of input edges.
     */
    public void setMinInputEdges(int minInputEdges) {
        this.minInputEdges = minInputEdges;
    }

    /**
     * Get minimum number of output edges.
     *
     * @return Minimum number of output edges.
     */
    public int getMinOutputEdges() {
        return minOutputEdges;
    }

    /**
     * Set minimum number of output edges.
     *
     * @param minOutputEdges Minimum number of input edges.
     */
    public void setMinOutputEdges(int minOutputEdges) {
        this.minOutputEdges = minOutputEdges;
    }

    /**
     * Get maximum number of output edges of the node.
     *
     * @return Maximum number of output edges.
     */
    public int getMaxOutputEdges() {
        return maxOutputEdges;
    }

    /**
     * Set maximum number of output edges of the node.
     *
     * @param maxOutputConnections Maximum number of edges.
     */
    public void setMaxOutputEdges(int maxOutputConnections) {
        this.maxOutputEdges = maxOutputConnections;
    }

    /**
     * Get decimal parameters of the node.
     *
     * @return Decimal parameters of node.
     */
    public double[] getDecimalParameters() {
        return decimalParameters;
    }

    /**
     * Set decimal parameters of the node.
     *
     * @param decimalParameters
     */
    public void setDecimalParameters(double[] decimalParameters) {
        this.decimalParameters = decimalParameters;
    }

    /**
     * Get decimal parameter of the node.
     *
     * @param ind Index of the parameter array.
     * @return Decimal parameter of node.
     */
    public double getDecimalParameter(int ind) {
        return decimalParameters[ind];
    }

    /**
     * Set decimal parameter of the node.
     *
     * @param ind Index of the parameter array.
     * @param par New value of the parameter.
     */
    public void setDecimalParameter(int ind, double par) {
        this.decimalParameters[ind] = par;
    }

    /**
     * Get integer parameters of the node.
     *
     * @return
     */
    public int[] getIntegerParameters() {
        return integerParameters;
    }

    /**
     * Set integer parameters of the node.
     *
     * @param integerParameters
     */
    public void setIntegerParameters(int[] integerParameters) {
        this.integerParameters = integerParameters;
    }

    /**
     * Get boolean parameters of the node.
     *
     * @return
     */
    public boolean[] getBooleanParameters() {
        return booleanParameters;
    }

    /**
     * Set boolean parameters of the node.
     *
     * @param booleanParameters
     */
    public void setBooleanParameters(boolean[] booleanParameters) {
        this.booleanParameters = booleanParameters;
    }

    /**
     * Check if all nodes connected as source have signal ready for this node's output calculation.
     *
     * @return
     */
    public boolean isSourcesSignalReady() {
        for (Edge edge : inputEdges) {
            if (!edge.isSignalReady()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if input edges didn't transfer their sources nodes output signal.
     *
     * @return
     */
    public boolean isSourcesSignalSend() {
        for (Edge edge : inputEdges) {
            if (edge.isSignalSend()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if node's inputs are ready for node calculation.
     * That includes this node's source nodes has to be signal ready and connecting edges must be transfer free (signal not send via them).
     *
     * @return
     */
    public boolean isNodeClcReady() {
        return isSourcesSignalReady() && isSourcesSignalSend();
    }

    /**
     * Get the difference between minimum and actual number of input edges.
     *
     * @return Difference.
     */
    public int getMinInputEdgesDifference() {
        return minInputEdges - inputEdges.size();
    }

    /**
     * Get the difference between minimum and actual number of output edges.
     *
     * @return Difference.
     */
    public int getMinOutputEdgesDifference() {
        return minOutputEdges - outputEdges.size();
    }

    /**
     * Get the difference between maximum and actual number of input edges.
     *
     * @return Difference.
     */
    public int getMaxInputEdgesDifference() {
        return maxInputEdges - inputEdges.size();
    }

    /**
     * Get the difference between maximum and actual number of output edges.
     *
     * @return Difference.
     */
    public int getMaxOutputEdgesDifference() {
        return maxOutputEdges - outputEdges.size();
    }

    public void clcNode() {

    }

    public void clearNode() {

    }

    public void rstNode() {

    }
}

