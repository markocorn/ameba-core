package ameba.core.reproductions.crossNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.Reproduction;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineAddDec;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParCombineAddDec extends Reproduction implements ICrossNode {
    CombineAddDec operationType;
    Random random;

    public ParCombineAddDec(CombineAddDec operationType) {
        super(operationType.getParOperationSettings().getProbability());
        this.operationType = operationType;
        random = new Random();
    }

    @Override
    public Node cross(Node node1, Node node2) throws Exception {
        if (node1.getParamsDec().size() > 0 && node2.getParamsDec().size() > 0) {
            int ind1 = random.nextInt(node1.getParamsDec().size());
            int ind2 = random.nextInt(node1.getParamsDec().size());
            node1.getParamsDec().set(ind1, operationType.crossover(node1.getParamsDec().get(ind1), node2.getParamsDec().get(ind2)));
            return node1;
        }
        return null;
    }
}
