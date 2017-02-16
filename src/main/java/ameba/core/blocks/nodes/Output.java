package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Signal;


/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class Output extends Node implements INode, INodeOutput {


    public Output(Signal type) throws Exception {
        super(1, 1, 1, 1);
        addInpCollector(new CollectorInp(type.clone(), this));
        addOutCollector(new CollectorOut(type.clone(), this));
    }

    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    getOutCollectors().get(0).setSignal(getInpCollectorsConn().get(0).getSignal());
                    setSignalClcDone(true);
                    setState(1);
                }
            case 1:
                if (isSignalClcDone()) {
                    setSignalReady(true);
                    setState(2);
                }
            case 2:
                if (isSignalReady()) {
                    setSignalSend(true);
                    setState(3);
                }
            case 3:
                if (isSignalSend()) {
                    setState(4);
                }
            case 4:
        }
    }


    @Override
    public Signal exportSignal() throws Exception {
        return getOutCollectors().get(0).getSignal();
    }
}