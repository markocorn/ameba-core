package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Signal;

/**
 * Created by marko on 2/1/17.
 */
public class Interval extends Node {

    public Interval(Signal type) throws Exception {
        super(3, 3, 1, 1);
        addInpCollector(new CollectorInp(type.clone(), this));
        addInpCollector(new CollectorInp(type.clone(), this));
        addInpCollector(new CollectorInp(type.clone(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0: {
                if (isSignalInputsReady()) {
                    if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                        double[] inputs = new double[]{getInpCollectorsConn().get(0).getSignal().getValueDouble(), getInpCollectorsConn().get(1).getSignal().getValueDouble(), getInpCollectorsConn().get(2).getSignal().getValueDouble()};
                        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
                            getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                        } else getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                    } else if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                        int[] inputs = new int[]{getInpCollectorsConn().get(0).getSignal().getValueInteger(), getInpCollectorsConn().get(1).getSignal().getValueInteger(), getInpCollectorsConn().get(2).getSignal().getValueInteger()};
                        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
                            getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                        } else getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                    } else
                        throw new Exception("Input signals of node " + this.getClass().getSimpleName() + " is not of type Double or Integer.");
                    setSignalClcDone(true);
                    setState(1);
                }
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
