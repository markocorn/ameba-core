package ameba.evolution.incubator;

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
    double[][] inputs;
    double[][] outputs;

    public DataEvo() {
        this.inputs = new double[0][0];
        this.outputs = new double[0][0];
    }

    public DataEvo(double[][] inputs, double[][] outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
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

    public double[][] getInputs() {
        return inputs;
    }

    public void setInputs(double[][] inputs) {
        this.inputs = inputs;
    }

    public double[][] getOutputs() {
        return outputs;
    }

    public void setOutputs(double[][] outputs) {
        this.outputs = outputs;
    }
}
