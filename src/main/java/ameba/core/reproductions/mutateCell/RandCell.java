package ameba.core.reproductions.mutateCell;

import ameba.core.blocks.Cell;
import ameba.core.factories.FactoryCell;

/**
 * Created by marko on 3/2/17.
 */
public class RandCell implements IMutateCell {
    FactoryCell cellFactory;

    public RandCell(FactoryCell cellFactory) {
        this.cellFactory = cellFactory;
    }

    @Override
    public Cell mutate(Cell cell) throws Exception {
        return cellFactory.genCellRnd();
    }
}
