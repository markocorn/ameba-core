package ameba.core.reproductions.mutateEdge;

import ameba.core.blocks.Edge;
import ameba.core.reproductions.parametersOperations.RepParSettings;

/**
 * Created by marko on 12/28/16.
 */
public interface IMutateEdge {
    Edge mutate(Edge edge) throws Exception;

    RepParSettings getSettings();
}
