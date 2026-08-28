package ameba.core.blocks.nodes.types;

/**
 * Created with IntelliJ IDEA.
 * User: Uporabnik
 * Date: 8.7.2013
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */

import ameba.core.blocks.collectors.CollectorSource;
import ameba.core.blocks.collectors.CollectorTarget;
import ameba.core.blocks.nodes.NodeMem;

/**
 * @author Marko
 */
public class FilterLP extends NodeMem {
    private double initValue;
    private double signalOld;


    public FilterLP(double initial, Double par, Double[] parLimits) throws Exception {
        super(new int[]{1, 1}, new int[]{1, 1}, 1, 0, 0);
        this.initValue = initial;
        signalOld = initial;

        addCollectorTarget(new CollectorTarget(this));
        addCollectorSource(new CollectorSource(this));

        getParamsDec().add(par);
        getParamsLimitsDec().add(parLimits);

        clearNode();
    }

    //Calculate output value
    @Override
    public void clcNode() {
        getCollectorsSource().get(0).setSignal(signalOld + getParamsDec().get(0) * (getCollectorsTarget().get(0).getSignal() - signalOld));
        signalOld = getCollectorsSource().get(0).getSignal();
    }

    @Override
    public void clearNode() {
        rstNode();
        getCollectorsSource().get(0).setSignal(initValue);
        signalOld = initValue;
        clcCollectorsTargetConnected();
    }
}