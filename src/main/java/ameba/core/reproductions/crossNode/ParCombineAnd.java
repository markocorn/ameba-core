package ameba.core.reproductions.crossNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineAnd;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParCombineAnd implements ICrossNode {
    CombineAnd operationType;
    Random random;

    public ParCombineAnd(CombineAnd operationType) {
        this.operationType = operationType;
        random = new Random();
    }

    @Override
    public Node cross(Node node1, Node node2) throws Exception {
        if (node1.getParamsBin().size() > 0 && node2.getParamsBin().size() > 0) {
            int ind1 = random.nextInt(node1.getParamsBin().size());
            int ind2 = random.nextInt(node1.getParamsBin().size());
            node1.getParamsBin().set(ind1, operationType.crossover(node1.getParamsBin().get(ind1), node2.getParamsBin().get(ind2)));
            return node1;
        }
        return null;
    }
}
