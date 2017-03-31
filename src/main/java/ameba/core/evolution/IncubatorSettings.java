package ameba.core.evolution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by marko on 3/31/17.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncubatorSettings {
    Integer populationSize;
    Integer eliteSize;
    Integer selection;
    String initialPopulation;
    Integer fitnessType;
    Integer prefCellSize;
    Double weightDown;
    Double weightUp;
    Double fitWeightDec;
    Double fitWeightInt;
    Double fitWeightBin;

    public IncubatorSettings() {
    }

    public IncubatorSettings(Integer populationSize, Integer eliteSize, Integer selection, String initialPopulation, Integer fitnessType, Integer prefCellSize, Double weightDown, Double weightUp, Double fitWeightDec, Double fitWeightInt, Double fitWeightBin) {
        this.populationSize = populationSize;
        this.eliteSize = eliteSize;
        this.selection = selection;
        this.initialPopulation = initialPopulation;
        this.fitnessType = fitnessType;
        this.prefCellSize = prefCellSize;
        this.weightDown = weightDown;
        this.weightUp = weightUp;
        this.fitWeightDec = fitWeightDec;
        this.fitWeightInt = fitWeightInt;
        this.fitWeightBin = fitWeightBin;
    }

    public static IncubatorSettings create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, IncubatorSettings.class);
    }

    @JsonIgnore
    public String getStringJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public Double getFitWeightDec() {
        return fitWeightDec;
    }

    public void setFitWeightDec(Double fitWeightDec) {
        this.fitWeightDec = fitWeightDec;
    }

    public Double getFitWeightInt() {
        return fitWeightInt;
    }

    public void setFitWeightInt(Double fitWeightInt) {
        this.fitWeightInt = fitWeightInt;
    }

    public Double getFitWeightBin() {
        return fitWeightBin;
    }

    public void setFitWeightBin(Double fitWeightBin) {
        this.fitWeightBin = fitWeightBin;
    }

    public Integer getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(Integer populationSize) {
        this.populationSize = populationSize;
    }

    public Integer getEliteSize() {
        return eliteSize;
    }

    public void setEliteSize(Integer eliteSize) {
        this.eliteSize = eliteSize;
    }

    public Integer getSelection() {
        return selection;
    }

    public void setSelection(Integer selection) {
        this.selection = selection;
    }

    public String getInitialPopulation() {
        return initialPopulation;
    }

    public void setInitialPopulation(String initialPopulation) {
        this.initialPopulation = initialPopulation;
    }

    public Integer getFitnessType() {
        return fitnessType;
    }

    public void setFitnessType(Integer fitnessType) {
        this.fitnessType = fitnessType;
    }

    public Integer getPrefCellSize() {
        return prefCellSize;
    }

    public void setPrefCellSize(Integer prefCellSize) {
        this.prefCellSize = prefCellSize;
    }

    public Double getWeightDown() {
        return weightDown;
    }

    public void setWeightDown(Double weightDown) {
        this.weightDown = weightDown;
    }

    public Double getWeightUp() {
        return weightUp;
    }

    public void setWeightUp(Double weightUp) {
        this.weightUp = weightUp;
    }
}
