package ameba.core.factories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by marko on 12/5/16.
 */
public class CellFactorySettings {
    /**
     * Initial maximum number of nodes.
     */
    private int nodeInitial;
    /**
     * Number of input types of nodes.
     */
    private int nodeInputs;
    /**
     * Number of output types of nodes.
     */
    private int nodeOutputs;
    /**
     * Maximum number of nodes.
     */
    private int nodeMax;

    /**
     * Default constructor.
     */
    public CellFactorySettings() {
    }

    /**
     * General constructor.
     *
     * @param nodeInitial
     * @param nodeInputs
     * @param nodeOutputs
     * @param nodeMax
     */
    public CellFactorySettings(int nodeInitial, int nodeInputs, int nodeOutputs, int nodeMax) {
        this.nodeInitial = nodeInitial;
        this.nodeInputs = nodeInputs;
        this.nodeOutputs = nodeOutputs;
        this.nodeMax = nodeMax;
    }

    /**
     * Build object from settings in form of a json string.
     *
     * @param jsonSettings String of settings.
     */
    public CellFactorySettings(String jsonSettings) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            CellFactorySettings settings = mapper.readValue(jsonSettings, CellFactorySettings.class);
            this.nodeInitial = settings.getNodeInitial();
            this.nodeInputs = settings.getNodeInputs();
            this.nodeOutputs = settings.getNodeOutputs();
            this.nodeMax = settings.getNodeMax();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
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

    public int getNodeInitial() {
        return nodeInitial;
    }

    public void setNodeInitial(int nodeInitial) {
        this.nodeInitial = nodeInitial;
    }

    public int getNodeInputs() {
        return nodeInputs;
    }

    public void setNodeInputs(int nodeInputs) {
        this.nodeInputs = nodeInputs;
    }

    public int getNodeOutputs() {
        return nodeOutputs;
    }

    public void setNodeOutputs(int nodeOutputs) {
        this.nodeOutputs = nodeOutputs;
    }

    public int getNodeMax() {
        return nodeMax;
    }

    public void setNodeMax(int nodeMax) {
        this.nodeMax = nodeMax;
    }
}
