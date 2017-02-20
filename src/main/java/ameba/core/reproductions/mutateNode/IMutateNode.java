package ameba.core.reproductions.mutateNode;

import ameba.core.blocks.nodes.Node;

/**
 * Created by marko on 12/27/16.
 */
public interface IMutateNode {
    Node mutate(Node node) throws Exception;
}
