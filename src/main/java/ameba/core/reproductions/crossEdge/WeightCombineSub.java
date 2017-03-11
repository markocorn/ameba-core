package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineSub;

/**
 * Created by marko on 12/28/16.
 */
public class WeightCombineSub extends Reproduction implements ICrossEdge {
    CombineSub operationType;

    public WeightCombineSub(CombineSub operationType) {
        super(operationType.getSettings().getProbability());
        this.operationType = operationType;
    }

    @Override
    public Edge cross(Edge edge1, Edge edge2) throws Exception {
        return new Edge(edge1.getSource(), edge1.getTarget(), operationType.crossover(edge1.getSignal(), edge2.getSignal()));
    }
}
