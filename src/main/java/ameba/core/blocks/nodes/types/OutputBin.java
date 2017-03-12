package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.INodeOutputDec;
import ameba.core.blocks.nodes.Node;


/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class OutputBin extends Node implements INode, INodeOutputDec {

    Signal value;

    public OutputBin() throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addInpCollector(new CollectorTarget(Signal.createBoolean(), this));
        value = Signal.createBoolean();
    }

    @Override
    public void clcNode() throws Exception {
        value = getCollectorsTargetBin().get(0).getSignal();
    }


    @Override
    public Signal exportSignal() throws Exception {
        return value;
    }
}