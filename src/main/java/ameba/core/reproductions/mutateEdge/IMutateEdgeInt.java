package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.EdgeInt;

/**
 * Created by marko on 12/28/16.
 */
public interface IMutateEdgeInt extends IMutateEdge {
    EdgeInt mutate(EdgeInt edge) throws Exception;
}
