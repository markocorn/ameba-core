package ameba.core.reproductions.crossNode;

import ameba.core.blocks.Cell;
import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 2/20/17.
 */
public interface ICrossNode {
    Node cross(Node node1, Node node2) throws Exception;

    Cell.ParType getType();
}
