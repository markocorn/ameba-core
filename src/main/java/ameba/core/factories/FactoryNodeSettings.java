package ameba.core.factories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by marko on 12/5/16.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FactoryNodeSettings {
    /**
     *
     */
    private String type;
    /**
     * Maximum number of input connection of the node
     */
    private Integer[] inpColLimitDec;
    private Integer[] inpColLimitInt;
    private Integer[] inpColLimitBin;

    private Integer[] outColLimitDec;
    private Integer[] outColLimitInt;
    private Integer[] outColLimitBin;

    /**
     * Node availability for the cell generation.
     */
    private Boolean available;
    private Integer probability;
    /**
     * Initial value if node has one.
     */
    private Double initialValueDec;

    private Integer initialValueInt;

    private Boolean initialValueBin;

    private Double[] parametersDec;
    private Integer[] parametersInt;
    private Boolean[] parametersBin;

    private Double[][] parametersLimitsDec;
    private Integer[][] parametersLimitsInt;
    private Boolean[][] parametersLimitsBin;

    public FactoryNodeSettings() {
    }

    public FactoryNodeSettings(String type, Integer[] inpColDecLimitDec, Integer[] inpColLimitInt, Integer[] inpColLimitBin, Integer[] outColLimitDec, Integer[] outColLimitInt, Integer[] outColLimitBin, Boolean available, Integer probability, Double initialValueDec, Integer initialValueInt, Boolean initialValueBin, Double[] parametersDec, Integer[] parametersInt, Boolean[] parametersBin, Double[][] parametersLimitsDec, Integer[][] parametersLimitsInt, Boolean[][] parametersLimitsBin) {
        this.type = type;
        this.inpColLimitDec = inpColDecLimitDec;
        this.inpColLimitInt = inpColLimitInt;
        this.inpColLimitBin = inpColLimitBin;
        this.outColLimitDec = outColLimitDec;
        this.outColLimitInt = outColLimitInt;
        this.outColLimitBin = outColLimitBin;
        this.available = available;
        this.probability = probability;
        this.initialValueDec = initialValueDec;
        this.initialValueInt = initialValueInt;
        this.initialValueBin = initialValueBin;
        this.parametersDec = parametersDec;
        this.parametersInt = parametersInt;
        this.parametersBin = parametersBin;
        this.parametersLimitsDec = parametersLimitsDec;
        this.parametersLimitsInt = parametersLimitsInt;
        this.parametersLimitsBin = parametersLimitsBin;
    }

    public static FactoryNodeSettings create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, FactoryNodeSettings.class);
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

    public Integer[] getInpColLimitDec() {
        return inpColLimitDec;
    }

    public void setInpColLimitDec(Integer[] inpColLimitDec) {
        this.inpColLimitDec = inpColLimitDec;
    }

    public Integer[] getInpColLimitInt() {
        return inpColLimitInt;
    }

    public void setInpColLimitInt(Integer[] inpColLimitInt) {
        this.inpColLimitInt = inpColLimitInt;
    }

    public Integer[] getInpColLimitBin() {
        return inpColLimitBin;
    }

    public void setInpColLimitBin(Integer[] inpColLimitBin) {
        this.inpColLimitBin = inpColLimitBin;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
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

    public Double[] getParametersDec() {
        return parametersDec;
    }

    public void setParametersDec(Double[] parametersDec) {
        this.parametersDec = parametersDec;
    }

    public Integer[] getParametersInt() {
        return parametersInt;
    }

    public void setParametersInt(Integer[] parametersInt) {
        this.parametersInt = parametersInt;
    }

    public Boolean[] getParametersBin() {
        return parametersBin;
    }

    public void setParametersBin(Boolean[] parametersBin) {
        this.parametersBin = parametersBin;
    }

    public Double[][] getParametersLimitsDec() {
        return parametersLimitsDec;
    }

    public void setParametersLimitsDec(Double[][] parametersLimitsDec) {
        this.parametersLimitsDec = parametersLimitsDec;
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

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public Integer[] getOutColLimitDec() {
        return outColLimitDec;
    }

    public void setOutColLimitDec(Integer[] outColLimitDec) {
        this.outColLimitDec = outColLimitDec;
    }

    public Integer[] getOutColLimitInt() {
        return outColLimitInt;
    }

    public void setOutColLimitInt(Integer[] outColLimitInt) {
        this.outColLimitInt = outColLimitInt;
    }

    public Integer[] getOutColLimitBin() {
        return outColLimitBin;
    }

    public void setOutColLimitBin(Integer[] outColLimitBin) {
        this.outColLimitBin = outColLimitBin;
    }
}