package ameba.core.blocks.nodes.types;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.nodes.Node;

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
        addInpCollector(new CollectorTarget(initial.clone(), this));
        addOutCollector(new CollectorSource(initial.clone(), this));

        getParams().add(par);
        getParamsLimits().add(parLimits);

        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() throws Exception {
        getCollectorsSourceInt().get(0).getSignal().setValueInteger((getCollectorsTargetInt().get(0).getSignal().getValueInteger() - signalOld.getValueInteger()) / getParams().get(0).getValueInteger());
        signalOld = getCollectorsTargetInt().get(0).getSignal();
    }

    @Override
    public void clearNode() {
        rstNode();
        try {
            getCollectorsSourceInt().get(0).setSignal(initValue);
            signalOld.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}