package ameba.core.blocks.nodes;

import ameba.core.blocks.conectivity.CollectorInp;
import ameba.core.blocks.conectivity.CollectorOut;
import ameba.core.blocks.conectivity.Signal;

/**
 * Created by marko on 2/1/17.
 */
public class Interval2Const extends Node {

    public Interval2Const(Signal type, Signal par1, Signal[] par1Limits, Signal par2, Signal[] par2Limits) throws Exception {
        super(1, 1, 1, 1);
        addInpCollector(new CollectorInp(type.clone(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));

        getParams().add(par1);
        getParamsLimits().add(par1Limits);
        getParams().add(par2);
        getParamsLimits().add(par2Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0: {
                if (isSignalInputsReady()) {
                    if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                        double[] inputs = new double[]{getInpCollectorsConn().get(0).getSignal().getValueDouble(), getParams().get(0).getValueDouble(), getParams().get(1).getValueDouble()};
                        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
                            getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                        } else getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                    } else if (getInpCollectorsConn().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                        int[] inputs = new int[]{getInpCollectorsConn().get(0).getSignal().getValueInteger(), getParams().get(0).getValueInteger(), getParams().get(1).getValueInteger()};
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
