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
    private Integer populationSize;
    private Integer eliteSize;
    private Integer selectionType;
    private Integer chosen;
    private Integer fitnessType;
    private Integer prefCellSize;
    private Double weightDown;
    private Double weightUp;
    private Double fitWeightDec;
    private Double fitWeightInt;
    private Double fitWeightBin;


    public IncubatorSettings() {
    }


    public IncubatorSettings(Integer populationSize, Integer eliteSize, Integer selectionType, Integer chosen, Integer fitnessType, Integer prefCellSize, Double weightDown, Double weightUp, Double fitWeightDec, Double fitWeightInt, Double fitWeightBin) {
        this.populationSize = populationSize;
        this.eliteSize = eliteSize;
        this.selectionType = selectionType;
        this.chosen = chosen;
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

    public Integer getChosen() {
        return chosen;
    }

    public void setChosen(Integer chosen) {
        this.chosen = chosen;
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

    public Integer getSelectionType() {
        return selectionType;
    }

    public void setSelectionType(Integer selectionType) {
        this.selectionType = selectionType;
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
