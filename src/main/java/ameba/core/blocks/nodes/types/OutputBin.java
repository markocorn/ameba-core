package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTargetBin;
import ameba.core.blocks.nodes.INodeOutputBin;
import ameba.core.blocks.nodes.Node;


/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class OutputBin extends Node implements INodeOutputBin {

    boolean value;

    public OutputBin() throws Exception {
        super(new int[]{0, 0}, new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0});
        addCollectorTargetBin(new CollectorTargetBin(this));
        value = false;
    }

    @Override
    public void clcNode() {
        value = getCollectorsTargetBin().get(0).getSignal();
    }


    @Override
    public boolean exportSignal() {
        return value;
    }

    @Override
    public void clearNode() {
        super.rstNode();
        value = false;
    }
}