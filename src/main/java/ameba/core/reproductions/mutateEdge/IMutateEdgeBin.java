package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.EdgeBin;

/**
 * Created by marko on 12/28/16.
 */
public interface IMutateEdgeBin {
    EdgeBin mutate(EdgeBin edge) throws Exception;
}