package ameba.core.factories;

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

/**
 * Created by marko on 10/20/16.
 */
public class FactoryEdge implements Serializable {
    /**
     * Settings for the edge factory.
     */
    FactoryEdgeSettings factoryEdgeSettings;
    /**
     * Random number generator for general purpose.
     */
    Random rndGen;

    /**
     * @param factoryEdgeSettings Edge factory nodeSettingsHashMap.
     */
    public FactoryEdge(FactoryEdgeSettings factoryEdgeSettings, long seed) {
        this.factoryEdgeSettings = factoryEdgeSettings;
        rndGen = new Random(seed);
    }

    public static FactoryEdge build(long seed) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(FactoryEdge.class.getClassLoader().getResourceAsStream("core/edgeFactorySettings.json"));
        return new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class), seed);
    }

    public static FactoryEdge build(String filePath, long seed) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(new File(filePath));
        return new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class), seed);
    }


    /**
     * @param source Source node for the edge.
     * @param target Target node for the edge.
     * @return Edge with randomly generated weight.
     */
    public Edge genEdge(CollectorSource source, CollectorTarget target) {
        return new Edge(source, target, genDouble(), factoryEdgeSettings.getWeighLimitsDec());
    }

    public Edge genEdge(CollectorSource source, CollectorTarget target, double weight) {
        return new Edge(source, target, weight, factoryEdgeSettings.getWeighLimitsDec());
    }


    /**
     * @return decimal number within the specified initial interval.
     */

    private double genDouble() {
        return rndGen.nextDouble() * (factoryEdgeSettings.getWeightInitialDec()[1] - factoryEdgeSettings.getWeightInitialDec()[0]) + factoryEdgeSettings.getWeightInitialDec()[0];
    }

    public FactoryEdge clone() {
        return (FactoryEdge) SerializationUtils.clone(this);
    }
}
