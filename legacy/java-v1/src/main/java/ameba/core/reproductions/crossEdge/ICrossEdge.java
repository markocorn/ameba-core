package ameba.core.reproductions.crossEdge;

import ameba.core.blocks.edges.Edge;

/**
 * Created by marko on 3/30/17.
 */
public interface ICrossEdge {
    Edge cross(Edge edge1, Edge edge2);
}
