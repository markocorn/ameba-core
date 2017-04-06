package ameba.core.evolution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by marko on 4/5/17.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataEvo {
    double[][] inpDec;
    int[][] inpInt;
    boolean[][] inpBin;
    double[][] outDec;
    int[][] outInt;
    boolean[][] outBin;

    public DataEvo() {
        this.inpDec = new double[0][0];
        this.inpInt = new int[0][0];
        this.inpBin = new boolean[0][0];
        this.outDec = new double[0][0];
        this.outInt = new int[0][0];
        this.outBin = new boolean[0][0];
    }

    public DataEvo(double[][] inpDec, int[][] inpInt, boolean[][] inpBin, double[][] outDec, int[][] outInt, boolean[][] outBin) {
        this.inpDec = inpDec;
        this.inpInt = inpInt;
        this.inpBin = inpBin;
        this.outDec = outDec;
        this.outInt = outInt;
        this.outBin = outBin;
    }

    public static DataEvo create(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, DataEvo.class);
    }

    @JsonIgnore
    public String getStringJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public double[][] getInpDec() {
        return inpDec;
    }

    public void setInpDec(double[][] inpDec) {
        this.inpDec = inpDec;
    }

    public int[][] getInpInt() {
        return inpInt;
    }

    public void setInpInt(int[][] inpInt) {
        this.inpInt = inpInt;
    }

    public boolean[][] getInpBin() {
        return inpBin;
    }

    public void setInpBin(boolean[][] inpBin) {
        this.inpBin = inpBin;
    }

    public double[][] getOutDec() {
        return outDec;
    }

    public void setOutDec(double[][] outDec) {
        this.outDec = outDec;
    }

    public int[][] getOutInt() {
        return outInt;
    }

    public void setOutInt(int[][] outInt) {
        this.outInt = outInt;
    }

    public boolean[][] getOutBin() {
        return outBin;
    }

    public void setOutBin(boolean[][] outBin) {
        this.outBin = outBin;
    }
}
