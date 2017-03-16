package ameba.core.reproductions.crossNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineSubInt;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParCombineSubInt implements ICrossNode {
    CombineSubInt operationType;
    Random random;

    public ParCombineSubInt(CombineSubInt operationType) {
        this.operationType = operationType;
        random = new Random();
    }

    @Override
    public Node cross(Node node1, Node node2) throws Exception {
        if (node1.getParamsInt().length > 0 && node2.getParamsInt().length > 0) {
            int ind1 = random.nextInt(node1.getParamsInt().length);
            int ind2 = random.nextInt(node1.getParamsInt().length);
            node1.getParamsInt()[ind1] = operationType.crossover(node1.getParamsInt()[ind1], node2.getParamsInt()[ind2]);
            return node1;
        }
        return null;
    }
}
