package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.collectors.CollectorTargetInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class SwitchEdgesSources implements IMutateCell {
    Random random;

    public SwitchEdgesSources() {
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
                    CollectorTargetDec collectorOut = cell.getEdgesDec().get(0).getTarget();
                    cell.getEdgesDec().get(0).getTarget().getEdges().set(cell.getEdgesDec().get(0).getTarget().getEdges().indexOf(cell.getEdgesDec().get(0)), cell.getEdgesDec().get(1));
                    cell.getEdgesDec().get(1).getTarget().getEdges().set(cell.getEdgesDec().get(1).getTarget().getEdges().indexOf(cell.getEdgesDec().get(1)), cell.getEdgesDec().get(0));
                    cell.getEdgesDec().get(0).setTarget(cell.getEdgesDec().get(1).getTarget());
                    cell.getEdgesDec().get(1).setTarget(collectorOut);
                }
                break;
                case "Int": {
                    Collections.shuffle(cell.getEdgesInt());
                    CollectorTargetInt collectorOut = cell.getEdgesInt().get(0).getTarget();
                    cell.getEdgesInt().get(0).getTarget().getEdges().set(cell.getEdgesInt().get(0).getTarget().getEdges().indexOf(cell.getEdgesInt().get(0)), cell.getEdgesInt().get(1));
                    cell.getEdgesInt().get(1).getTarget().getEdges().set(cell.getEdgesInt().get(1).getTarget().getEdges().indexOf(cell.getEdgesInt().get(1)), cell.getEdgesInt().get(0));
                    cell.getEdgesInt().get(0).setTarget(cell.getEdgesInt().get(1).getTarget());
                    cell.getEdgesInt().get(1).setTarget(collectorOut);
                }
                break;
                case "Bin": {
                    Collections.shuffle(cell.getEdgesBin());
                    CollectorTargetBin collectorOut = cell.getEdgesBin().get(0).getTarget();
                    cell.getEdgesBin().get(0).getTarget().getEdges().set(cell.getEdgesBin().get(0).getTarget().getEdges().indexOf(cell.getEdgesBin().get(0)), cell.getEdgesBin().get(1));
                    cell.getEdgesBin().get(1).getTarget().getEdges().set(cell.getEdgesBin().get(1).getTarget().getEdges().indexOf(cell.getEdgesBin().get(1)), cell.getEdgesBin().get(0));
                    cell.getEdgesBin().get(0).setTarget(cell.getEdgesBin().get(1).getTarget());
                    cell.getEdgesBin().get(1).setTarget(collectorOut);
                }
                break;
            }
            return cell;
        }
    }
}
