package ameba.core.blocks.nodes;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.CollectorInp;
import ameba.core.blocks.CollectorOut;
import ameba.core.blocks.Signal;

/**
 * @author Marko
 */
public class DerivativeBin extends NodeMem {

    //Initial value
    private Signal initValue;
    private Signal signalOld;

    public DerivativeBin(Signal initial, Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1});
        this.initValue = initial;
        signalOld = initial.clone();
        addInpCollector(new CollectorInp(Signal.createBoolean(), this));
        addOutCollector(new CollectorOut(Signal.createBoolean(), this));

        getParams().add(par);
        getParamsLimits().add(parLimits);

        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getOutCollectorsBin().get(0).getSignal().setValueBoolean(getParams().get(0).getValueBoolean());
        if (!signalOld.getValueBoolean().equals(getInpCollectorsBin().get(0).getSignal().getValueBoolean())) {
            getOutCollectorsBin().get(0).getSignal().setValueBoolean(!getParams().get(0).getValueBoolean());
        }
        signalOld = getInpCollectorsBin().get(0).getSignal();
    }

    @Override
    public void clearNode() {
        rstNode();
        try {
            getOutCollectorsBin().get(0).setSignal(initValue);
            signalOld.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}