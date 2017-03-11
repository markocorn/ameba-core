package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.Edge;

/**
 * Created by marko on 2/20/17.
 */
public interface ICrossEdge {
    Edge cross(Edge edge1, Edge edge2) throws Exception;
}
