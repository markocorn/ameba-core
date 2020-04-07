package ameba.core.reproductions.crossCell;

import ameba.core.blocks.Cell;

/**
 * Created by marko on 2/20/17.
 */
public interface ICrossCell {
    Cell cross(Cell cell1, Cell cell2) throws Exception;
}
