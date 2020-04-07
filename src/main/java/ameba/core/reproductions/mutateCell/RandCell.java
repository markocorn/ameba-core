package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.factories.FactoryCell;
import ameba.core.reproductions.Reproduction;

/**
 * Created by marko on 3/2/17.
 */
public class RandCell extends Reproduction implements IMutateCell {
    FactoryCell cellFactory;

    public RandCell(FactoryCell cellFactory, int probability) {
        super(probability);
        this.cellFactory = cellFactory;
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        return cellFactory.genCellRnd();
    }
}
