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
public class EdgeFactorySettings {
    /**
     * Interval of values for the weight initial value during rndGen generation.
     */
    private double[] weightIntervalInitial;
    /**
     * Interval of limit values for the
     */
    private double[] weightIntervalLimits;

    /**
     * Default constructor.
     */
    public EdgeFactorySettings() {
        weightIntervalInitial = new double[]{0, 1};
        weightIntervalLimits = new double[]{-100, 100};
    }

    /**
     * @param weightIntervalInitial Set values of the initial interval weight parameter.
     * @param weightIntervalLimits  Set values of the limit interval of weight parameter.
     */
    public EdgeFactorySettings(double[] weightIntervalInitial, double[] weightIntervalLimits) {
        this.weightIntervalInitial = weightIntervalInitial;
        this.weightIntervalLimits = weightIntervalLimits;
    }

    /**
     * Construct factory with json file.
     *
     * @param jsonSettings String representing json file.
     */
    public EdgeFactorySettings(String jsonSettings) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            EdgeFactorySettings settings = mapper.readValue(jsonSettings, EdgeFactorySettings.class);
            this.weightIntervalInitial = settings.weightIntervalInitial;
            this.weightIntervalLimits = settings.weightIntervalLimits;

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

    /**
     * @return Values of the initial interval weight parameter.
     */
    public double[] getWeightIntervalInitial() {
        return weightIntervalInitial;
    }

    /**
     * @param weightIntervalInitial Set values of the initial interval weight parameter.
     */

    public void setWeightIntervalInitial(double[] weightIntervalInitial) {
        this.weightIntervalInitial = weightIntervalInitial;
    }

    /**
     * @return Values of the limit interval of weight parameter.
     */
    public double[] getWeightIntervalLimits() {
        return weightIntervalLimits;
    }

    /**
     * @param weightIntervalLimits Set values of the limit interval of weight parameter.
     */
    public void setWeightIntervalLimits(double[] weightIntervalLimits) {
        this.weightIntervalLimits = weightIntervalLimits;
    }
}
