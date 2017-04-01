package ameba.core.evolution.selections;

import ameba.core.blocks.Cell;

import java.util.ArrayList;

/**
 * Created by marko on 3/31/17.
 */
public interface ISelect {
    ArrayList<Cell> select(ArrayList<Cell> parents, int chosen);
}
