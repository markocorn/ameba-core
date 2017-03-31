package ameba.core.evolution.selections;

import ameba.core.blocks.Cell;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by marko on 3/31/17.
 */
public class Tournament implements ISelect {
    Random random;

    public Tournament() {
        this.random = new Random();
    }

    @Override
    public ArrayList<Cell> select(ArrayList<Cell> parents, int numChild) {
        ArrayList<Cell> selected = new ArrayList<>();
        for (int i = 0; i < numChild; i++) {
            Cell cell1 = parents.get(random.nextInt(parents.size()));
            Cell cell2 = parents.get(random.nextInt(parents.size()));
            if (cell1.getFitnessValue() < cell2.getFitnessValue()) {
                selected.add(cell1);
            }
        }
        return selected;
    }
}
