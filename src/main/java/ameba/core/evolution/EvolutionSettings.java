package ameba.core.evolution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by marko on 4/3/17.
 */
public class EvolutionSettings {
    private String settingsPathFile;
    private String dataPathFile;
    private Boolean loadInitialPopulation;
    private String initialPopulationPathFile;
    private Boolean savePopulation;
    private String savePopulationPathFile;
    private Integer savePopulationPeriod;
    private Integer savePopulationSize;
    private Integer evolutionExitType;
    private Integer maxGenerations;
    private Integer fitnessChange;
    private Boolean enableGPU;


    public EvolutionSettings() {
    }

    public EvolutionSettings(String settingsPathFile, String dataPathFile, Boolean loadInitialPopulation, String initialPopulationPathFile, Boolean savePopulation, String savePopulationPathFile, Integer savePopulationPeriod, Integer savePopulationSize, Integer evolutionExitType, Integer maxGenerations, Integer fitnessChange, Boolean enableGPU) {
        this.settingsPathFile = settingsPathFile;
        this.dataPathFile = dataPathFile;
        this.loadInitialPopulation = loadInitialPopulation;
        this.initialPopulationPathFile = initialPopulationPathFile;
        this.savePopulation = savePopulation;
        this.savePopulationPathFile = savePopulationPathFile;
        this.savePopulationPeriod = savePopulationPeriod;
        this.savePopulationSize = savePopulationSize;
        this.evolutionExitType = evolutionExitType;
        this.maxGenerations = maxGenerations;
        this.fitnessChange = fitnessChange;
        this.enableGPU = enableGPU;
    }

    public static EvolutionSettings create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, EvolutionSettings.class);
    }

    @JsonIgnore
    public String getStringJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public Boolean getLoadInitialPopulation() {
        return loadInitialPopulation;
    }

    public void setLoadInitialPopulation(Boolean loadInitialPopulation) {
        this.loadInitialPopulation = loadInitialPopulation;
    }

    public Boolean getSavePopulation() {
        return savePopulation;
    }

    public void setSavePopulation(Boolean savePopulation) {
        this.savePopulation = savePopulation;
    }

    public Integer getSavePopulationSize() {
        return savePopulationSize;
    }

    public void setSavePopulationSize(Integer savePopulationSize) {
        this.savePopulationSize = savePopulationSize;
    }

    public Boolean getEnableGPU() {
        return enableGPU;
    }

    public void setEnableGPU(Boolean enableGPU) {
        this.enableGPU = enableGPU;
    }

    public String getSettingsPathFile() {
        return settingsPathFile;
    }

    public void setSettingsPathFile(String settingsPathFile) {
        this.settingsPathFile = settingsPathFile;
    }

    public String getDataPathFile() {
        return dataPathFile;
    }

    public void setDataPathFile(String dataPathFile) {
        this.dataPathFile = dataPathFile;
    }

    public String getInitialPopulationPathFile() {
        return initialPopulationPathFile;
    }

    public void setInitialPopulationPathFile(String initialPopulationPathFile) {
        this.initialPopulationPathFile = initialPopulationPathFile;
    }

    public String getSavePopulationPathFile() {
        return savePopulationPathFile;
    }

    public void setSavePopulationPathFile(String savePopulationPathFile) {
        this.savePopulationPathFile = savePopulationPathFile;
    }

    public Integer getSavePopulationPeriod() {
        return savePopulationPeriod;
    }

    public void setSavePopulationPeriod(Integer savePopulationPeriod) {
        this.savePopulationPeriod = savePopulationPeriod;
    }

    public Integer getEvolutionExitType() {
        return evolutionExitType;
    }

    public void setEvolutionExitType(Integer evolutionExitType) {
        this.evolutionExitType = evolutionExitType;
    }

    public Integer getMaxGenerations() {
        return maxGenerations;
    }

    public void setMaxGenerations(Integer maxGenerations) {
        this.maxGenerations = maxGenerations;
    }

    public Integer getFitnessChange() {
        return fitnessChange;
    }

    public void setFitnessChange(Integer fitnessChange) {
        this.fitnessChange = fitnessChange;
    }
}
