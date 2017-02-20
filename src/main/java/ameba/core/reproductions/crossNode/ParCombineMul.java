package ameba.core.reproductions.crossNode;

import ameba.core.blocks.nodes.Node;
import ameba.core.reproductions.parametersOperations.genParCrossover.CombineMul;

import java.util.Random;

/**
 * Created by marko on 12/28/16.
 */
public class ParCombineMul implements ICrossNode {
    CombineMul operationType;
    Random random;

    public ParCombineMul(CombineMul operationType) {
        this.operationType = operationType;
        random = new Random();
    }

    @Override
    public Node cross(Node node1, Node node2) throws Exception {
        if (node1.getParams().size() > 0 && node2.getParams().size() > 0) {
            int ind1 = random.nextInt(node1.getParams().size());
            int ind2 = random.nextInt(node1.getParams().size());
            Node node = node1.clone();
            node1.setParam(ind1, operationType.crossover(node1.getParam(ind1), node2.getParam(ind2)));
            return node;
        }
        return null;
    }
}
