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
public class DerivativeDec extends NodeMem {

    //Initial value
    private Signal initValue;
    private Signal signalOld;

    public DerivativeDec(Signal initial, Signal par, Signal[] parLimits) throws Exception {
        super(new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0}, new Integer[]{1, 1}, new Integer[]{0, 0}, new Integer[]{0, 0});
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
        getCollectorsSourceDec().get(0).getSignal().setValueDouble((getCollectorsTargetDec().get(0).getSignal().getValueDouble() - signalOld.getValueDouble()) / getParams().get(0).getValueDouble());
        signalOld = getCollectorsTargetDec().get(0).getSignal();
    }

    @Override
    public void clearNode() {
        rstNode();
        try {
            getCollectorsSourceDec().get(0).setSignal(initValue);
            signalOld.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}