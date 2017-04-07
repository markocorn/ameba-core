package ameba.core.factories;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.*;
import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.edges.EdgeBin;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.blocks.edges.EdgeInt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Random;

/**
 * Created by marko on 10/20/16.
 */
public class FactoryEdge {
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
    public FactoryEdge(FactoryEdgeSettings factoryEdgeSettings) {
        this.factoryEdgeSettings = factoryEdgeSettings;
        rndGen = new Random();
    }

    public static FactoryEdge build() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSettings = mapper.readTree(FactoryEdge.class.getClassLoader().getResourceAsStream("edgeFactorySettings.json"));
        return new FactoryEdge(mapper.readValue(jsonSettings.get("edgeFactorySettings").toString(), FactoryEdgeSettings.class));
    }


    /**
     * @param source Source node for the edge.
     * @param target Target node for the edge.
     * @return Edge with randomly generated weight.
     */
    public EdgeDec genEdgeDec(CollectorSourceDec source, CollectorTargetDec target) {
        return new EdgeDec(source, target, genDouble());
    }

    public EdgeInt genEdgeInt(CollectorSourceInt source, CollectorTargetInt target) {
        return new EdgeInt(source, target, genInt());
    }

    public EdgeBin genEdgeBin(CollectorSourceBin source, CollectorTargetBin target) {
        return new EdgeBin(source, target, genBool());
    }

    public Edge genEdge(Cell.Signal type, CollectorSource source, CollectorTarget target) {
        switch (type) {
            case DECIMAL:
                return genEdgeDec((CollectorSourceDec) source, (CollectorTargetDec) target);
            case INTEGER:
                return genEdgeInt((CollectorSourceInt) source, (CollectorTargetInt) target);
            case BOOLEAN:
                return genEdgeBin((CollectorSourceBin) source, (CollectorTargetBin) target);
        }
        return null;
    }

    /**
     * @return decimal number within the specified initial interval.
     */

    private double genDouble() {
        return rndGen.nextDouble() * (factoryEdgeSettings.getWeightInitialDec()[1] - factoryEdgeSettings.getWeightInitialDec()[0]) + factoryEdgeSettings.getWeightInitialDec()[0];
    }

    private int genInt() {
        return rndGen.nextInt(factoryEdgeSettings.getWeightInitialInt()[1] - factoryEdgeSettings.getWeightInitialInt()[0]) + factoryEdgeSettings.getWeightInitialInt()[0];
    }

    private boolean genBool() {
        if (factoryEdgeSettings.getWeightInitialBin()[0].equals(true)) {
            return true;
        }
        if (factoryEdgeSettings.getWeightInitialBin()[1].equals(false)) {
            return false;
        }
        return rndGen.nextBoolean();
    }
}
