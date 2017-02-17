package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.Signal;


/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:41
 * To change this template use File | Settings | File Templates.
 */

public class Output extends Node implements INode, INodeOutput {

    Signal value;

    public Output(Signal type) throws Exception {
        super(1, 1, 0, 0);
        addInpCollector(new CollectorInp(type.clone(), this));
        value = type;
    }

    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    value = getInpCollectorsConn().get(0).getSignal();
                    setSignalClcDone(true);
                    setState(4);
                }
                break;
            case 4:
        }
    }


    @Override
    public Signal exportSignal() throws Exception {
        return value;
    }
}