package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.Edge;

/**
 * Created by marko on 3/30/17.
 */
public interface IMutateEdge {
    Edge mutate(Edge edge) throws Exception;
}
