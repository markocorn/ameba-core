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
import ameba.core.blocks.nodes.NodeMem;

/**
 * @author Marko
 */
public class IntegralInt extends NodeMem {
    private Signal initValue;
    private Signal signalOld;


    public IntegralInt(Signal initial, Signal par, Signal[] parLimits) throws Exception {
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
        getCollectorsSourceInt().get(0).getSignal().setValueInteger(getCollectorsTarget().get(0).getSignal().getValueInteger() * getParams().get(0).getValueInteger() + signalOld.getValueInteger());
        signalOld = getCollectorsSourceInt().get(0).getSignal().clone();
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