package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorInp;


/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class OutputBin extends Node implements INode, INodeOutput {
    public OutputBin() {
        super();
        addInpCollector(Boolean.class, new CollectorInp(1, 1, this));
    }

    @Override
    public void clcNode() {
        if (getInpCollectors().get(0).isSignalReady()) {
            setSignalBin((Boolean) getInpCollectors().get(0).getSignal(Boolean.class));
        }
    }

    @Override
    public void clearNode() {
        rstNode();
        setSignalBin(false);
    }

    @Override
    public void rstNode() {
        setSignalReady(false);
    }

    @Override
    public <T> T exportSignal() {
        return (T) getSignal(Boolean.class);
    }
}