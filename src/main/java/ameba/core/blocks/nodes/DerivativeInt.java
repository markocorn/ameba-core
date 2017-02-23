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
public class DerivativeInt extends Node {

    //Initial value
    private Signal initValue;
    private Signal signalOld;

    public DerivativeInt(Signal initial, Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0});
        this.initValue = initial;
        signalOld = initial.clone();
        addInpCollector(new CollectorInp(initial.clone(), this));
        addOutCollector(new CollectorOut(initial.clone(), this));

        getParams().add(par);
        getParamsLimits().add(parLimits);

        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getOutCollectorsInt().get(0).getSignal().setValueInteger((getInpCollectorsInt().get(0).getSignal().getValueInteger() - signalOld.getValueInteger()) / getParams().get(0).getValueInteger());
        signalOld = getInpCollectorsInt().get(0).getSignal();
    }

    @Override
    public void clearNode() {
        rstNode();
        try {
            getOutCollectorsInt().get(0).setSignal(initValue);
            signalOld.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}