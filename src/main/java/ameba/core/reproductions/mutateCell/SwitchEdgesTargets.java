package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorSourceBin;
import ameba.core.blocks.collectors.CollectorSourceDec;
import ameba.core.blocks.collectors.CollectorSourceInt;
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
                    Collections.shuffle(cell.getEdgesDec());
                    CollectorSourceDec collectorOut = cell.getEdgesDec().get(0).getSourceDec();
                    cell.getEdgesDec().get(0).getSource().getEdges().set(cell.getEdgesDec().get(0).getSource().getEdges().indexOf(cell.getEdgesDec().get(0)), cell.getEdgesDec().get(1));
                    cell.getEdgesDec().get(1).getSource().getEdges().set(cell.getEdgesDec().get(1).getSource().getEdges().indexOf(cell.getEdgesDec().get(1)), cell.getEdgesDec().get(0));
                    cell.getEdgesDec().get(0).setSource(cell.getEdgesDec().get(1).getSource());
                    cell.getEdgesDec().get(1).setSource(collectorOut);
                }
                break;
                case "Int": {
                    Collections.shuffle(cell.getEdgesInt());
                    CollectorSourceInt collectorOut = cell.getEdgesInt().get(0).getSourceInt();
                    cell.getEdgesInt().get(0).getSource().getEdges().set(cell.getEdgesInt().get(0).getSource().getEdges().indexOf(cell.getEdgesInt().get(0)), cell.getEdgesInt().get(1));
                    cell.getEdgesInt().get(1).getSource().getEdges().set(cell.getEdgesInt().get(1).getSource().getEdges().indexOf(cell.getEdgesInt().get(1)), cell.getEdgesInt().get(0));
                    cell.getEdgesInt().get(0).setSource(cell.getEdgesInt().get(1).getSource());
                    cell.getEdgesInt().get(1).setSource(collectorOut);
                }
                break;
                case "Bin": {
                    Collections.shuffle(cell.getEdgesBin());
                    CollectorSourceBin collectorOut = cell.getEdgesBin().get(0).getSourceBin();
                    cell.getEdgesBin().get(0).getSource().getEdges().set(cell.getEdgesBin().get(0).getSource().getEdges().indexOf(cell.getEdgesBin().get(0)), cell.getEdgesBin().get(1));
                    cell.getEdgesBin().get(1).getSource().getEdges().set(cell.getEdgesBin().get(1).getSource().getEdges().indexOf(cell.getEdgesBin().get(1)), cell.getEdgesBin().get(0));
                    cell.getEdgesBin().get(0).setSource(cell.getEdgesBin().get(1).getSource());
                    cell.getEdgesBin().get(1).setSource(collectorOut);
                }
                break;
            }
        }
        return cell;
    }
}
