package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.edges.EdgeDec;

/**
 * Created by marko on 12/28/16.
 */
public interface IMutateEdgeDec {
    EdgeDec mutate(EdgeDec edge) throws Exception;
}
