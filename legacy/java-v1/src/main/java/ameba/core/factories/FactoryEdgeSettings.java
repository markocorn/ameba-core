package ameba.core.factories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by marko on 12/5/16.
 */
public class FactoryEdgeSettings implements Serializable {
    /**
     * IntervalDec of values for the weight initial value during rndGen generation.
     */
    private Double[] weightInitialDec;
    /**
     * IntervalDec of limit values for the
     */
    private Double[] weighLimitsDec;

    /**
     * Default constructor.
     */
    public FactoryEdgeSettings() {
    }

    public FactoryEdgeSettings(Double[] weightInitialDec, Integer[] weightInitialInt, Boolean[] weightInitialBin, Double[] weighLimitsDec, Integer[] weighLimitsInt, Boolean[] weighLimitsBin) {
        this.weightInitialDec = weightInitialDec;
        this.weighLimitsDec = weighLimitsDec;
    }

    public static FactoryEdgeSettings create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, FactoryEdgeSettings.class);
    }

    public void load(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        FactoryEdgeSettings settings = mapper.readValue(json, FactoryEdgeSettings.class);
        this.weightInitialDec = settings.getWeightInitialDec();
        this.weighLimitsDec = settings.getWeighLimitsDec();
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

    public Double[] getWeighLimitsDec() {
        return weighLimitsDec;
    }

    public void setWeighLimitsDec(Double[] weighLimitsDec) {
        this.weighLimitsDec = weighLimitsDec;
    }
}
