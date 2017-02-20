package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.Edge;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineAnd;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCombineAnd implements ICrossEdge {
    CombineAnd operationType;

    public WeightCombineAnd(CombineAnd operationType) {
        this.operationType = operationType;
    }

    @Override
    public Edge cross(Edge edge1, Edge edge2) throws Exception {
        return new Edge(edge1.getSource(), edge1.getTarget(), operationType.crossover(edge1.getSignal(), edge2.getSignal()));
    }
}
