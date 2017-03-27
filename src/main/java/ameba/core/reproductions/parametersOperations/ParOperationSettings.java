package ameba.core.reproductions.parametersOperations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by marko on 12/21/16.
 */
public class ParOperationSettings {

    private Integer probability;

    private Double[] changeLimitDec;
    private Integer[] changeLimitInt;
    private Boolean[] changeLimitBin;

    private Double[] valueLimitDec;
    private Integer[] valueLimitInt;
    private Boolean[] valueLimitBin;


    public ParOperationSettings() {
    }

    public ParOperationSettings(Integer probability, Double[] changeLimitDec, Integer[] changeLimitInt, Boolean[] changeLimitBin, Double[] valueLimitDec, Integer[] valueLimitInt, Boolean[] valueLimitBin) {
        this.probability = probability;
        this.changeLimitDec = changeLimitDec;
        this.changeLimitInt = changeLimitInt;
        this.changeLimitBin = changeLimitBin;
        this.valueLimitDec = valueLimitDec;
        this.valueLimitInt = valueLimitInt;
        this.valueLimitBin = valueLimitBin;

    }

    public static ParOperationSettings create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, ParOperationSettings.class);
    }

    @JsonIgnore
    public String getStringJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public Double[] getChangeLimitDec() {
        return changeLimitDec;
    }

    public void setChangeLimitDec(Double[] changeLimitDec) {
        this.changeLimitDec = changeLimitDec;
    }

    public Integer[] getChangeLimitInt() {
        return changeLimitInt;
    }

    public void setChangeLimitInt(Integer[] changeLimitInt) {
        this.changeLimitInt = changeLimitInt;
    }

    public Boolean[] getChangeLimitBin() {
        return changeLimitBin;
    }

    public void setChangeLimitBin(Boolean[] changeLimitBin) {
        this.changeLimitBin = changeLimitBin;
    }

    public Double[] getValueLimitDec() {
        return valueLimitDec;
    }

    public void setValueLimitDec(Double[] valueLimitDec) {
        this.valueLimitDec = valueLimitDec;
    }

    public Integer[] getValueLimitInt() {
        return valueLimitInt;
    }

    public void setValueLimitInt(Integer[] valueLimitInt) {
        this.valueLimitInt = valueLimitInt;
    }

    public Boolean[] getValueLimitBin() {
        return valueLimitBin;
    }

    public void setValueLimitBin(Boolean[] valueLimitBin) {
        this.valueLimitBin = valueLimitBin;
    }

}
