package ameba.core.evolution.selections;

import ameba.core.blocks.Cell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by marko on 3/31/17.
 */
public class BestOf implements ISelect {
    @Override
    public ArrayList<Cell> select(ArrayList<Cell> parents, int chosen) {
        Collections.sort(parents, Comparator.comparingDouble(Cell::getFitnessValue));
        return new ArrayList<Cell>(parents.subList(0, chosen));
    }
}
