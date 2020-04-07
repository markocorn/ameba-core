package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.Reproduction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class SwitchEdgesSources extends Reproduction implements IMutateCell {
    Random random;

    public SwitchEdgesSources(int probability, long seed) {
        super(probability);
        random = new Random(seed);
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        if (cell.getEdgesUnlockedSource().size() > 1) {
            ArrayList<Edge> edgeDecs = cell.getEdgesUnlockedSource();
            Collections.shuffle(edgeDecs);
            Edge e1 = edgeDecs.get(0);
            Edge e2 = edgeDecs.get(1);

            CollectorSource s1 = e1.getSource();
            CollectorSource s2 = e2.getSource();

            e1.setSource(s2);
            e2.setSource(s1);

            s1.getEdges().set(s1.getEdges().indexOf(e1), e2);
            s2.getEdges().set(s2.getEdges().indexOf(e2), e1);
        } else {
            throw new Exception("Not enough edges of same type.");
        }
        return cell;
    }
}
