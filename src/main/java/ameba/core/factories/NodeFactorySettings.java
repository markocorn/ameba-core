package ameba.core.factories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by marko on 12/5/16.
 */
public class NodeFactorySettings {
    /**
     *
     */
    private String nodeType;
    /**
     * Maximum number of input connection of the node.
     */
    private int maxInputEdges;
    /**
     * Minimum number of input connections.
     */
    private int minInputEdges;
    /**
     * Maximum number on output connection of the node.
     */
    private int maxOutputEdges;
    /**
     * Minimum number of output connections.
     */
    private int minOutputEdges;
    /**
     * Node availability for the cell generation.
     */
    private boolean available;
    /**
     * Initial value if node has one.
     */
    private double initialValue;
    /**
     * Initial interval for rndGen generation of decimal parameters.
     */
    private double[][] initialDecimalParametersLimits;
    /**
     * Initial interval for rndGen generation of integral parameters.
     */
    private int[][] initialIntegralParametersLimits;
    /**
     * Initial interval for rndGen generation of boolean parameters.
     */
    private boolean[][] initialBooleanParametersLimits;
    /**
     * Limit interval for rndGen generation of decimal parameters.
     */
    private double[][] decimalParametersLimits;
    /**
     * Limit interval for rndGen generation of integral parameters.
     */
    private int[][] integralParametersLimits;
    /**
     * Limit interval for rndGen generation of boolean parameters.
     */
    private boolean[][] booleanParametersLimits;

    /**
     * Default constructor.
     */
    public NodeFactorySettings() {
    }

    /**
     * General constructor.
     *
     * @param maxInputEdges
     * @param maxOutputEdges
     * @param available
     * @param initialDecimalParametersLimits
     * @param initialIntegralParametersLimits
     * @param initialBooleanParametersLimits
     * @param decimalParametersLimits
     * @param integralParametersLimits
     * @param booleanParametersLimits
     */
    public NodeFactorySettings(String nodeType, int maxInputEdges, int minInputEdges, int maxOutputEdges, int minOutputEdges, boolean available, double initialValue, double[][] initialDecimalParametersLimits, int[][] initialIntegralParametersLimits, boolean[][] initialBooleanParametersLimits, double[][] decimalParametersLimits, int[][] integralParametersLimits, boolean[][] booleanParametersLimits) {
        this.nodeType = nodeType;
        this.maxInputEdges = maxInputEdges;
        this.minInputEdges = minInputEdges;
        this.maxOutputEdges = maxOutputEdges;
        this.minOutputEdges = minOutputEdges;
        this.available = available;
        this.initialValue = initialValue;
        this.initialDecimalParametersLimits = initialDecimalParametersLimits;
        this.initialIntegralParametersLimits = initialIntegralParametersLimits;
        this.initialBooleanParametersLimits = initialBooleanParametersLimits;
        this.decimalParametersLimits = decimalParametersLimits;
        this.integralParametersLimits = integralParametersLimits;
        this.booleanParametersLimits = booleanParametersLimits;
    }

    /**
     * Build object from settings in form of a json string.
     *
     * @param jsonSettings String of settings.
     */
    public NodeFactorySettings(String jsonSettings) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            NodeFactorySettings settings = mapper.readValue(jsonSettings, NodeFactorySettings.class);
            this.nodeType = settings.getNodeType();
            this.maxInputEdges = settings.getMaxInputEdges();
            this.maxOutputEdges = settings.getMaxOutputEdges();
            this.available = settings.isAvailable();
            this.initialValue = settings.getInitialValue();
            this.initialDecimalParametersLimits = settings.getInitialDecimalParametersLimits();
            this.initialIntegralParametersLimits = settings.getInitialIntegerParametersLimits();
            this.initialBooleanParametersLimits = settings.getInitialBooleanParametersLimits();
            this.decimalParametersLimits = settings.getDecimalParametersLimits();
            this.integralParametersLimits = settings.getIntegralParametersLimits();
            this.booleanParametersLimits = settings.getBooleanParametersLimits();

        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, NodeFactorySettings> genNodeFactorySettingsHashMap(String jsonSettings) {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, NodeFactorySettings> settingsHashMap = new HashMap<String, NodeFactorySettings>();
        try {
            JsonNode jsonNode = mapper.readTree(jsonSettings);
            for (int i = 0; i < jsonNode.size(); i++) {
                NodeFactorySettings nodeFactorySettings = new NodeFactorySettings(jsonNode.get(i).toString());
                settingsHashMap.put(nodeFactorySettings.getNodeType(), nodeFactorySettings);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return settingsHashMap;
    }

    /**
     * @return Json representation of the object.
     */
    @JsonIgnore
    public String getTextJson() {
        ObjectMapper mapper = new ObjectMapper();
        String out = "";
        try {
            out = mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return out;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public int getMaxInputEdges() {
        return maxInputEdges;
    }

    public void setMaxInputEdges(int maxInputEdges) {
        this.maxInputEdges = maxInputEdges;
    }

    public int getMaxOutputEdges() {
        return maxOutputEdges;
    }

    public void setMaxOutputEdges(int maxOutputEdges) {
        this.maxOutputEdges = maxOutputEdges;
    }

    public int getMinInputEdges() {
        return minInputEdges;
    }

    public void setMinInputEdges(int minInputEdges) {
        this.minInputEdges = minInputEdges;
    }

    public int getMinOutputEdges() {
        return minOutputEdges;
    }

    public void setMinOutputEdges(int minOutputEdges) {
        this.minOutputEdges = minOutputEdges;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public double getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(double initialValue) {
        this.initialValue = initialValue;
    }

    public int[][] getInitialIntegralParametersLimits() {
        return initialIntegralParametersLimits;
    }

    public void setInitialIntegralParametersLimits(int[][] initialIntegralParametersLimits) {
        this.initialIntegralParametersLimits = initialIntegralParametersLimits;
    }

    public double[][] getInitialDecimalParametersLimits() {
        return initialDecimalParametersLimits;
    }

    public void setInitialDecimalParametersLimits(double[][] initialDecimalParametersLimits) {
        this.initialDecimalParametersLimits = initialDecimalParametersLimits;
    }

    public int[][] getInitialIntegerParametersLimits() {
        return initialIntegralParametersLimits;
    }

    public boolean[][] getInitialBooleanParametersLimits() {
        return initialBooleanParametersLimits;
    }

    public void setInitialBooleanParametersLimits(boolean[][] initialBooleanParametersLimits) {
        this.initialBooleanParametersLimits = initialBooleanParametersLimits;
    }

    public double[][] getDecimalParametersLimits() {
        return decimalParametersLimits;
    }

    public void setDecimalParametersLimits(double[][] decimalParametersLimits) {
        this.decimalParametersLimits = decimalParametersLimits;
    }

    public int[][] getIntegralParametersLimits() {
        return integralParametersLimits;
    }

    public void setIntegralParametersLimits(int[][] integralParametersLimits) {
        this.integralParametersLimits = integralParametersLimits;
    }

    public boolean[][] getBooleanParametersLimits() {
        return booleanParametersLimits;
    }

    public void setBooleanParametersLimits(boolean[][] booleanParametersLimits) {
        this.booleanParametersLimits = booleanParametersLimits;
    }
}
