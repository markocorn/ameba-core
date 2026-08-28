package ameba.core.factories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by marko on 12/5/16.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FactoryNodeSettings implements Serializable {
    /**
     *
     */
    private String type;
    /**
     * Maximum number of input connection of the node
     */
    private Integer[] collectorsTargetLimit;

    private Integer[] collectorsSourceLimit;

    /**
     * Node availability for the cell generation.
     */
    private Integer probability;
    /**
     * Initial value if node has one.
     */
    private Double initialValueDec;

    private Integer initialValueInt;

    private Boolean initialValueBin;

    /**
     * Parameters constrains
     */
    private Double[][] parametersLimitsDec;

    private Integer[][] parametersLimitsInt;

    private Boolean[][] parametersLimitsBin;


    /**
     * Parameters initial generations constrains
     */
    private Double[][] parametersInitLimitsDec;

    private Integer[][] parametersInitLimitsInt;

    private Boolean[][] parametersInitLimitsBin;

    public FactoryNodeSettings() {
    }

    public FactoryNodeSettings(String type, Integer[] inpColLimitDec, Integer[] outColLimitDec, Integer probability, Double initialValueDec, Integer initialValueInt, Boolean initialValueBin, Double[][] parametersLimitsDec, Double[][] parametersInitLimitsDec) {
        this.type = type;
        this.collectorsTargetLimit = inpColLimitDec;
        this.collectorsSourceLimit = outColLimitDec;
        this.probability = probability;
        this.initialValueDec = initialValueDec;
        this.initialValueInt = initialValueInt;
        this.initialValueBin = initialValueBin;
        this.parametersLimitsDec = parametersLimitsDec;
        this.parametersInitLimitsDec = parametersInitLimitsDec;
    }


    public static FactoryNodeSettings create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, FactoryNodeSettings.class);
    }

    public void load(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        FactoryNodeSettings settings = mapper.readValue(json, FactoryNodeSettings.class);
        this.type = settings.getType();
        this.collectorsTargetLimit = settings.getCollectorsTargetLimit();
        this.collectorsSourceLimit = settings.getOutColLimitDec();
        this.probability = settings.getProbability();
        this.initialValueDec = settings.getInitialValueDec();
        this.initialValueInt = settings.getInitialValueInt();
        this.initialValueBin = settings.getInitialValueBin();
        this.parametersLimitsDec = settings.getParametersLimitsDec();
        this.parametersInitLimitsDec = settings.getParametersInitLimitsDec();
    }

    @JsonIgnore
    public String getStringJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer[] getCollectorsTargetLimit() {
        return collectorsTargetLimit;
    }

    public void setCollectorsTargetLimit(Integer[] collectorsTargetLimit) {
        this.collectorsTargetLimit = collectorsTargetLimit;
    }

    public Double getInitialValueDec() {
        return initialValueDec;
    }

    public void setInitialValueDec(Double initialValueDec) {
        this.initialValueDec = initialValueDec;
    }

    public Integer getInitialValueInt() {
        return initialValueInt;
    }

    public void setInitialValueInt(Integer initialValueInt) {
        this.initialValueInt = initialValueInt;
    }

    public Boolean getInitialValueBin() {
        return initialValueBin;
    }

    public void setInitialValueBin(Boolean initialValueBin) {
        this.initialValueBin = initialValueBin;
    }

    public Double[][] getParametersLimitsDec() {
        return parametersLimitsDec;
    }

    public void setParametersLimitsDec(Double[][] parametersLimitsDec) {
        this.parametersLimitsDec = parametersLimitsDec;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public Integer[] getOutColLimitDec() {
        return collectorsSourceLimit;
    }

    public void setOutColLimitDec(Integer[] outColLimitDec) {
        this.collectorsSourceLimit = outColLimitDec;
    }

    public Double[][] getParametersInitLimitsDec() {
        return parametersInitLimitsDec;
    }

    public void setParametersInitLimitsDec(Double[][] parametersInitLimitsDec) {
        this.parametersInitLimitsDec = parametersInitLimitsDec;
    }

    public Integer[][] getParametersInitLimitsInt() {
        return parametersInitLimitsInt;
    }

    public void setParametersInitLimitsInt(Integer[][] parametersInitLimitsInt) {
        this.parametersInitLimitsInt = parametersInitLimitsInt;
    }

    public Boolean[][] getParametersInitLimitsBin() {
        return parametersInitLimitsBin;
    }

    public void setParametersInitLimitsBin(Boolean[][] parametersInitLimitsBin) {
        this.parametersInitLimitsBin = parametersInitLimitsBin;
    }

    public Integer[] getCollectorsSourceLimit() {
        return collectorsSourceLimit;
    }

    public void setCollectorsSourceLimit(Integer[] collectorsSourceLimit) {
        this.collectorsSourceLimit = collectorsSourceLimit;
    }

    public Integer[][] getParametersLimitsInt() {
        return parametersLimitsInt;
    }

    public void setParametersLimitsInt(Integer[][] parametersLimitsInt) {
        this.parametersLimitsInt = parametersLimitsInt;
    }

    public Boolean[][] getParametersLimitsBin() {
        return parametersLimitsBin;
    }

    public void setParametersLimitsBin(Boolean[][] parametersLimitsBin) {
        this.parametersLimitsBin = parametersLimitsBin;
    }
}
