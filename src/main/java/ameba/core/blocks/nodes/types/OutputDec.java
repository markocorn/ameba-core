package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTargetDec;
import ameba.core.blocks.nodes.INodeOutputDec;
import ameba.core.blocks.nodes.Node;


/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class OutputDec extends Node implements INodeOutputDec {

    double value;

    public OutputDec() throws Exception {
        super(new int[]{1, 1}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 0});
        addCollectorTargetDec(new CollectorTargetDec(this));
        value = 0.0;
    }

    @Override
    public void clcNode() throws Exception {
        value = getCollectorsTargetDec().get(0).getSignal();
    }

    @Override
    public double exportSignal() {
        return value;
    }
}