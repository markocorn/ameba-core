package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.edges.EdgeBin;
import ameba.core.blocks.edges.EdgeDec;
import ameba.core.blocks.edges.EdgeInt;
import ameba.core.reproductions.Reproduction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class SwitchEdgesTargets extends Reproduction implements IMutateCell {
    Random random;

    public SwitchEdgesTargets(int probability) {
        super(probability);
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        ArrayList<String> opts = new ArrayList<>();
        if (cell.getEdgesDec().size() > 1) {
            opts.add("Dec");
        }
        if (cell.getEdgesInt().size() > 1) {
            opts.add("Int");
        }
        if (cell.getEdgesBin().size() > 1) {
            opts.add("Bin");
        }
        if (opts.size() == 0) {
            throw new Exception("Not enough edges of same type.");
        } else {
            String opt = opts.get(random.nextInt(opts.size()));
            switch (opt) {
                case "Dec": {
                    ArrayList<EdgeDec> edgeDecs = cell.getEdgesUnlockedTargetDec();
                    Collections.shuffle(edgeDecs);
                    EdgeDec e1 = edgeDecs.get(0);
                    EdgeDec e2 = edgeDecs.get(1);

                    CollectorTargetDec s1 = e1.getTargetDec();
                    CollectorTargetDec s2 = e2.getTargetDec();

                    e1.setTarget(s2);
                    e2.setTarget(s1);

                    s1.getEdges().set(s1.getEdges().indexOf(e1), e2);
                    s2.getEdges().set(s2.getEdges().indexOf(e2), e1);
                }
                break;
                case "Int": {
                    ArrayList<EdgeInt> edgeInts = cell.getEdgesUnlockedTargetInt();
                    Collections.shuffle(edgeInts);
                    EdgeInt e1 = edgeInts.get(0);
                    EdgeInt e2 = edgeInts.get(1);

                    CollectorTargetInt s1 = e1.getTargetInt();
                    CollectorTargetInt s2 = e2.getTargetInt();

                    e1.setTarget(s2);
                    e2.setTarget(s1);

                    s1.getEdges().set(s1.getEdges().indexOf(e1), e2);
                    s2.getEdges().set(s2.getEdges().indexOf(e2), e1);
                }
                break;
                case "Bin": {
                    ArrayList<EdgeBin> edgeBins = cell.getEdgesUnlockedTargetBin();
                    Collections.shuffle(edgeBins);
                    EdgeBin e1 = edgeBins.get(0);
                    EdgeBin e2 = edgeBins.get(1);

                    CollectorTargetBin s1 = e1.getTargetBin();
                    CollectorTargetBin s2 = e2.getTargetBin();

                    e1.setTarget(s2);
                    e2.setTarget(s1);

                    s1.getEdges().set(s1.getEdges().indexOf(e1), e2);
                    s2.getEdges().set(s2.getEdges().indexOf(e2), e1);
                }
                break;
            }
            return cell;
        }
    }
}
