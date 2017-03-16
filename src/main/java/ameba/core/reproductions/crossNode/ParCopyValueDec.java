package ameba.core.reproductions.crossNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.parametersOperations.genParCrossover.CopyValueDec;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParCopyValueDec implements ICrossNode {
    CopyValueDec operationType;
    Random random;

    public ParCopyValueDec(CopyValueDec operationType) {
        this.operationType = operationType;
        random = new Random();
    }

    @Override
    public Node cross(Node node1, Node node2) throws Exception {
        if (node1.getParamsDec().length > 0 && node2.getParamsDec().length > 0) {
            int ind1 = random.nextInt(node1.getParamsDec().length);
            int ind2 = random.nextInt(node1.getParamsDec().length);
            node1.getParamsDec()[ind1] = operationType.crossover(node1.getParamsDec()[ind1], node2.getParamsDec()[ind2]);
            return node1;
        }
        return null;
    }
}
