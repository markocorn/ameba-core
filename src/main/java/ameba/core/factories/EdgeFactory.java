//package ameba.core.factories;
//
//import ameba.core.blocks.connections.Edge;
//import ameba.core.blocks.nodes.Node;
//
//import java.util.Random;
//
///**
// * Created by marko on 10/20/16.
// */
//public class EdgeFactory {
//    /**
//     * Settings for the edge factory.
//     */
//    EdgeFactorySettings edgeFactorySettings;
//    /**
//     * Random number generator for general purpose.
//     */
//    Random rndGen;
//
//    /**
//     * @param edgeFactorySettings Edge factory settings.
//     */
//    public EdgeFactory(EdgeFactorySettings edgeFactorySettings) {
//        this.edgeFactorySettings = edgeFactorySettings;
//        rndGen = new Random();
//    }
//
//    /**
//     * @param source Source node for the edge.
//     * @param target Target node for the edge.
//     * @return Edge with randomly generated weight.
//     */
//    public Edge genEdge(Node source, Node target) {
//        return new Edge(source, target, genDouble());
//    }
//
//    /**
//     * @return decimal number within the specified initial interval.
//     */
//    private double genDouble() {
//        return rndGen.nextDouble() * (edgeFactorySettings.getWeightIntervalInitial()[1] - edgeFactorySettings.getWeightIntervalInitial()[0]) + edgeFactorySettings.getWeightIntervalInitial()[0];
//    }
//
//    public EdgeFactorySettings getEdgeFactorySettings() {
//        return edgeFactorySettings;
//    }
//
//    public void setEdgeFactorySettings(EdgeFactorySettings edgeFactorySettings) {
//        this.edgeFactorySettings = edgeFactorySettings;
//    }
//}
