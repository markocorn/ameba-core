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
    private Integer[] inpColLimitDec;
    private Integer[] inpColLimitInt;
    private Integer[] inpColLimitBin;

    private Integer[] outColLimitDec;
    private Integer[] outColLimitInt;
    private Integer[] outColLimitBin;

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

    public FactoryNodeSettings(String type, Integer[] inpColLimitDec, Integer[] inpColLimitInt, Integer[] inpColLimitBin, Integer[] outColLimitDec, Integer[] outColLimitInt, Integer[] outColLimitBin, Integer probability, Double initialValueDec, Integer initialValueInt, Boolean initialValueBin, Double[][] parametersLimitsDec, Integer[][] parametersLimitsInt, Boolean[][] parametersLimitsBin, Double[][] parametersInitLimitsDec, Integer[][] parametersInitLimitsInt, Boolean[][] parametersInitLimitsBin) {
        this.type = type;
        this.inpColLimitDec = inpColLimitDec;
        this.inpColLimitInt = inpColLimitInt;
        this.inpColLimitBin = inpColLimitBin;
        this.outColLimitDec = outColLimitDec;
        this.outColLimitInt = outColLimitInt;
        this.outColLimitBin = outColLimitBin;
        this.probability = probability;
        this.initialValueDec = initialValueDec;
        this.initialValueInt = initialValueInt;
        this.initialValueBin = initialValueBin;
        this.parametersLimitsDec = parametersLimitsDec;
        this.parametersLimitsInt = parametersLimitsInt;
        this.parametersLimitsBin = parametersLimitsBin;
        this.parametersInitLimitsDec = parametersInitLimitsDec;
        this.parametersInitLimitsInt = parametersInitLimitsInt;
        this.parametersInitLimitsBin = parametersInitLimitsBin;
    }


    public static FactoryNodeSettings create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, FactoryNodeSettings.class);
    }

    public void load(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        FactoryNodeSettings settings = mapper.readValue(json, FactoryNodeSettings.class);
        this.type = settings.getType();
        this.inpColLimitDec = settings.getInpColLimitDec();
        this.inpColLimitInt = settings.getInpColLimitInt();
        this.inpColLimitBin = settings.getInpColLimitBin();
        this.outColLimitDec = settings.getOutColLimitDec();
        this.outColLimitInt = settings.getOutColLimitInt();
        this.outColLimitBin = settings.getOutColLimitBin();
        this.probability = settings.getProbability();
        this.initialValueDec = settings.getInitialValueDec();
        this.initialValueInt = settings.getInitialValueInt();
        this.initialValueBin = settings.getInitialValueBin();
        this.parametersLimitsDec = settings.getParametersLimitsDec();
        this.parametersLimitsInt = settings.getParametersLimitsInt();
        this.parametersLimitsBin = settings.getParametersLimitsBin();
        this.parametersInitLimitsDec = settings.getParametersInitLimitsDec();
        this.parametersInitLimitsInt = settings.getParametersInitLimitsInt();
        this.parametersInitLimitsBin = settings.getParametersInitLimitsBin();
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
}
