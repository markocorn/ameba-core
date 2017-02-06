package ameba.core.blocks.nodes;

import ameba.core.blocks.connections.CollectorInp;
import ameba.core.blocks.connections.CollectorOut;
import ameba.core.blocks.connections.Signal;

/**
 * Created by marko on 2/1/17.
 */
public class IntervalCons extends Node {

    public IntervalCons(Signal type, int minOutputEdges, int maxOutputEdges, Signal const1, Signal[] const1Limits) throws Exception {
        super(2, 2, 1, 1);
        addInpCollector(new CollectorInp(type.clone(), this));
        addInpCollector(new CollectorInp(type.clone(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), minOutputEdges, maxOutputEdges, this));

        getParams().add(const1);
        getParamsLimits().add(const1Limits);
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        switch (getState()) {
            case 0: {
                if (isSignalInputsReady()) {
                    if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Double.class)) {
                        double[] inputs = new double[]{getInpCollectors().get(0).getSignal().getValueDouble(), getInpCollectors().get(1).getSignal().getValueDouble(), getParams().get(0).getValueDouble()};
                        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
                            getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                        } else getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                    } else if (getInpCollectors().get(0).getSignal().gettClass().isAssignableFrom(Integer.class)) {
                        int[] inputs = new int[]{getInpCollectors().get(0).getSignal().getValueInteger(), getInpCollectors().get(1).getSignal().getValueInteger(), getParams().get(0).getValueInteger()};
                        if (inputs[1] <= inputs[0] && inputs[0] <= inputs[2]) {
                            getOutCollectors().get(0).setSignal(Signal.createBoolean(true));
                        } else getOutCollectors().get(0).setSignal(Signal.createBoolean(false));
                    } else
                        throw new Exception("Input signals of node " + this.getClass().getSimpleName() + " is not of type Double or Integer.");
                    setSignalClcDone(true);
                    setState(1);
                }
            }
            case 1:
                if (isSignalClcDone()) {
                    setSignalReady(true);
                    setState(2);
                }
            case 2:
                if (isSignalReady()) {
                    setState(3);
                }
            case 3:
                if (isSignalSend()) {
                    setState(4);
                }
            case 4:
        }
    }
}
