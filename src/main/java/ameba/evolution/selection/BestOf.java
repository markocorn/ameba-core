package ameba.evolution.selection;

import ameba.core.blocks.Cell;

import java.util.ArrayList;

/**
 * Created by marko on 3/31/17.
 */
public class BestOf implements ISelect {
    @Override
    public ArrayList<Cell> select(ArrayList<Cell> parents, int chosen) {
        return new ArrayList<Cell>(parents.subList(0, chosen));
    }
}
