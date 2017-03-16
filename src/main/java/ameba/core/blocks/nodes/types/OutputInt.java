package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTargetInt;
import ameba.core.blocks.nodes.INodeOutputInt;
import ameba.core.blocks.nodes.Node;


/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class OutputInt extends Node implements INodeOutputInt {

    int value;

    public OutputInt() throws Exception {
        super(new int[]{0, 0}, new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0});
        addCollectorTargetInt(new CollectorTargetInt(this));
        value = 0;
    }

    @Override
    public void clcNode() throws Exception {
        value = getCollectorsTargetInt().get(0).getSignal();
    }


    @Override
    public int exportSignal() {
        return value;
    }
}