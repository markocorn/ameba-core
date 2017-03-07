package ameba.core.factories;

import ameba.core.reproductions.crossCell.ICrossCell;
import ameba.core.reproductions.crossEdge.ICrossEdge;
import ameba.core.reproductions.crossNode.ICrossNode;
import ameba.core.reproductions.mutateCell.IMutateCell;
import ameba.core.reproductions.mutateEdge.IMutateEdge;
import ameba.core.reproductions.mutateNode.IMutateNode;

import java.util.HashMap;

/**
 * Created by marko on 3/7/17.
 */
public class FactoryReproduction {
    HashMap<String, IMutateEdge> mutateEdges;
    HashMap<String, IMutateNode> mutateNodes;
    HashMap<String, IMutateCell> mutateCells;

    HashMap<String, ICrossEdge> crossEdges;
    HashMap<String, ICrossNode> crossNodes;
    HashMap<String, ICrossCell> crossCells;

    public FactoryReproduction() {
        mutateEdges = new HashMap<>();
        mutateNodes = new HashMap<>();
        mutateCells = new HashMap<>();
        crossEdges = new HashMap<>();
        crossNodes = new HashMap<>();
        crossCells = new HashMap<>();
    }

    public void loadReproductions(String json) {

    }
}

