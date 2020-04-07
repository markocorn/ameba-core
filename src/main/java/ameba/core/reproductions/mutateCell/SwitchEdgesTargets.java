package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.edges.Edge;
import ameba.core.reproductions.Reproduction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class SwitchEdgesTargets extends Reproduction implements IMutateCell {
    Random random;

    public SwitchEdgesTargets(int probability, long seed) {
        super(probability);
        random = new Random(seed);
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        if (cell.getEdges().size() > 1) {
            ArrayList<Edge> edgeDecs = cell.getEdgesUnlockedTarget();
            Collections.shuffle(edgeDecs);
            Edge e1 = edgeDecs.get(0);
            Edge e2 = edgeDecs.get(1);

            CollectorTarget s1 = e1.getTarget();
            CollectorTarget s2 = e2.getTarget();

            e1.setTarget(s2);
            e2.setTarget(s1);

            s1.getEdges().set(s1.getEdges().indexOf(e1), e2);
            s2.getEdges().set(s2.getEdges().indexOf(e2), e1);
        } else {
            throw new Exception("Not enough edges of same type.");
        }
        return cell;
    }
}
