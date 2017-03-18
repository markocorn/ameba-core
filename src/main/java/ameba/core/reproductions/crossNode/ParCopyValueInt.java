package ameba.core.reproductions.crossNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.parametersOperations.genParCrossover.CopyValueInt;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParCopyValueInt implements ICrossNode {
    CopyValueInt operationType;
    Random random;

    public ParCopyValueInt(CopyValueInt operationType) {
        this.operationType = operationType;
        random = new Random();
    }

    @Override
    public Node cross(Node node1, Node node2) throws Exception {
        if (node1.getParamsInt().size() > 0 && node2.getParamsInt().size() > 0) {
            int ind1 = random.nextInt(node1.getParamsInt().size());
            int ind2 = random.nextInt(node1.getParamsInt().size());
            node1.getParamsInt().set(ind1, operationType.crossover(node1.getParamsInt().get(ind1), node2.getParamsInt().get(ind2)));
            return node1;
        }
        return null;
    }
}
