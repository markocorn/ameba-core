package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.edges.Edge;
import ameba.core.blocks.nodes.Node;
import ameba.core.factories.NodeFactory;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by marko on 12/30/16.
 */
public class ChangeType implements IMutateNode {
    NodeFactory nodeFactory;
    Random random;

    public ChangeType(NodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
        random = new Random();
    }

    @Override
    public Node mutate(Node node) {
        ArrayList<String> nodeTypes = nodeFactory.getAllowedNodes(node.getInputEdges().size(), node.getOutputEdges().size(), node.getClass().getSimpleName());
        if (nodeTypes.size() > 0) {
            Node node2 = nodeFactory.genNode(nodeTypes.get(random.nextInt(nodeTypes.size())));
            //Copy input edges
            for (Edge edge : node.getInputEdges()) {
                edge.setTarget(node2);
                node2.addEdgeInput(edge);
            }
            //Copy output edges
            for (Edge edge : node.getOutputEdges()) {
                edge.setSource(node2);
                node2.addOutputEdge(edge);
            }
            return node2;
        }
        return null;
    }
}
