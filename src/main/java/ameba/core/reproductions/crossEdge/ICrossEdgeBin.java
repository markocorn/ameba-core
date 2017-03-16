package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.EdgeBin;

/**
 * Created by marko on 2/20/17.
 */
public interface ICrossEdgeBin {
    EdgeBin cross(EdgeBin edge1, EdgeBin edge2) throws Exception;
}
