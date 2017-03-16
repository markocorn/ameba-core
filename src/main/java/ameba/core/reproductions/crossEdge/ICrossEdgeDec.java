package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.EdgeDec;

/**
 * Created by marko on 2/20/17.
 */
public interface ICrossEdgeDec {
    EdgeDec cross(EdgeDec edge1, EdgeDec edge2) throws Exception;
}
