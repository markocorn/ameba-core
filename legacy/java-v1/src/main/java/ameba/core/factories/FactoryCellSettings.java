package ameba.core.factories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by marko on 12/5/16.
 */
public class FactoryCellSettings implements Serializable {
    /**
     * IntervalDec that limits random number of nodes generated at cell building process.
     */
    private Integer[] nodeInitial;
    /**
     * Number of input types of nodes.
     */
    private Integer nodeInp;
    /**
     * Number of output types of nodes.
     */
    private Integer nodeOut;
    /**
     * Maximum number of nodes.
     */
    private int nodeMax;

    /**
     * Default constructor.
     */
    public FactoryCellSettings() {
    }

    public FactoryCellSettings(Integer[] nodeInitial, Integer nodeInp, Integer nodeOut, int nodeMax) {
        this.nodeInitial = nodeInitial;
        this.nodeInp = nodeInp;
        this.nodeOut = nodeOut;
        this.nodeMax = nodeMax;
    }

    public static FactoryCellSettings create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, FactoryCellSettings.class);
    }

    public void load(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        FactoryCellSettings settings = mapper.readValue(json, FactoryCellSettings.class);
        this.nodeInitial = getNodeInitial();
        this.nodeInp = settings.getNodeInp();
        this.nodeOut = settings.getNodeOut();
        this.nodeMax = settings.getNodeMax();
    }

    @JsonIgnore
    public String getStringJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public Integer[] getNodeInitial() {
        return nodeInitial;
    }

    public void setNodeInitial(Integer[] nodeInitial) {
        this.nodeInitial = nodeInitial;
    }

    public Integer getNodeInp() {
        return nodeInp;
    }

    public void setNodeInp(Integer nodeInpDec) {
        this.nodeInp = nodeInpDec;
    }

    public Integer getNodeOut() {
        return nodeOut;
    }

    public void setNodeOut(Integer nodeOutDec) {
        this.nodeOut = nodeOutDec;
    }

    public int getNodeMax() {
        return nodeMax;
    }

    public void setNodeMax(int nodeMax) {
        this.nodeMax = nodeMax;
    }
}
