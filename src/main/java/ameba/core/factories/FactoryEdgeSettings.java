package ameba.core.factories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by marko on 12/5/16.
 */
public class FactoryEdgeSettings {
    /**
     * IntervalDec of values for the weight initial value during rndGen generation.
     */
    private Double[] weightInitialDec;
    private Integer[] weightInitialInt;
    private Boolean[] weightInitialBin;
    /**
     * IntervalDec of limit values for the
     */
    private Double[] weighLimitsDec;
    private Integer[] weighLimitsInt;
    private Boolean[] weighLimitsBin;

    /**
     * Default constructor.
     */
    public FactoryEdgeSettings() {
    }

    public FactoryEdgeSettings(Double[] weightInitialDec, Integer[] weightInitialInt, Boolean[] weightInitialBin, Double[] weighLimitsDec, Integer[] weighLimitsInt, Boolean[] weighLimitsBin) {
        this.weightInitialDec = weightInitialDec;
        this.weightInitialInt = weightInitialInt;
        this.weightInitialBin = weightInitialBin;
        this.weighLimitsDec = weighLimitsDec;
        this.weighLimitsInt = weighLimitsInt;
        this.weighLimitsBin = weighLimitsBin;
    }

    public static FactoryEdgeSettings create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, FactoryEdgeSettings.class);
    }

    public void load(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        FactoryEdgeSettings settings = mapper.readValue(json, FactoryEdgeSettings.class);
        this.weightInitialDec = settings.getWeightInitialDec();
        this.weightInitialInt = settings.getWeightInitialInt();
        this.weightInitialBin = settings.getWeightInitialBin();
        this.weighLimitsDec = settings.getWeighLimitsDec();
        this.weighLimitsInt = settings.getWeighLimitsInt();
        this.weighLimitsBin = settings.getWeighLimitsBin();
    }

    @JsonIgnore
    public String getStringJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public Double[] getWeightInitialDec() {
        return weightInitialDec;
    }

    public void setWeightInitialDec(Double[] weightInitialDec) {
        this.weightInitialDec = weightInitialDec;
    }

    public Integer[] getWeightInitialInt() {
        return weightInitialInt;
    }

    public void setWeightInitialInt(Integer[] weightInitialInt) {
        this.weightInitialInt = weightInitialInt;
    }

    public Boolean[] getWeightInitialBin() {
        return weightInitialBin;
    }

    public void setWeightInitialBin(Boolean[] weightInitialBin) {
        this.weightInitialBin = weightInitialBin;
    }

    public Double[] getWeighLimitsDec() {
        return weighLimitsDec;
    }

    public void setWeighLimitsDec(Double[] weighLimitsDec) {
        this.weighLimitsDec = weighLimitsDec;
    }

    public Integer[] getWeighLimitsInt() {
        return weighLimitsInt;
    }

    public void setWeighLimitsInt(Integer[] weighLimitsInt) {
        this.weighLimitsInt = weighLimitsInt;
    }

    public Boolean[] getWeighLimitsBin() {
        return weighLimitsBin;
    }

    public void setWeighLimitsBin(Boolean[] weighLimitsBin) {
        this.weighLimitsBin = weighLimitsBin;
    }
}
