package ameba.core.factories;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.edges.Edge;

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
     * @param factoryEdgeSettings Edge factory settings.
     */
    public FactoryEdge(FactoryEdgeSettings factoryEdgeSettings) {
        this.factoryEdgeSettings = factoryEdgeSettings;
        rndGen = new Random();
    }

    /**
     * @param source Source node for the edge.
     * @param target Target node for the edge.
     * @return Edge with randomly generated weight.
     */
    public Edge genEdge(Class tclass, CollectorSource source, CollectorTarget target) throws Exception {
        return new Edge(source, target, genInitSignal(tclass));
    }

    /**
     * @return decimal number within the specified initial interval.
     */

    private Signal genInitSignal(Class tclass) throws Exception {
        if (tclass.isAssignableFrom(Double.class)) {
            return Signal.createDouble(rndGen.nextDouble() * (factoryEdgeSettings.getWeightInitialDec()[1] - factoryEdgeSettings.getWeightInitialDec()[0]) + factoryEdgeSettings.getWeightInitialDec()[0]);
        }
        if (tclass.isAssignableFrom(Integer.class)) {
            return Signal.createInteger(rndGen.nextInt(factoryEdgeSettings.getWeightInitialInt()[1] - factoryEdgeSettings.getWeightInitialInt()[0]) + factoryEdgeSettings.getWeightInitialInt()[0]);
        }
        if (tclass.isAssignableFrom(Boolean.class)) {
            if (factoryEdgeSettings.getWeightInitialBin()[0].equals(true)) {
                return Signal.createBoolean(true);
            }
            if (factoryEdgeSettings.getWeightInitialBin()[1].equals(false)) {
                return Signal.createBoolean(false);
            }
            return Signal.createBoolean(rndGen.nextBoolean());
        }
        throw new Exception("Cant generate signal of type: " + tclass.getSimpleName());
    }
}
