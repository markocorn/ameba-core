package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorSourceInt;
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
public class SwitchEdgesSources extends Reproduction implements IMutateCell {
    Random random;

    public SwitchEdgesSources(int probability) {
        super(probability);
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        ArrayList<String> opts = new ArrayList<>();
        if (cell.getEdgesUnlockedSourceDec().size() > 1) {
            opts.add("Dec");
        }
        if (cell.getEdgesUnlockedSourceInt().size() > 1) {
            opts.add("Int");
        }
        if (cell.getEdgesUnlockedSourceBin().size() > 1) {
            opts.add("Bin");
        }
        if (opts.size() == 0) {
            throw new Exception("Not enough edges of same type.");
        } else {
            String opt = opts.get(random.nextInt(opts.size()));
            switch (opt) {
                case "Dec": {
                    ArrayList<EdgeDec> edgeDecs = cell.getEdgesUnlockedSourceDec();
                    Collections.shuffle(edgeDecs);
                    EdgeDec e1 = edgeDecs.get(0);
                    EdgeDec e2 = edgeDecs.get(1);

                    CollectorSourceDec s1 = e1.getSourceDec();
                    CollectorSourceDec s2 = e2.getSourceDec();

                    e1.setSource(s2);
                    e2.setSource(s1);

                    s1.getEdges().set(s1.getEdges().indexOf(e1), e2);
                    s2.getEdges().set(s2.getEdges().indexOf(e2), e1);
                }
                break;
                case "Int": {
                    ArrayList<EdgeInt> edgeInts = cell.getEdgesUnlockedSourceInt();
                    Collections.shuffle(edgeInts);
                    EdgeInt e1 = edgeInts.get(0);
                    EdgeInt e2 = edgeInts.get(0);

                    CollectorSourceInt s1 = e1.getSourceInt();
                    CollectorSourceInt s2 = e2.getSourceInt();

                    e1.setSource(s2);
                    e2.setSource(s1);

                    s1.getEdges().set(s1.getEdges().indexOf(e1), e2);
                    s2.getEdges().set(s2.getEdges().indexOf(e2), e1);
                }
                break;
                case "Bin": {
                    ArrayList<EdgeBin> edgeBins = cell.getEdgesUnlockedSourceBin();
                    Collections.shuffle(edgeBins);
                    EdgeBin e1 = edgeBins.get(0);
                    EdgeBin e2 = edgeBins.get(0);

                    CollectorSourceBin s1 = e1.getSourceBin();
                    CollectorSourceBin s2 = e2.getSourceBin();

                    e1.setSource(s2);
                    e2.setSource(s1);

                    s1.getEdges().set(s1.getEdges().indexOf(e1), e2);
                    s2.getEdges().set(s2.getEdges().indexOf(e2), e1);
                }
                break;
            }
        }
        return cell;
    }
}
