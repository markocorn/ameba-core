package blocks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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
     * Maximum number on output connection of the node.
     */
    private int maxOutputEdges;
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
    @JsonManagedReference
    private double signal;
    /**
     * Flag that indicates signal is reads to be sent to other blocks.nodes.
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
    public Node(int maxInputEdges, int maxOutputEdges) {
        this.maxInputEdges = maxInputEdges;
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
     * @param edge blocks.Edge to be added to the input edges of the node.
     */
    public void addInputEdge(Edge edge) {
        if (maxInputEdges < inputEdges.size()) {
            inputEdges.add(edge);
        }
    }

    /**
     * Add edge to the output edges of the node.
     *
     * @param edge blocks.Edge to be added to the output edges of the node.
     */
    public void addOutputEdge(Edge edge) {
        if (maxOutputEdges < outputEdges.size()) {
            outputEdges.add(edge);
        }
    }

    /**
     * Remove edge from the input edges of the node.
     *
     * @param edge blocks.Edge to be removed from the input edges of the node.
     */
    public void removeInputEdge(Edge edge) {
        inputEdges.remove(edge);
    }

    /**
     * Remove edge from the input edges of the node.
     *
     * @param edge blocks.Edge to be removed from the output edges of the node.
     */
    public void removeOutputEdge(Edge edge) {
        outputEdges.remove(edge);
    }

    /**
     * Replace edge from the inputs edges of the node.
     *
     * @param edgeOld blocks.Edge to be replaced.
     * @param edgeNew blocks.Edge for replacement.
     */
    public void replaceInputEdge(Edge edgeOld, Edge edgeNew) {
        inputEdges.set(inputEdges.indexOf(edgeOld), edgeNew);
    }

    /**
     * Replace edge from the outputs edges of the node.
     *
     * @param edgeOld blocks.Edge to be replaced.
     * @param edgeNew blocks.Edge for replacement.
     */
    public void replaceOutputEdge(Edge edgeOld, Edge edgeNew) {
        outputEdges.set(outputEdges.indexOf(edgeOld), edgeNew);
    }

    /**
     * Calculate node's output signal trough it's mapping function if the input edges contains signals from their source blocks.nodes.
     */
    public void clcNode() {
    }

    /**
     * Reset node's flags {@link Node#signalReady}.
     */
    public void rstNode() {
        signalReady = false;
    }

    /**
     * Clear node reset's node and reset also the signal value.
     */
    public void clearNode() {
        rstNode();
        signal = 0.0;
    }

    /**
     * Get maximum number of input edges of the node.
     *
     * @return
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
     * Get maximum number of output edges of the node.
     *
     * @return
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
     * @return
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
}

