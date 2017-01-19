package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorInp;


/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class OutputInt extends Node implements INode, INodeOutput {
    public OutputInt() {
        super();
        addInpCollector(Integer.class, new CollectorInp(1, 1, this));
    }

    @Override
    public void clcNode() {
        if (getInpCollectors().get(0).isSignalReady()) {
            setSignalInt((Integer) getInpCollectors().get(0).getSignal(Integer.class));
        }
    }

    @Override
    public void clearNode() {
        rstNode();
        setSignalInt(0);
    }

    @Override
    public void rstNode() {
        setSignalReady(false);
    }

    @Override
    public <T> T exportSignal() {
        return (T) getSignal(Integer.class);
    }
}