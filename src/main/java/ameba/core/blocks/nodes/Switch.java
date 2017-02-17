package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Signal;

/**
 * Created by marko on 2/1/17.
 */
public class Switch extends Node {

    public Switch(Signal type) throws Exception {
        super(3, 3, 1, 1);
        addInpCollector(new CollectorInp(Signal.createBoolean(), this));
        addInpCollector(new CollectorInp(type, this));
        addInpCollector(new CollectorInp(type, this));
        addOutCollector(new CollectorOut(type, this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0:
                if (isSignalInputsReady()) {
                    if (!getInpCollectorsConn().get(0).getSignal().getValueBoolean()) {
                        getOutCollectors().get(0).setSignal(getInpCollectorsConn().get(1).getSignal());
                        //Be carefully allays initiate get method for inputs to set send flag of input nodes.
                        getInpCollectorsConn().get(2).getSignal();
                    } else {
                        getOutCollectors().get(0).setSignal(getInpCollectorsConn().get(2).getSignal());
                        //Be carefully allays initiate get method for inputs to set send flag of input nodes.
                        getInpCollectorsConn().get(1).getSignal();
                    }
                    setSignalClcDone(true);
                    setState(1);
                }
                break;
            case 1:
                if (isSignalClcDone()) {
                    setSignalReady(true);
                    setState(2);
                }
                break;
            case 2:
                if (isSignalReady()) {
                    setState(3);
                }
                break;
            case 3:
                if (isSignalSend() || getOutCollectors().get(0).getEdges().size() == 0) {
                    setState(4);
                }
                break;
            case 4:
        }
    }
}
