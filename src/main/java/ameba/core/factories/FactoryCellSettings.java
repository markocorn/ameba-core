package ameba.core.factories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by marko on 12/5/16.
 */
public class FactoryCellSettings {
    /**
     * IntervalDec that limits random number of nodes generated at cell building process.
     */
    private Integer[] nodeInitial;
    /**
     * Number of input types of nodes.
     */
    private Integer nodeInpDec;
    private Integer nodeInpInt;
    private Integer nodeInpBin;
    /**
     * Number of output types of nodes.
     */
    private Integer nodeOutDec;
    private Integer nodeOutInt;
    private Integer nodeOutBin;
    /**
     * Maximum number of nodes.
     */
    private int nodeMax;

    /**
     * Default constructor.
     */
    public FactoryCellSettings() {
    }

    public FactoryCellSettings(Integer[] nodeInitial, Integer nodeInpDec, Integer nodeInpInt, Integer nodeInpBin, Integer nodeOutDec, Integer nodeOutInt, Integer nodeOutBin, int nodeMax) {
        this.nodeInitial = nodeInitial;
        this.nodeInpDec = nodeInpDec;
        this.nodeInpInt = nodeInpInt;
        this.nodeInpBin = nodeInpBin;
        this.nodeOutDec = nodeOutDec;
        this.nodeOutInt = nodeOutInt;
        this.nodeOutBin = nodeOutBin;
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
        this.nodeInpDec = settings.getNodeInpDec();
        this.nodeInpInt = settings.getNodeInpInt();
        this.nodeInpBin = settings.getNodeInpBin();
        this.nodeOutDec = settings.getNodeOutDec();
        this.nodeOutInt = settings.getNodeOutInt();
        this.nodeOutBin = settings.getNodeOutBin();
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

    public Integer getNodeInpDec() {
        return nodeInpDec;
    }

    public void setNodeInpDec(Integer nodeInpDec) {
        this.nodeInpDec = nodeInpDec;
    }

    public Integer getNodeInpInt() {
        return nodeInpInt;
    }

    public void setNodeInpInt(Integer nodeInpInt) {
        this.nodeInpInt = nodeInpInt;
    }

    public Integer getNodeInpBin() {
        return nodeInpBin;
    }

    public void setNodeInpBin(Integer nodeInpBin) {
        this.nodeInpBin = nodeInpBin;
    }

    public Integer getNodeOutDec() {
        return nodeOutDec;
    }

    public void setNodeOutDec(Integer nodeOutDec) {
        this.nodeOutDec = nodeOutDec;
    }

    public Integer getNodeOutInt() {
        return nodeOutInt;
    }

    public void setNodeOutInt(Integer nodeOutInt) {
        this.nodeOutInt = nodeOutInt;
    }

    public Integer getNodeOutBin() {
        return nodeOutBin;
    }

    public void setNodeOutBin(Integer nodeOutBin) {
        this.nodeOutBin = nodeOutBin;
    }

    public int getNodeMax() {
        return nodeMax;
    }

    public void setNodeMax(int nodeMax) {
        this.nodeMax = nodeMax;
    }
}
