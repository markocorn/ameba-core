package ameba.evolution.incubator;

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

    private Double weightEdge;
    private Double weightError;
    private Double weightActuator;
    private Double weightErrorDiff;
    private Double weightActuatorDiff;

    private Double[] fitWeight;

    public IncubatorSettings() {
    }

    public IncubatorSettings(Integer populationSize, Integer eliteSize, Integer selectionType, Integer chosen, Integer fitnessType, Integer prefCellSize, Double weightDown, Double weightUp, Double weightEdge, Double weightError, Double weightActuator, Double weightErrorDiff, Double weightActuatorDiff, Double[] fitWeight) {
        this.populationSize = populationSize;
        this.eliteSize = eliteSize;
        this.selectionType = selectionType;
        this.chosen = chosen;
        this.fitnessType = fitnessType;
        this.prefCellSize = prefCellSize;
        this.weightDown = weightDown;
        this.weightUp = weightUp;
        this.weightEdge = weightEdge;
        this.weightError = weightError;
        this.weightActuator = weightActuator;
        this.weightErrorDiff = weightErrorDiff;
        this.weightActuatorDiff = weightActuatorDiff;
        this.fitWeight = fitWeight;
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

    public Double getWeightErrorDiff() {
        return weightErrorDiff;
    }

    public void setWeightErrorDiff(Double weightErrorDiff) {
        this.weightErrorDiff = weightErrorDiff;
    }

    public Double getWeightActuatorDiff() {
        return weightActuatorDiff;
    }

    public void setWeightActuatorDiff(Double weightActuatorDiff) {
        this.weightActuatorDiff = weightActuatorDiff;
    }

    public Double getWeightError() {
        return weightError;
    }

    public void setWeightError(Double weightError) {
        this.weightError = weightError;
    }

    public Double getWeightActuator() {
        return weightActuator;
    }

    public void setWeightActuator(Double weightActuator) {
        this.weightActuator = weightActuator;
    }

    public Integer getChosen() {
        return chosen;
    }

    public void setChosen(Integer chosen) {
        this.chosen = chosen;
    }

    public Double[] getFitWeight() {
        return fitWeight;
    }

    public void setFitWeight(Double[] fitWeight) {
        this.fitWeight = fitWeight;
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

    public Double getWeightEdge() {
        return weightEdge;
    }

    public void setWeightEdge(Double weightEdge) {
        this.weightEdge = weightEdge;
    }
}
