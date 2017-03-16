package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.EdgeInt;

/**
 * Created by marko on 2/20/17.
 */
public interface ICrossEdgeInt {
    EdgeInt cross(EdgeInt edge1, EdgeInt edge2) throws Exception;
}
