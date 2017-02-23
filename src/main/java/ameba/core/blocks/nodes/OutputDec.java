package ameba.core.blocks.nodes;

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.Signal;


/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class OutputDec extends Node implements INode, INodeOutput {

    Signal value;

    public OutputDec() throws Exception {
        super(new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{0, 0});
        addInpCollector(new CollectorInp(Signal.createDouble(), this));
        value = Signal.createDouble();
    }

    @Override
    public void clcNode() throws Exception {
        try {
            value = getInpCollectorsDec().get(0).getSignal();
        } catch (Exception ex) {
            value = getInpCollectorsDec().get(0).getSignal();
        }
    }


    @Override
    public Signal exportSignal() throws Exception {
        return value;
    }
}