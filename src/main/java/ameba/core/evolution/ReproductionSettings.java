package ameba.core.evolution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by marko on 12/19/16.
 */
public class ReproductionSettings {
    /**
     *
     */
    private String type;
    /**
     *
     */
    private String name;
    /**
     *
     */
    private int probability;

    /**
     *
     */
    public ReproductionSettings() {
    }

    /**
     * @param type
     * @param name
     * @param probability
     */
    public ReproductionSettings(String type, String name, int probability) {
        this.type = type;
        this.name = name;
        this.probability = probability;
    }

    /**
     * @param json
     */
    public ReproductionSettings(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ReproductionSettings settings = mapper.readValue(json, ReproductionSettings.class);
            this.type = settings.getType();
            this.name = settings.getName();
            this.probability = settings.getProbability();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return Json representation of the object.
     */
    @JsonIgnore
    public String getTextJson() {
        ObjectMapper mapper = new ObjectMapper();
        String out = "";
        try {
            out = mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return out;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getProbability() {
        return probability;
    }

    public void setProbability(int probability) {
        this.probability = probability;
    }
}
