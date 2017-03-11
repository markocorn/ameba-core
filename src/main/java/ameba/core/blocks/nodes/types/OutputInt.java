package ameba.core.blocks.nodes.types;

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.INodeOutput;
import ameba.core.blocks.nodes.Node;


/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class OutputInt extends Node implements INode, INodeOutput {

    Signal value;

    public OutputInt() throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addInpCollector(new CollectorTarget(Signal.createInteger(), this));
        value = Signal.createInteger();
    }

    @Override
    public void clcNode() throws Exception {
        try {
            value = getCollectorsTargetInt().get(0).getSignal();
        } catch (Exception ex) {
            value = getCollectorsTargetInt().get(0).getSignal();
        }

    }


    @Override
    public Signal exportSignal() throws Exception {
        return value;
    }
}