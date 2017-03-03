package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Edge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by marko on 2/28/17.
 */
public class SwitchEdges1 implements IMutateCell {
    Random random;

    public SwitchEdges1() {
        random = new Random();
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        ArrayList<Edge> edges = new ArrayList<>();
        ArrayList<Edge> edgesDec = cell.getEdges(Double.class);
        ArrayList<Edge> edgesInt = cell.getEdges(Integer.class);
        ArrayList<Edge> edgesBin = cell.getEdges(Boolean.class);
        ArrayList<String> opts = new ArrayList<>();
        if (edgesDec.size() > 1) {
            opts.add("Dec");
        }
        if (edgesInt.size() > 1) {
            opts.add("Int");
        }
        if (edgesBin.size() > 1) {
            opts.add("Bin");
        }
        if (opts.size() == 0) {
            throw new Exception("Not enough edges of same type");
        } else {
            String opt = opts.get(random.nextInt(opts.size()));
            switch (opt) {
                case "Dec": {
                    Collections.shuffle(edgesDec);
                    edges.add(edgesDec.get(0));
                    edges.add(edgesDec.get(1));
                }
                break;
                case "Int":
                    Collections.shuffle(edgesInt);
                    edges.add(edgesInt.get(0));
                    edges.add(edgesInt.get(1));
                    break;
                case "Bin":
                    Collections.shuffle(edgesBin);
                    edges.add(edgesBin.get(0));
                    edges.add(edgesBin.get(1));
                    break;
            }
        }

        if (edges.size() > 0) {
            CollectorOut collectorOut = edges.get(0).getSource();
            edges.get(0).getSource().getEdges().set(edges.get(0).getSource().getEdges().indexOf(edges.get(0)), edges.get(1));
            edges.get(1).getSource().getEdges().set(edges.get(1).getSource().getEdges().indexOf(edges.get(1)), edges.get(0));
            edges.get(0).setSource(edges.get(1).getSource());
            edges.get(1).setSource(collectorOut);

        } else throw new Exception("Not enough edges of same type");
        return cell;
    }
}
